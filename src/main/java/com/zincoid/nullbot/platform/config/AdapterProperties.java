package com.zincoid.nullbot.platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Adapter selection bound from the {@code bot.*} namespace.
 * <p>绑定自 {@code bot.*} 命名空间的适配器选择配置。</p>
 */
@Getter
@Setter
@ConfigurationProperties("bot")
public class AdapterProperties {

    /**
     * Active bot framework adapter.
     * <p>当前生效的机器人框架适配器。</p>
     */
    private Adapter adapter = Adapter.SHIRO;

    /**
     * Supported values for bot.adapter.
     * <p>bot.adapter 的可选值。</p>
     */
    public enum Adapter {

        /**
         * Via the terminal.
         * <p>通过终端。</p>
         */
        STDIN,

        /**
         * Via a framework - Shiro (QQ).
         * <p>通过机器人框架 - Shiro（QQ）。</p>
         */
        SHIRO
    }
}
