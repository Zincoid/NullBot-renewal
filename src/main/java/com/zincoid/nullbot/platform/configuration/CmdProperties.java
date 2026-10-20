package com.zincoid.nullbot.platform.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Command configuration bound from the {@code bot.cmd.*} namespace.
 * <p>绑定自 {@code bot.cmd.*} 命名空间的指令配置。</p>
 */
@Getter
@Setter
@ConfigurationProperties("bot.cmd")
public class CmdProperties {

    /**
     * Command prefix.
     * <p>指令前缀。</p>
     */
    private String prefix = "/";

    /**
     * Whether command names match case-insensitively.
     * <p>指令名是否忽略大小写。</p>
     */
    private boolean ignoreCase = true;
}
