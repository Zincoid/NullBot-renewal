package com.zincoid.query;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

/**
 * 查询插件配置：环境变量优先，其次读取数据目录中的 config.properties。
 * <p>Query plugin configuration: environment variables win over config.properties in the data directory.</p>
 *
 * @param aaApiKey        Artificial Analysis API key
 * @param sauceNaoApiKey  SauceNAO API key
 * @param aaCacheTtl      AA cache lifetime
 * @param prtsCacheTtl    PRTS cache lifetime
 * @param userAgent       HTTP user agent
 */
@Slf4j
public record QueryConfig(
        String aaApiKey,
        String sauceNaoApiKey,
        Duration aaCacheTtl,
        Duration prtsCacheTtl,
        String userAgent
) {

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(10);

    /**
     * 读取插件配置。
     * <p>Loads the plugin configuration.</p>
     *
     * @param dataDir plugin data directory
     * @return the configuration
     */
    public static QueryConfig load(Path dataDir) {
        Properties properties = new Properties();
        Path file = dataDir.resolve("config.properties");
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                log.warn("Cannot read query plugin config: {}", file, e);
            }
        }
        String userAgent = env("HTTP_USER_AGENT",
                properties.getProperty("http.user-agent", "NullBot-Query/1.0"));
        return new QueryConfig(
                env("AA_API_KEY", properties.getProperty("aa.api-key", "")),
                env("SAUCENAO_API_KEY", properties.getProperty("saucenao.api-key", "")),
                ttl(env("AA_CACHE_TTL", properties.getProperty("aa.cache-ttl", "")), DEFAULT_CACHE_TTL),
                ttl(env("PRTS_CACHE_TTL", properties.getProperty("prts.cache-ttl", "")), DEFAULT_CACHE_TTL),
                userAgent.isBlank() ? "NullBot-Query/1.0" : userAgent
        );
    }

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
}
