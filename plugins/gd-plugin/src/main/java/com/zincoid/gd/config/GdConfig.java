package com.zincoid.gd.config;

import com.zincoid.nullbot.bot.plugin.PluginConfigProperties;

import java.time.Duration;

/**
 * GD 插件配置：从插件 config.properties 读取。
 * <p>GD plugin configuration read from the plugin config.properties.</p>
 *
 * @param userAgent      HTTP user agent
 * @param gddlBase       GDDL API base URL
 * @param gddlAvatarBase GDDL avatar CDN base URL
 * @param thumbnailBase  thumbnail service base URL
 * @param connectTimeout HTTP connect timeout
 * @param requestTimeout HTTP request timeout
 * @param retries        HTTP retry count after the first attempt
 * @param proxy          optional HTTP proxy, host:port
 * @param cacheTtl        cache lifetime for GDDL data and thumbnails
 * @param tierCacheTtl    cache lifetime for rated GDDL levels
 * @param unratedCacheTtl cache lifetime for unrated or missing GDDL levels
 * @param tierExponent    exponent applied to the GDDL tier in the B20 formula
 */
public record GdConfig(
        String userAgent,
        String gddlBase,
        String gddlAvatarBase,
        String thumbnailBase,
        Duration connectTimeout,
        Duration requestTimeout,
        int retries,
        String proxy,
        Duration cacheTtl,
        Duration tierCacheTtl,
        Duration unratedCacheTtl,
        double tierExponent
) {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int DEFAULT_RETRIES = 1;
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration DEFAULT_TIER_CACHE_TTL = Duration.ofHours(24);
    private static final Duration DEFAULT_UNRATED_CACHE_TTL = Duration.ofHours(1);
    private static final double DEFAULT_TIER_EXPONENT = 1.5;
    private static final String DEFAULT_GDDL_BASE = "https://gdladder.com/api";
    private static final String DEFAULT_GDDL_AVATAR_BASE = "https://cdn.gdladder.com/avatars/";
    private static final String DEFAULT_THUMBNAIL_BASE = "https://levelthumbs.prevter.me/thumbnail/";

    /**
     * 从原始配置构建 GD 插件配置。
     * <p>Builds the GD plugin configuration from raw config values.</p>
     *
     * @param properties the plugin config properties
     * @return the configuration
     */
    public static GdConfig load(PluginConfigProperties properties) {
        String userAgent = properties.get("http.user-agent", "NullBot-GD/1.0");
        String gddlBase = properties.get("gddl.api-base", DEFAULT_GDDL_BASE);
        String gddlAvatarBase = properties.get("gddl.avatar-base", DEFAULT_GDDL_AVATAR_BASE);
        String thumbnailBase = properties.get("thumbnail.base", DEFAULT_THUMBNAIL_BASE);
        Duration connectTimeout = seconds(properties.get("http.connect-timeout", ""),
                DEFAULT_CONNECT_TIMEOUT, 1, 120);
        Duration requestTimeout = seconds(properties.get("http.request-timeout", ""),
                DEFAULT_REQUEST_TIMEOUT, 1, 300);
        int retries = retries(properties.get("http.retries", ""), DEFAULT_RETRIES);
        String proxy = properties.get("http.proxy", "").trim();
        Duration cacheTtl = ttl(properties.get("cache-ttl", ""), DEFAULT_CACHE_TTL);
        Duration tierCacheTtl = seconds(properties.get("gddl.tier-cache-ttl", ""),
                DEFAULT_TIER_CACHE_TTL, 60, Duration.ofDays(30).toSeconds());
        Duration unratedCacheTtl = seconds(properties.get("gddl.unrated-cache-ttl", ""),
                DEFAULT_UNRATED_CACHE_TTL, 60, Duration.ofDays(7).toSeconds());
        double tierExponent = exponent(properties.get("score.tier-exponent", ""),
                DEFAULT_TIER_EXPONENT);
        return new GdConfig(
                userAgent.isBlank() ? "NullBot-GD/1.0" : userAgent,
                trimSlash(gddlBase, DEFAULT_GDDL_BASE),
                ensureSlash(gddlAvatarBase, DEFAULT_GDDL_AVATAR_BASE),
                ensureSlash(thumbnailBase, DEFAULT_THUMBNAIL_BASE),
                connectTimeout,
                requestTimeout,
                retries,
                proxy,
                cacheTtl,
                tierCacheTtl,
                unratedCacheTtl,
                tierExponent
        );
    }

    // ════ Helpers ═════════════════

    private static Duration ttl(String raw, Duration fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            long seconds = Long.parseLong(raw.trim());
            return seconds >= 30 ? Duration.ofSeconds(seconds) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Duration seconds(String raw, Duration fallback, long min, long max) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            long value = Long.parseLong(raw.trim());
            return value >= min && value <= max ? Duration.ofSeconds(value) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int retries(String raw, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= 0 && value <= 5 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double exponent(String raw, double fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            double value = Double.parseDouble(raw.trim());
            return value >= 1.0 && value <= 3.0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String trimSlash(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value.isEmpty() ? fallback : value;
    }

    private static String ensureSlash(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) value = fallback;
        return value.endsWith("/") ? value : value + "/";
    }
}
