package com.zincoid.nullbot.adapter.qq;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * QQ 官方机器人适配器配置。
 * <p>QQ official bot adapter configuration.</p>
 */
@Getter
@Setter
@ConfigurationProperties("qq")
public class QqProperties {

    private static final String DEFAULT_API_BASE = "https://api.bot.qq.com";
    private static final String DEFAULT_TOKEN_URL = DEFAULT_API_BASE + "/app/getAppAccessToken";

    /**
     * 开放平台 AppID。
     * <p>Open platform AppID.</p>
     */
    private String appId;

    /**
     * 开放平台 AppSecret。
     * <p>Open platform AppSecret.</p>
     */
    private String appSecret;

    /**
     * OpenAPI 基础地址，空值时使用官方地址。
     * <p>OpenAPI base URL; the official URL is used when blank.</p>
     */
    private String apiBase;

    /**
     * Access token 获取地址，空值时使用官方地址。
     * <p>Access token endpoint; the official URL is used when blank.</p>
     */
    private String tokenUrl;

    /**
     * 订阅的事件位掩码，默认群聊与 C2C。
     * <p>Event intent bitmask, group and C2C by default.</p>
     */
    private int intents = 1 << 25;

    /**
     * 分片编号。
     * <p>Shard id.</p>
     */
    private int shard = 0;

    /**
     * 分片总数。
     * <p>Shard count.</p>
     */
    private int shardCount = 1;

    /**
     * 单条文本的最大 Unicode code point 数。
     * <p>Maximum Unicode code points per text message.</p>
     */
    private int textLimit = 1800;

    /**
     * 是否已配置必要凭据。
     * <p>Whether the required credentials are configured.</p>
     *
     * @return true when AppID and AppSecret are present
     */
    public boolean configured() {
        return appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }

    /**
     * 返回规范化后的 OpenAPI 基础地址。
     * <p>Returns the normalized OpenAPI base URL.</p>
     *
     * @return the API base URL
     */
    public String resolvedApiBase() {
        return stripTrailingSlash(isBlank(apiBase) ? DEFAULT_API_BASE : apiBase.strip());
    }

    /**
     * 返回规范化后的 token 地址。
     * <p>Returns the normalized token endpoint.</p>
     *
     * @return the token endpoint
     */
    public String resolvedTokenUrl() {
        return isBlank(tokenUrl) ? DEFAULT_TOKEN_URL : tokenUrl.strip();
    }

    /**
     * 返回有效分片总数。
     * <p>Returns the effective shard count.</p>
     *
     * @return the shard count
     */
    public int resolvedShardCount() {
        return Math.max(1, shardCount);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String stripTrailingSlash(String value) {
        int end = value.length();
        while (end > 1 && value.charAt(end - 1) == '/') end--;
        return value.substring(0, end);
    }
}
