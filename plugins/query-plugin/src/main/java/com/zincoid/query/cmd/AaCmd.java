package com.zincoid.query.cmd;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.query.QueryException;
import com.zincoid.query.QueryText;
import com.zincoid.query.config.QueryConfig;
import com.zincoid.query.model.AaMetric;
import com.zincoid.query.model.AaModel;
import com.zincoid.query.service.AaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;

/**
 * AA 指令：展示 Artificial Analysis 模型排行。
 * <p>AA command: shows Artificial Analysis model rankings.</p>
 */
@Slf4j
@CmdMapping({"AA", "模型查询"})
@RequiredArgsConstructor
public class AaCmd implements Cmd {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    private final AaService service;
    private final QueryConfig config;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    private void handle(CmdContext ctx, CmdArgs args) {
        String option = args.next("智能");
        AaMetric metric = AaMetric.from(option);
        if (metric == null)
            throw new CmdWarnException(
                    QueryText.AA_UNKNOWN_METRIC.of(ctx.language(), option));
        int limit = limit(ctx, args);
        if (config.aaApiKey().isBlank())
            throw new CmdWarnException(QueryText.AA_NO_API_KEY.of(ctx.language()));
        List<AaModel> models;
        try {
            models = service.top(metric, limit);
        } catch (QueryException e) {
            log.warn("AA query failed", e);
            throw new CmdWarnException(QueryText.AA_QUERY_FAILED.of(ctx.language()));
        }
        if (models.isEmpty()) {
            ctx.send(QueryText.AA_NO_DATA.of(ctx.language()));
            return;
        }
        ctx.send(format(ctx, metric, models));
        log.info("AA sent: metric={}, rows={}", metric.name(), models.size());
    }

    // ════ Format ══════════════════

    private static String format(CmdContext ctx, AaMetric metric, List<AaModel> models) {
        StringBuilder text = new StringBuilder()
                .append("AA ").append(metric.label(ctx.language()))
                .append(" Top ").append(models.size()).append('\n');
        int index = 1;
        for (AaModel model : models) {
            text.append(index++).append(". ").append(model.name());
            if (!model.creator().isBlank()) text.append(" (").append(model.creator()).append(')');
            text.append(" — ").append(value(ctx, metric, model)).append('\n');
        }
        return text.toString().stripTrailing();
    }

    private static String value(CmdContext ctx, AaMetric metric, AaModel model) {
        Language language = ctx.language();
        return switch (metric) {
            case INTELLIGENCE -> QueryText.AA_INTELLIGENCE.of(language)
                    + " " + number(model.intelligence());
            case HISTORY -> model.releaseDate() + " · "
                    + QueryText.AA_INTELLIGENCE.of(language) + " " + number(model.intelligence());
            case VALUE -> QueryText.AA_INTELLIGENCE.of(language) + " " + number(model.intelligence())
                    + " · " + QueryText.AA_COST.of(language) + " " + moneyCost(model.costPerTask());
            case SPEED -> number(model.outputSpeed()) + " tok/s";
            case CODING -> QueryText.AA_CODING.of(language)
                    + " " + number(model.coding());
            case COST -> moneyCost(model.costPerTask()) + QueryText.AA_PER_TASK.of(language);
            case PRICING -> QueryText.AA_INPUT.of(language) + " " + money(model.inputPrice())
                    + " · " + QueryText.AA_OUTPUT.of(language) + " " + money(model.outputPrice());
        };
    }

    private static String number(Double value) {
        return value == null ? "-" : String.format(Locale.ROOT, "%.1f", value);
    }

    private static String money(Double value) {
        return value == null ? "-" : String.format(Locale.ROOT, "$%.2f", value);
    }

    private static String moneyCost(Double value) {
        if (value == null) return "-";
        if (Math.abs(value) < 0.001) return String.format(Locale.ROOT, "$%.6f", value);
        if (Math.abs(value) < 1) return String.format(Locale.ROOT, "$%.4f", value);
        return String.format(Locale.ROOT, "$%.2f", value);
    }

    // ════ Helpers ═════════════════

    private static int limit(CmdContext ctx, CmdArgs args) {
        if (!args.hasNext()) return DEFAULT_LIMIT;
        String raw = args.next();
        try {
            return Math.clamp(Integer.parseInt(raw), 1, MAX_LIMIT);
        } catch (NumberFormatException e) {
            throw new CmdWarnException(QueryText.AA_MALFORMED_COUNT.of(ctx.language(), raw));
        }
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return QueryText.AA_HELP.of(language);
    }
}
