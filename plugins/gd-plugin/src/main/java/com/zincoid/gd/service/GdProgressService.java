package com.zincoid.gd.service;

import com.zincoid.gd.GdException;
import com.zincoid.gd.config.GdConfig;
import com.zincoid.gd.service.GddlService.GddlLevel;
import com.zincoid.gd.store.CachedTier;
import com.zincoid.gd.store.GdProgressStore;
import com.zincoid.gd.store.ProgressRecord;
import jdash.common.entity.GDLevel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * GD 进度服务：注册、进度维护与本地 B20 计算。
 * <p>GD progress service: registration, progress maintenance, and local B20 calculation.</p>
 */
@Slf4j
public final class GdProgressService implements AutoCloseable {

    private static final int FETCH_THREADS = 4;
    private static final int MAX_USER_AVATAR_BYTES = 5 * 1024 * 1024;

    private final GdProgressStore store;
    private final GdLevelService levels;
    private final GddlService gddl;
    private final GdConfig config;
    private final GdHttp http;
    private final Map<String, CacheEntry<byte[]>> userAvatarCache = new ConcurrentHashMap<>();

    /**
     * 创建进度服务。
     * <p>Creates the progress service.</p>
     *
     * @param store  the progress store
     * @param levels the GD level service
     * @param gddl   the GDDL service
     * @param config the plugin configuration
     * @param http   the shared HTTP helper
     */
    public GdProgressService(GdProgressStore store, GdLevelService levels,
                             GddlService gddl, GdConfig config, GdHttp http) {
        this.store = store;
        this.levels = levels;
        this.gddl = gddl;
        this.config = config;
        this.http = http;
    }

    // ════ Registration ═══════════

    /**
     * 用户是否已注册。
     * <p>Whether the user is registered.</p>
     *
     * @param userId the platform user id
     * @return true when registered
     */
    public boolean isRegistered(String userId) {
        return store.isRegistered(userId);
    }

    /**
     * 注册用户。
     * <p>Registers a user.</p>
     *
     * @param userId the platform user id
     * @return true when this is a new registration
     */
    public boolean register(String userId) {
        return store.register(userId);
    }

    /**
     * 注销用户并删除全部本地进度。
     * <p>Unregisters a user and deletes all local progress.</p>
     *
     * @param userId the platform user id
     * @return the number of deleted progress rows
     */
    public int unregister(String userId) {
        return store.unregister(userId);
    }

    /**
     * 返回用户的进度记录数量。
     * <p>Returns the number of progress records of a user.</p>
     *
     * @param userId the platform user id
     * @return the record count
     */
    public int progressCount(String userId) {
        return store.progressCount(userId);
    }

    // ════ Progress ═══════════════

    /**
     * 验证关卡并用其名称/作者写入进度。
     * <p>Validates the level and stores progress with its name and author.</p>
     *
     * @param userId   the platform user id
     * @param levelId  the GD level id
     * @param progress the completion percentage, 1-100
     * @return the stored level details, or empty when the level does not exist
     */
    public Optional<UpdateResult> update(String userId, long levelId, int progress) {
        if (progress < 1 || progress > 100)
            throw new GdException("Progress must be between 1 and 100");
        GDLevel level = levels.level(levelId);
        if (level == null) return Optional.empty();
        String name = level.name();
        if (name == null || name.isBlank()) name = "#" + levelId;
        String author = level.creatorName().orElse(null);
        store.upsertProgress(userId, levelId, progress, name, author);
        return Optional.of(new UpdateResult(name, author));
    }

    /**
     * 删除一条进度记录。
     * <p>Removes a progress record.</p>
     *
     * @param userId  the platform user id
     * @param levelId the GD level id
     * @return true when a record was removed
     */
    public boolean remove(String userId, long levelId) {
        return store.removeProgress(userId, levelId);
    }

    /**
     * 获取聊天用户头像。
     * <p>Fetches a chat user's avatar.</p>
     *
     * @param url the avatar url, nullable
     * @return the image bytes, or null when unavailable
     */
    public byte[] avatar(String url) {
        if (url == null || url.isBlank()) return null;
        CacheEntry<byte[]> cached = userAvatarCache.get(url);
        if (cached != null && !cached.expired(config.cacheTtl())) return cached.value();
        try {
            GdHttp.Response response = http.get(url, "image/*", MAX_USER_AVATAR_BYTES);
            if (!response.ok()) return null;
            if (response.body().length == 0 || !response.contentType().startsWith("image/"))
                return null;
            byte[] bytes = response.body();
            if (userAvatarCache.size() > 128) userAvatarCache.clear();
            userAvatarCache.put(url, new CacheEntry<>(bytes));
            return bytes;
        } catch (RuntimeException e) {
            if (cached != null) {
                log.warn("GD user avatar refresh failed; using stale: {}", url, e);
                return cached.value();
            }
            log.warn("GD user avatar fetch failed: {}", url, e);
            return null;
        }
    }

    // ════ Local B20 ══════════════

