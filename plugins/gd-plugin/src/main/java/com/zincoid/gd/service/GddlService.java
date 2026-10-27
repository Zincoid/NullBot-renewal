package com.zincoid.gd.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zincoid.gd.GdException;
import com.zincoid.gd.config.GdConfig;
import com.zincoid.nullbot.platform.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GDDL 数据服务：关卡评级、用户、提交记录与 B20。
 * <p>GDDL data service: level ratings, users, submissions, and B20.</p>
 */
@Slf4j
public final class GddlService {

    private static final int PAGE_SIZE = 25;
    private static final int MAX_BEATEN = 25;
    private static final int MAX_BEATING = 200;
    private static final int MAX_JSON_BYTES = 2 * 1024 * 1024;
    private static final int MAX_AVATAR_BYTES = 2 * 1024 * 1024;

    private final GdConfig config;
    private final GdHttp http;
    private final Map<Long, CacheEntry<Optional<GddlLevel>>> levelCache = new ConcurrentHashMap<>();
    private final Map<Long, CacheEntry<GddlB20Result>> scoreCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<byte[]>> avatarCache = new ConcurrentHashMap<>();

    /**
     * 创建 GDDL 数据服务。
     * <p>Creates the GDDL data service.</p>
     *
     * @param config the plugin configuration
     * @param http   the shared HTTP helper
     */
    public GddlService(GdConfig config, GdHttp http) {
        this.config = config;
        this.http = http;
    }

    // ════ Levels ═════════════════

    /**
     * 获取关卡的 GDDL 信息。
     * <p>Fetches the GDDL information of a level.</p>
     *
     * @param id the level id
     * @return the GDDL level, or null when not rated
     */
    public GddlLevel gddlLevel(long id) {
        return gddlLevel(id, true);
    }

    /**
     * 获取关卡的 GDDL 信息，可选择刷新失败时是否回退旧缓存。
     * <p>Fetches the GDDL information of a level, optionally falling back to stale data on failure.</p>
     *
     * @param id            the level id
     * @param fallbackStale whether to return stale cached data on refresh failure
     * @return the GDDL level, or null when not rated
     */
    public GddlLevel gddlLevel(long id, boolean fallbackStale) {
        CacheEntry<Optional<GddlLevel>> cached = levelCache.get(id);
        if (cached != null && !cached.expired(config.cacheTtl()))
            return cached.value().orElse(null);
        try {
            GdHttp.Response response = http.get(
                    config.gddlBase() + "/levels/" + id, "application/json", MAX_JSON_BYTES);
            if (response.status() == 404) {
                levelCache.put(id, new CacheEntry<>(Optional.empty()));
                return null;
            }
            if (!response.ok()) throw new GdException("GDDL returned HTTP " + response.status());
            GddlLevel level = JsonUtils.parse(response.text(), GddlLevel.class);
            Optional<GddlLevel> value = level == null || level.rating() == null
                    ? Optional.empty()
                    : Optional.of(level);
            levelCache.put(id, new CacheEntry<>(value));
            return value.orElse(null);
        } catch (RuntimeException e) {
            if (cached != null && fallbackStale) {
                log.warn("GDDL level refresh failed; using stale: id={}", id, e);
                return cached.value().orElse(null);
            }
            throw e instanceof GdException gdException
                    ? gdException
                    : new GdException("GDDL level query failed", e);
        }
    }

    // ════ Users ══════════════════

    /**
     * 按用户名搜索 GDDL 用户。
     * <p>Searches GDDL users by name.</p>
     *
     * @param name the user name
     * @return the matching users
     */
    public List<GddlUser> searchUsers(String name) {
        String url = config.gddlBase() + "/user?name="
                + URLEncoder.encode(name, StandardCharsets.UTF_8);
        GdHttp.Response response = http.get(url, "application/json", MAX_JSON_BYTES);
        if (response.status() == 404) return List.of();
        if (!response.ok()) throw new GdException("GDDL returned HTTP " + response.status());
        try {
            UserSearch search = JsonUtils.parse(response.text(), UserSearch.class);
            if (search == null || search.data() == null) return List.of();
            return search.data().stream()
                    .filter(item -> item != null && item.name() != null)
                    .map(item -> new GddlUser(item.id(), item.name(), item.avatar()))
                    .toList();
        } catch (RuntimeException e) {
            throw new GdException("GDDL user search failed", e);
        }
    }

