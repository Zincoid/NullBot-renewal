package com.zincoid.nullbot.platform.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * File storage configuration bound from the {@code bot.file.*} namespace.
 * <p>绑定自 {@code bot.file.*} 命名空间的文件存储配置。</p>
 */
@Getter
@Setter
@ConfigurationProperties("bot.file")
public class FileProperties {

    /**
     * Storage root directory for flat file storage.
     * <p>平铺文件存储的根目录。</p>
     */
    private String root = "./files";
}
