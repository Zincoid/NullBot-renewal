package com.zincoid.nullbot.console.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定自 {@code bot.console.*} 命名空间的控制台配置。
 * <p>Console configuration bound from the {@code bot.console.*} namespace.</p>
 */
@Getter
@Setter
@ConfigurationProperties("bot.console")
public class ConsoleProperties {

    /**
     * 控制台接口的静态访问令牌。
     * <p>Static access token of the console API.</p>
     */
    private String token;
}