    /**
     * 根据本地进度与 GDDL 关卡评级计算 B20。
     * <p>Computes B20 from local progress and GDDL level ratings.</p>
     *
     * @param userId the platform user id
     * @return the local B20 result
     */
    public LocalB20Result localB20(String userId) {
        List<ProgressRecord> records = store.progress(userId);
        if (records.isEmpty())
            return new LocalB20Result(userId, 0, 0, 0, 0, 0, 0, 0, List.of());

        Set<Long> levelIds = new LinkedHashSet<>();
        for (ProgressRecord record : records) levelIds.add(record.levelId());
        Map<Long, CachedTier> effective = new HashMap<>(store.cachedTiers(levelIds));

        long now = System.currentTimeMillis();
        List<Long> stale = new ArrayList<>();
        for (long levelId : levelIds) {
            CachedTier cached = effective.get(levelId);
            if (cached == null || expired(cached, now)) stale.add(levelId);
        }
        if (!stale.isEmpty()) refresh(stale, effective);

        List<GdScore.Play> plays = new ArrayList<>();
        int beaten = 0;
        int beating = 0;
        int rated = 0;
        for (ProgressRecord record : records) {
            if (record.progress() == 100) beaten++;
            else beating++;
            CachedTier cached = effective.get(record.levelId());
            if (cached == null || cached.tier() == null) continue;
            String name = record.levelName();
            if (name == null || name.isBlank()) name = "#" + record.levelId();
            plays.add(new GdScore.Play(record.levelId(), name, cached.tier(), record.progress()));
            rated++;
        }

        GdScore.B20 b20 = GdScore.b20(plays, config.tierExponent());
        int highest = b20.top().isEmpty() ? 0 : b20.top().getFirst().play().tier();
        return new LocalB20Result(userId, beaten, beating, rated, records.size() - rated,
                b20.score(), highest, b20.sample(), b20.top());
    }

    @Override
    public void close() {
        store.close();
    }

    // ════ Helpers ═════════════════

    private boolean expired(CachedTier cached, long now) {
        Duration ttl = cached.tier() == null ? config.unratedCacheTtl() : config.tierCacheTtl();
        return now - cached.fetchedAt() >= ttl.toMillis();
    }

    private void refresh(Collection<Long> levelIds, Map<Long, CachedTier> effective) {
        List<FetchResult> results = fetch(levelIds);
        List<CachedTier> saves = new ArrayList<>();
        GdException fatal = null;
        for (FetchResult result : results) {
            if (result.entry() != null) {
                saves.add(result.entry());
                effective.put(result.id(), result.entry());
                continue;
            }
            if (effective.containsKey(result.id())) {
                log.warn("GDDL tier refresh failed; using stale: id={}", result.id(), result.error());
            } else if (fatal == null) {
                fatal = result.error();
            }
        }
        if (!saves.isEmpty()) store.saveTiers(saves);
        if (fatal != null) throw fatal;
    }

    private List<FetchResult> fetch(Collection<Long> levelIds) {
        List<Long> ids = new ArrayList<>(levelIds);
        int threads = Math.min(FETCH_THREADS, ids.size());
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            List<Future<FetchResult>> futures = new ArrayList<>(ids.size());
            for (long levelId : ids) futures.add(executor.submit(() -> fetchOne(levelId)));
            List<FetchResult> results = new ArrayList<>(ids.size());
            for (Future<FetchResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new GdException("GDDL tier fetch interrupted", e);
                } catch (ExecutionException e) {
                    throw new GdException("GDDL tier fetch failed", e.getCause());
                }
            }
            return results;
        }
    }

    private FetchResult fetchOne(long levelId) {
        try {
            GddlLevel level = gddl.gddlLevel(levelId, false);
            long now = System.currentTimeMillis();
            if (level == null)
                return new FetchResult(levelId, new CachedTier(levelId, null, null, now), null);
            Double rating = level.rating();
            Integer tier = tier(rating);
            if (tier == null) rating = null;
            return new FetchResult(levelId, new CachedTier(levelId, rating, tier, now), null);
        } catch (RuntimeException e) {
            GdException error = e instanceof GdException gdException
                    ? gdException : new GdException("GDDL level query failed", e);
            return new FetchResult(levelId, null, error);
        }
    }

    private static Integer tier(Double rating) {
        if (rating == null || rating.isNaN()) return null;
        return Math.max(1, (int) Math.round(rating));
    }

    /**
     * 更新结果：JDash 验证得到的关卡名称与作者。
     * <p>Update result: the level name and author returned by JDash validation.</p>
     *
     * @param levelName the level name
     * @param author    the level author, nullable
     */
    public record UpdateResult(String levelName, String author) {
    }

    /**
     * 本地 B20 结果。
     * <p>Local B20 result.</p>
     *
     * @param userId       the platform user id
     * @param beatenTotal  the number of 100% records
     * @param beatingTotal the number of in-progress records
     * @param ratedTotal   the number of records with a GDDL tier
     * @param unratedTotal the number of records without a GDDL tier
     * @param score        the B20 score
     * @param highestTier  the highest tier in the B20
     * @param sample       the number of records averaged
     * @param top          the scored records
     */
    public record LocalB20Result(
            String userId,
            int beatenTotal,
            int beatingTotal,
            int ratedTotal,
            int unratedTotal,
            double score,
            int highestTier,
            int sample,
            List<GdScore.Scored> top
    ) {
    }

    private record FetchResult(long id, CachedTier entry, GdException error) {
    }
}
