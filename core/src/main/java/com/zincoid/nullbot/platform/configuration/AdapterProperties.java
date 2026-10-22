package com.zincoid.nullbot.platform.configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定自 {@code bot.*} 命名空间的适配器选择配置。
 * <p>Adapter selection bound from the {@code bot.*} namespace.</p>
 */
@Slf4j
@Getter
@Setter
@ConfigurationProperties("bot")
public class AdapterProperties {

    /**
     * 当前生效的机器人框架适配器。
     * <p>Active bot framework adapter.</p>
     */
    private Adapter adapter = Adapter.SHIRO;

    /**
     * 启动时输出当前生效的适配器。
     * <p>Logs the active adapter at startup.</p>
     */
    @PostConstruct
    void init() {
        log.info("Bot adapter: {}", adapter);
    }

    /**
     * bot.adapter 的可选值。
     * <p>Supported values for bot.adapter.</p>
     */
    public enum Adapter {

        /**
         * 通过终端。
         * <p>Via the terminal.</p>
         */
        STDIN,

        /**
         * 通过机器人框架 - Shiro（QQ）。
         * <p>Via a framework - Shiro (QQ).</p>
         */
        SHIRO
    }
}