    /**
     * 按 ID 获取 GDDL 用户。
     * <p>Fetches a GDDL user by id.</p>
     *
     * @param id the GDDL user id
     * @return the user, or null when not found
     */
    public GddlUser user(long id) {
        GdHttp.Response response = http.get(
                config.gddlBase() + "/user/" + id, "application/json", MAX_JSON_BYTES);
        if (response.status() == 404) return null;
        if (!response.ok()) throw new GdException("GDDL returned HTTP " + response.status());
        try {
            UserItem item = JsonUtils.parse(response.text(), UserItem.class);
            return item == null || item.name() == null
                    ? null : new GddlUser(item.id(), item.name(), item.avatar());
        } catch (RuntimeException e) {
            throw new GdException("GDDL user query failed", e);
        }
    }

    /**
     * 获取用户的 GDDL 提交记录。
     * <p>Fetches a page of GDDL submissions for a user.</p>
     *
     * @param userId the GDDL user id
     * @param status the submission status: beaten or beating
     * @param page   the zero-based page
     * @return the submission page, or null when not found
     */
    SubmissionsPage submissions(long userId, String status, int page) {
        String url = config.gddlBase() + "/user/" + userId + "/submissions"
                + "?limit=" + PAGE_SIZE
                + "&page=" + page
                + "&status=" + status
                + "&sort=levelRating&sortDirection=desc";
        GdHttp.Response response = http.get(url, "application/json", MAX_JSON_BYTES);
        if (response.status() == 404) return null;
        if (!response.ok()) throw new GdException("GDDL returned HTTP " + response.status());
        try {
            return JsonUtils.parse(response.text(), SubmissionsPage.class);
        } catch (RuntimeException e) {
            throw new GdException("GDDL submissions query failed", e);
        }
    }

    /**
     * 计算用户 B20：beaten 取按难度排序的前 25 条，beating 取最多 200 条。
     * <p>Computes the user B20: the top 25 beaten by level rating and up to 200 beating records.</p>
     *
     * @param userId   the GDDL user id
     * @param userName the GDDL user name
     * @return the GDDL B20 result
     */
    public GddlB20Result gddlB20(long userId, String userName) {
        CacheEntry<GddlB20Result> cached = scoreCache.get(userId);
        if (cached != null && !cached.expired(config.cacheTtl())) return cached.value();
        try {
            List<GdScore.Play> plays = new ArrayList<>();

            SubmissionsPage beaten = submissions(userId, "beaten", 0);
            int beatenTotal = beaten == null ? 0 : beaten.total();
            int beatenFetched = addPlays(plays, beaten, true);
            int page = 1;
            while (beatenFetched < Math.min(beatenTotal, MAX_BEATEN) && page < 2) {
                SubmissionsPage next = submissions(userId, "beaten", page++);
                int added = addPlays(plays, next, true);
                if (added == 0) break;
                beatenFetched += added;
            }

            SubmissionsPage beating = submissions(userId, "beating", 0);
            int beatingTotal = beating == null ? 0 : beating.total();
            int beatingFetched = addPlays(plays, beating, false);
            page = 1;
            while (beatingFetched < Math.min(beatingTotal, MAX_BEATING) && page < 8) {
                SubmissionsPage next = submissions(userId, "beating", page++);
                int added = addPlays(plays, next, false);
                if (added == 0) break;
                beatingFetched += added;
            }

            Map<Long, GdScore.Play> unique = new LinkedHashMap<>();
            for (GdScore.Play play : plays) {
                unique.merge(play.levelId(), play,
                        (left, right) -> left.progress() >= right.progress() ? left : right);
            }
            List<GdScore.Play> values = new ArrayList<>(unique.values());
            GdScore.B20 b20 = GdScore.b20(values, config.tierExponent());
            int highest = b20.top().isEmpty() ? 0 : b20.top().getFirst().play().tier();
            GddlB20Result result = new GddlB20Result(
                    userId, userName, beatenTotal, beatingTotal, values.size(), highest, b20);
            scoreCache.put(userId, new CacheEntry<>(result));
            return result;
        } catch (RuntimeException e) {
            if (cached != null) {
                log.warn("GDDL score refresh failed; using stale: user={}", userId, e);
                return cached.value();
            }
            throw e instanceof GdException gdException
                    ? gdException
                    : new GdException("GDDL score query failed", e);
        }
    }

