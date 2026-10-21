package com.zincoid.nullbot.platform.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定自 {@code bot.cmd.*} 命名空间的指令配置。
 * <p>Command configuration bound from the {@code bot.cmd.*} namespace.</p>
 */
@Getter
@Setter
@ConfigurationProperties("bot.cmd")
public class CmdProperties {

    /**
     * 指令前缀。
     * <p>Command prefix.</p>
     */
    private String prefix = "/";

    /**
     * 指令名是否忽略大小写。
     * <p>Whether command names match case-insensitively.</p>
     */
    private boolean ignoreCase = true;
}
