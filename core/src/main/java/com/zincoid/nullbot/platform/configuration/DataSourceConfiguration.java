package com.zincoid.nullbot.platform.configuration;

import com.zaxxer.hikari.HikariDataSource;
import com.zincoid.nullbot.platform.exception.PlatformException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

/**
 * 数据源配置：根据 {@code nullbot.db.type} 显式选择 MySQL 或 SQLite。
 * <p>Data source configuration: explicitly selects MySQL or SQLite according to {@code nullbot.db.type}.</p>
 */
@Slf4j
@Configuration
public class DataSourceConfiguration {

    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    private static final String SQLITE_SCHEMA = "schema/schema-sqlite.sql";

    /**
     * 创建数据源：按配置的数据库类型选择 MySQL 或 SQLite。
     * <p>Creates the data source: selects MySQL or SQLite according to the configured database type.</p>
     *
     * @param properties the database properties
     * @return the data source
     */
    @Bean
    public DataSource dataSource(DbProperties properties) {
        DbProperties.DbType type = properties.getType();
        if (type == null) throw new PlatformException("nullbot.db.type must be mysql or sqlite");
        return switch (type) {
            case MYSQL -> mysql(properties.getMysql());
            case SQLITE -> sqlite(properties.getSqlite().getPath());
        };
    }

    /**
     * 创建并验证 MySQL 连接池。
     * <p>Creates and verifies the MySQL connection pool.</p>
     *
     * @param config the MySQL connection properties
     * @return the MySQL data source
     */
    private static HikariDataSource mysql(DbProperties.Mysql config) {
        if (!StringUtils.hasText(config.getDatabase()) || !StringUtils.hasText(config.getUsername()))
            throw new PlatformException("MySQL is selected but nullbot.db.mysql.database or username is missing");
        String host = StringUtils.hasText(config.getHost()) ? config.getHost() : "localhost";
        int port = config.getPort() > 0 ? config.getPort() : 3306;
        String url = "jdbc:mysql://" + host + ":" + port + "/" + config.getDatabase();
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("nullbot-mysql");
        dataSource.setDriverClassName(MYSQL_DRIVER);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        dataSource.setConnectionTimeout(5000);
        try (Connection _ = dataSource.getConnection()) {
            log.info("MySQL connected: {}", url);
            return dataSource;
        } catch (Exception exception) {
            dataSource.close();
            throw new PlatformException("MySQL connection failed: " + url, exception);
        }
    }

    /**
     * 创建 SQLite 连接池并初始化表结构：建好父目录，拼装 WAL 与锁等待参数，单连接避免写锁竞争。
     * <p>Creates the SQLite connection pool and initializes the schema: creates the parent directory,
     * builds the URL with WAL and lock timeout, and uses one connection to avoid write-lock contention.</p>
     *
     * @param path the database file path
     * @return the SQLite data source
     */
    private static HikariDataSource sqlite(String path) {
        if (!StringUtils.hasText(path)) throw new PlatformException("nullbot.db.sqlite.path must not be blank");
        Path file = Path.of(path).toAbsolutePath().normalize();
        Path parent = file.getParent();
        if (parent != null)
            try {
                Files.createDirectories(parent);
            } catch (IOException exception) {
                throw new PlatformException("Cannot create SQLite directory: " + parent, exception);
            }
        String url = "jdbc:sqlite:" + file.toString().replace('\\', '/') + "?journal_mode=WAL&busy_timeout=5000";
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("nullbot-sqlite");
        dataSource.setDriverClassName(SQLITE_DRIVER);
        dataSource.setJdbcUrl(url);
        dataSource.setMaximumPoolSize(1);
        dataSource.setConnectionTimeout(5000);
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(SQLITE_SCHEMA));
        } catch (Exception exception) {
            dataSource.close();
            throw new PlatformException("Failed to initialize SQLite schema", exception);
        }
        log.info("SQLite connected: {}", file);
        return dataSource;
    }
}