    /**
     * 获取 GDDL 用户头像。
     * <p>Fetches the GDDL user avatar.</p>
     *
     * @param userId   the GDDL user id
     * @param avatarId the avatar uuid, nullable
     * @return the image bytes, or null when unavailable
     */
    public byte[] avatar(long userId, String avatarId) {
        if (avatarId == null || avatarId.isBlank()) return null;
        String key = userId + ":" + avatarId;
        CacheEntry<byte[]> cached = avatarCache.get(key);
        if (cached != null && !cached.expired(config.cacheTtl())) return cached.value();
        try {
            GdHttp.Response response = http.get(
                    config.gddlAvatarBase() + userId + "/" + avatarId + ".png?size=160",
                    "image/*", MAX_AVATAR_BYTES);
            if (response.status() == 404) return null;
            if (!response.ok())
                throw new GdException("Avatar CDN returned HTTP " + response.status());
            if (response.body().length == 0 || !response.contentType().startsWith("image/"))
                return null;
            byte[] bytes = response.body();
            if (avatarCache.size() > 128) avatarCache.clear();
            avatarCache.put(key, new CacheEntry<>(bytes));
            return bytes;
        } catch (RuntimeException e) {
            if (cached != null) {
                log.warn("GD avatar refresh failed; using stale: user={}", userId, e);
                return cached.value();
            }
            log.warn("GD avatar fetch failed: user={}", userId, e);
            return null;
        }
    }

    // ════ Helpers ═════════════════

    private int addPlays(List<GdScore.Play> plays, SubmissionsPage page, boolean beaten) {
        if (page == null || page.submissions() == null) return 0;
        int count = 0;
        for (Submission submission : page.submissions()) {
            count++;
            if (submission == null || submission.level() == null || submission.level().rating() == null)
                continue;
            double rating = submission.level().rating();
            if (Double.isNaN(rating)) continue;
            int tier = Math.max(1, (int) Math.round(rating));
            int progress = submission.progress() <= 0 && beaten ? 100 : submission.progress();
            plays.add(new GdScore.Play(
                    submission.level().id(), submission.level().displayName(), tier, progress));
        }
        return count;
    }

    // ════ GDDL DTO ════════════════

    public record GddlUser(long id, String name, String avatar) {
    }

    public record GddlLevel(
            @JsonProperty("Rating") Double rating,
            @JsonProperty("Enjoyment") Double enjoyment,
            @JsonProperty("Meta") Meta meta,
            @JsonProperty("AREDLPosition") Integer aredlPosition,
            @JsonProperty("RatingCount") Integer ratingCount,
            @JsonProperty("SubmissionCount") Integer submissionCount
    ) {
    }

    record Meta(
            @JsonProperty("Name") String name,
            @JsonProperty("Difficulty") String difficulty,
            @JsonProperty("Publisher") Publisher publisher
    ) {
    }

    record Publisher(@JsonProperty("name") String name) {
    }

    record UserSearch(
            @JsonProperty("total") int total,
            @JsonProperty("data") List<UserItem> data
    ) {
    }

    record UserItem(
            @JsonProperty("ID") long id,
            @JsonProperty("Name") String name,
            @JsonProperty("avatar") String avatar
    ) {
    }

    record SubmissionsPage(
            @JsonProperty("total") int total,
            @JsonProperty("submissions") List<Submission> submissions
    ) {
    }

    record Submission(
            @JsonProperty("Progress") int progress,
            @JsonProperty("Level") LevelRef level
    ) {
    }

    record LevelRef(
            @JsonProperty("ID") long id,
            @JsonProperty("Rating") Double rating,
            @JsonProperty("Meta") Meta meta
    ) {

        String displayName() {
            return meta == null || meta.name() == null || meta.name().isBlank()
                    ? "#" + id
                    : meta.name();
        }
    }

    public record GddlB20Result(
            long userId,
            String userName,
            int beatenTotal,
            int beatingTotal,
            int ratedRecords,
            int highestTier,
            GdScore.B20 b20
    ) {
    }
}
