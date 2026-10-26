package com.zincoid.gd;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

/**
 * GD 插件配置：环境变量优先，其次读取数据目录中的 config.properties。
 * <p>GD plugin configuration: environment variables win over config.properties in the data directory.</p>
 *
 * @param userAgent     HTTP user agent
 * @param gddlBase      GDDL API base URL
 * @param thumbnailBase thumbnail service base URL
 * @param cacheTtl      cache lifetime for GDDL data
 * @param tierExponent  exponent applied to the GDDL tier in the B20 formula
 */
@Slf4j
record GdConfig(
        String userAgent,
        String gddlBase,
        String thumbnailBase,
        Duration cacheTtl,
        double tierExponent
) {

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(10);
    private static final double DEFAULT_TIER_EXPONENT = 1.5;
    private static final String DEFAULT_GDDL_BASE = "https://gdladder.com/api";
    private static final String DEFAULT_THUMBNAIL_BASE = "https://levelthumbs.prevter.me/thumbnail/";

    /**
     * 读取插件配置。
     * <p>Loads the plugin configuration.</p>
     *
     * @param dataDir plugin data directory
     * @return the configuration
     */
    static GdConfig load(Path dataDir) {
        Properties properties = new Properties();
        Path file = dataDir.resolve("config.properties");
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                log.warn("Cannot read GD plugin config: {}", file, e);
            }
        }
        String userAgent = env("GD_HTTP_USER_AGENT",
                properties.getProperty("http.user-agent", "NullBot-GD/1.0"));
        String gddlBase = env("GD_GDDL_API_BASE",
                properties.getProperty("gddl.api-base", DEFAULT_GDDL_BASE));
        String thumbnailBase = env("GD_THUMBNAIL_BASE",
                properties.getProperty("thumbnail.base", DEFAULT_THUMBNAIL_BASE));
        Duration cacheTtl = ttl(env("GD_CACHE_TTL",
                properties.getProperty("cache-ttl", "")), DEFAULT_CACHE_TTL);
        double tierExponent = exponent(env("GD_TIER_EXPONENT",
                properties.getProperty("score.tier-exponent", "")), DEFAULT_TIER_EXPONENT);
        return new GdConfig(
                userAgent.isBlank() ? "NullBot-GD/1.0" : userAgent,
                trimSlash(gddlBase, DEFAULT_GDDL_BASE),
                ensureSlash(thumbnailBase, DEFAULT_THUMBNAIL_BASE),
                cacheTtl,
                tierExponent
        );
    }

    // ════ Helpers ═════════════════

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Duration ttl(String raw, Duration fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            long seconds = Long.parseLong(raw.trim());
            return seconds >= 30 ? Duration.ofSeconds(seconds) : fallback;
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
