package com.zincoid.nullbot.platform.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定自 {@code bot.plugin.*} 命名空间的外部插件配置。
 * <p>External plugin configuration bound from the {@code bot.plugin.*} namespace.</p>
 */
@Getter
@Setter
@ConfigurationProperties("bot.plugin")
public class PluginProperties {

    /**
     * 是否在启动时加载外部插件。
     * <p>Whether to load external plugins at startup.</p>
     */
    private boolean enabled = true;

    /**
     * 外部插件 JAR 所在目录。
     * <p>Directory containing external plugin JARs.</p>
     */
    private String dir = "./plugins";
}
