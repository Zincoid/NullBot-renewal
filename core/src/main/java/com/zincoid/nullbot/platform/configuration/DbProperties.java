package com.zincoid.nullbot.platform.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定自 {@code bot.db.*} 命名空间的数据库配置。
 * <p>Database configuration bound from the {@code bot.db.*} namespace.</p>
 */
@Getter
@Setter
@ConfigurationProperties("bot.db")
public class DbProperties {

    /**
     * 数据库类型：{@code mysql} 或 {@code sqlite}。
     * <p>Database type: {@code mysql} or {@code sqlite}.</p>
     */
    private DbType type = DbType.SQLITE;

    /**
     * MySQL 连接配置。
     * <p>MySQL connection configuration.</p>
     */
    private Mysql mysql = new Mysql();

    /**
     * SQLite 连接配置。
     * <p>SQLite connection configuration.</p>
     */
    private Sqlite sqlite = new Sqlite();

    /**
     * 可选的数据库类型。
     * <p>Supported database types.</p>
     */
    public enum DbType {

        /**
         * MySQL。
         * <p>MySQL.</p>
         */
        MYSQL,

        /**
         * SQLite。
         * <p>SQLite.</p>
         */
        SQLITE
    }

    /**
     * MySQL 连接参数。
     * <p>MySQL connection parameters.</p>
     */
    @Getter
    @Setter
    public static class Mysql {

        /**
         * 主机名。
         * <p>Host name.</p>
         */
        private String host = "localhost";

        /**
         * 端口。
         * <p>Port.</p>
         */
        private int port = 3306;

        /**
         * 数据库名。
         * <p>Database name.</p>
         */
        private String database = "";

        /**
         * 用户名。
         * <p>User name.</p>
         */
        private String username = "";

        /**
         * 密码。
         * <p>Password.</p>
         */
        private String password = "";
    }

    /**
     * SQLite 连接参数。
     * <p>SQLite connection parameters.</p>
     */
    @Getter
    @Setter
    public static class Sqlite {

        /**
         * 数据库文件路径；目录和文件会在启动时自动创建。
         * <p>Database file path; the directory and file are created at startup.</p>
         */
        private String path = "./data/sqlite.db";
    }
}
