package com.zincoid.nullbot.platform.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定自 {@code bot.sqlite.*} 命名空间的 SQLite 回退配置。
 * <p>SQLite fallback configuration bound from the {@code bot.sqlite.*} namespace.</p>
 */
@Getter
@Setter
@ConfigurationProperties("bot.sqlite")
public class SqliteProperties {

    /**
     * 是否在 MySQL 配置缺失或无法连接时回退到 SQLite。
     * <p>Whether to fall back to SQLite when MySQL is incomplete or unreachable.</p>
     */
    private boolean fallback = true;

    /**
     * SQLite 数据库文件路径；回退时目录和文件会自动创建。
     * <p>SQLite database file path; the directory and file are created automatically on fallback.</p>
     */
    private String path = "./data/sqlite.db";
}
