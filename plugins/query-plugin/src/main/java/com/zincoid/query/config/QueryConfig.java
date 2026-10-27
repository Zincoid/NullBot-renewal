package com.zincoid.query.config;

import com.zincoid.nullbot.bot.plugin.PluginConfigProperties;

import java.time.Duration;

/**
 * 查询插件配置：从插件 config.properties 读取。
 * <p>Query plugin configuration read from the plugin config.properties.</p>
 *
 * @param aaApiKey        Artificial Analysis API key
 * @param sauceNaoApiKey  SauceNAO API key
 * @param aaCacheTtl      AA cache lifetime
 * @param prtsCacheTtl    PRTS cache lifetime
 * @param userAgent       HTTP user agent
 */
public record QueryConfig(
        String aaApiKey,
        String sauceNaoApiKey,
        Duration aaCacheTtl,
        Duration prtsCacheTtl,
        String userAgent
) {

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(10);

    /**
     * 从原始配置构建查询插件配置。
     * <p>Builds the query plugin configuration from raw config values.</p>
     *
     * @param properties the plugin config properties
     * @return the configuration
     */
    public static QueryConfig load(PluginConfigProperties properties) {
        String userAgent = properties.get("http.user-agent", "NullBot-Query/1.0");
        return new QueryConfig(
                properties.get("aa.api-key", ""),
                properties.get("saucenao.api-key", ""),
                ttl(properties.get("aa.cache-ttl", ""), DEFAULT_CACHE_TTL),
                ttl(properties.get("prts.cache-ttl", ""), DEFAULT_CACHE_TTL),
                userAgent.isBlank() ? "NullBot-Query/1.0" : userAgent
        );
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
}
