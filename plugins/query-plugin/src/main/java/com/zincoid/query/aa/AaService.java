package com.zincoid.query.aa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zincoid.nullbot.platform.util.JsonUtils;
import com.zincoid.query.Http;
import com.zincoid.query.QueryConfig;
import com.zincoid.query.QueryException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AA 查询服务：调用 Free API、分页合并、缓存与排序。
 * <p>AA query service: calls the Free API, merges pages, caches, and sorts.</p>
 */
@Slf4j
public class AaService {

    private static final String API_URL = "https://artificialanalysis.ai/api/v2/language/models/free";
    private static final int MAX_PAGES = 20;

    private final QueryConfig config;
    private volatile Snapshot cache;

    public AaService(QueryConfig config) {
        this.config = config;
    }

    // ════ Query ═══════════════════

    /**
     * 返回指定查询项的 Top N 模型。
     * <p>Returns the top N models for the given metric.</p>
     *
     * @param metric the metric
     * @param limit  maximum number of rows
     * @return the sorted models
     */
    public List<AaModel> top(AaMetric metric, int limit) {
        Comparator<AaModel> comparator = switch (metric) {
            case INTELLIGENCE -> Comparator.comparing(AaModel::intelligence,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case HISTORY -> Comparator.comparing(AaModel::releaseDate,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case VALUE -> Comparator.comparing(AaModel::valueRatio,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case SPEED -> Comparator.comparing(AaModel::outputSpeed,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case CODING -> Comparator.comparing(AaModel::coding,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case COST -> Comparator.comparing(AaModel::costPerTask,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case PRICING -> Comparator.comparing(AaModel::inputPrice,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };
        return models().stream()
                .filter(model -> present(metric, model))
                .sorted(comparator)
                .limit(limit)
                .toList();
    }

    private static boolean present(AaMetric metric, AaModel model) {
        return switch (metric) {
            case INTELLIGENCE -> model.intelligence() != null;
            case HISTORY -> model.releaseDate() != null && model.intelligence() != null;
            case VALUE -> model.valueRatio() != null;
            case SPEED -> model.outputSpeed() != null;
            case CODING -> model.coding() != null;
            case COST -> model.costPerTask() != null && model.costPerTask() > 0;
            case PRICING -> model.inputPrice() != null && model.inputPrice() > 0;
        };
    }

    // ════ Cache ═══════════════════

    private List<AaModel> models() {
        Snapshot current = cache;
        if (current != null && current.fresh(config.aaCacheTtl())) return current.models();
        synchronized (this) {
            current = cache;
            if (current != null && current.fresh(config.aaCacheTtl())) return current.models();
            try {
                List<AaModel> fetched = fetchAll();
                cache = new Snapshot(Instant.now(), List.copyOf(fetched));
                return fetched;
            } catch (RuntimeException e) {
                if (current != null) {
                    log.warn("AA refresh failed; using stale cache", e);
                    return current.models();
                }
                throw e;
            }
        }
    }

    // ════ Fetch ═══════════════════

    private List<AaModel> fetchAll() {
        if (config.aaApiKey().isBlank())
            throw new QueryException("AA API key is not configured");
        List<AaModel> models = new ArrayList<>();
        int page = 1;
        int totalPages = 1;
        while (page <= totalPages && page <= MAX_PAGES) {
            AaResponse response = fetchPage(page);
            if (response == null) break;
            if (response.pagination() != null && response.pagination().totalPages() != null)
                totalPages = Math.max(1, response.pagination().totalPages());
            if (response.data() != null) {
                for (Item item : response.data()) {
                    AaModel model = normalize(item);
                    if (model != null) models.add(model);
                }
            }
            page++;
        }
        log.info("AA fetched: {} models, pages={}", models.size(), Math.min(totalPages, MAX_PAGES));
        return models;
    }

    private AaResponse fetchPage(int page) {
        String url = API_URL + "?page=" + page;
        HttpResponse<String> response;
        try {
            response = Http.get(url, config.aaApiKey(), config.userAgent());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QueryException("AA request interrupted", e);
        } catch (IOException e) {
            throw new QueryException("AA request failed", e);
        }
        if (response.statusCode() != 200)
            throw new QueryException("AA HTTP " + response.statusCode());
        return JsonUtils.parse(response.body(), AaResponse.class);
    }

    private static AaModel normalize(Item item) {
        if (item == null || item.name() == null || item.name().isBlank()) return null;
        Evaluations evaluations = item.evaluations();
        Performance performance = item.performance();
        Pricing pricing = item.pricing();
        return new AaModel(
                item.name(),
                item.modelCreator() == null ? "" : nullToEmpty(item.modelCreator().name()),
                parseDate(item.releaseDate()),
                evaluations == null ? null : evaluations.intelligence(),
                evaluations == null ? null : evaluations.coding(),
                cost(item.cost()),
                pricing == null ? null : pricing.inputPrice(),
                pricing == null ? null : pricing.outputPrice(),
                performance == null ? null : performance.outputSpeed()
        );
    }

    private static Double cost(Cost cost) {
        if (cost == null) return null;
        if (cost.costPerTask() != null && cost.costPerTask().totalCost() != null)
            return cost.costPerTask().totalCost();
        return cost.totalCost();
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            return LocalDate.parse(date);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    // ════ DTOs ════════════════════

    private record Snapshot(Instant fetchedAt, List<AaModel> models) {
        boolean fresh(Duration ttl) {
            return Duration.between(fetchedAt, Instant.now()).compareTo(ttl) < 0;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AaResponse(String tier, Pagination pagination, List<Item> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Pagination(@JsonProperty("total_pages") Integer totalPages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Item(
            String id,
            String name,
            String slug,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("model_creator") Creator modelCreator,
            Evaluations evaluations,
            Performance performance,
            Pricing pricing,
            @JsonProperty("artificial_analysis_intelligence_index_cost") Cost cost
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Creator(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Evaluations(
            @JsonProperty("artificial_analysis_intelligence_index") Double intelligence,
            @JsonProperty("artificial_analysis_coding_index") Double coding
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Performance(
            @JsonProperty("median_output_tokens_per_second") Double outputSpeed
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Pricing(
            @JsonProperty("price_1m_input_tokens") Double inputPrice,
            @JsonProperty("price_1m_output_tokens") Double outputPrice
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Cost(
            @JsonProperty("total_cost") Double totalCost,
            @JsonProperty("cost_per_task") CostPerTask costPerTask
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CostPerTask(@JsonProperty("total_cost") Double totalCost) {
    }
}
