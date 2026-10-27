package com.zincoid.gd.service;

import com.zincoid.gd.GdException;
import com.zincoid.gd.config.GdConfig;
import jdash.client.GDClient;
import jdash.client.exception.ActionFailedException;
import jdash.common.LevelSearchFilter;
import jdash.common.LevelSearchMode;
import jdash.common.entity.GDLevel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GD 关卡数据服务：JDash 关卡搜索、详情与缩略图。
 * <p>GD level data service: JDash level search, details, and thumbnails.</p>
 */
@Slf4j
public final class GdLevelService {

    private static final int MAX_THUMBNAIL_BYTES = 5 * 1024 * 1024;
    private static final Duration JDASH_TIMEOUT = Duration.ofSeconds(20);

    private final GdConfig config;
    private final GdHttp http;
    private final GDClient gd;
    private final Map<Long, CacheEntry<byte[]>> thumbnailCache = new ConcurrentHashMap<>();

    /**
     * 创建关卡数据服务。
     * <p>Creates the level data service.</p>
     *
     * @param config the plugin configuration
     * @param http   the shared HTTP helper
     */
    public GdLevelService(GdConfig config, GdHttp http) {
        this.config = config;
        this.http = http;
        this.gd = GDClient.create();
    }

    /**
     * 搜索关卡，最多返回 10 条。
     * <p>Searches levels and returns at most 10 results.</p>
     *
     * @param query  the search keyword
     * @param filter the search filter
     * @param page   the zero-based page
     * @return the levels on the page
     */
    public List<GDLevel> search(String query, LevelSearchFilter filter, int page) {
        try {
            List<GDLevel> levels = gd.searchLevels(LevelSearchMode.SEARCH, query, filter, page)
                    .take(10)
                    .collectList()
                    .block(JDASH_TIMEOUT);
            return levels == null ? List.of() : levels;
        } catch (RuntimeException e) {
            if (isNotFound(e)) return List.of();
            throw new GdException("Geometry Dash search failed", e);
        }
    }

    /**
     * 按 ID 获取关卡。
     * <p>Fetches a level by id.</p>
     *
     * @param id the level id
     * @return the level, or null when not found
     */
    public GDLevel level(long id) {
        try {
            return gd.findLevelById(id).block(JDASH_TIMEOUT);
        } catch (RuntimeException e) {
            if (isNotFound(e)) return null;
            throw new GdException("Geometry Dash level query failed", e);
        }
    }

    /**
     * 获取关卡缩略图。
     * <p>Fetches the level thumbnail.</p>
     *
     * @param levelId the level id
     * @return the image bytes, or null when unavailable
     */
    public byte[] thumbnail(long levelId) {
        CacheEntry<byte[]> cached = thumbnailCache.get(levelId);
        if (cached != null && !cached.expired(config.cacheTtl())) return cached.value();
        try {
            GdHttp.Response response = http.get(
                    config.thumbnailBase() + levelId, "image/*", MAX_THUMBNAIL_BYTES);
            if (response.status() == 404) return null;
            if (!response.ok())
                throw new GdException("Thumbnail service returned HTTP " + response.status());
            if (response.body().length == 0 || !response.contentType().startsWith("image/"))
                return null;
            byte[] bytes = response.body();
            if (thumbnailCache.size() > 64) thumbnailCache.clear();
            thumbnailCache.put(levelId, new CacheEntry<>(bytes));
            return bytes;
        } catch (RuntimeException e) {
            if (cached != null) {
                log.warn("GD thumbnail refresh failed; using stale: id={}", levelId, e);
                return cached.value();
            }
            throw e instanceof GdException gdException
                    ? gdException
                    : new GdException("Thumbnail query failed", e);
        }
    }

    // ════ Helpers ═════════════════

    private static boolean isNotFound(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof ActionFailedException
                    && current.getMessage() != null
                    && current.getMessage().contains("Failed to load levels"))
                return true;
        }
        return false;
    }
}
