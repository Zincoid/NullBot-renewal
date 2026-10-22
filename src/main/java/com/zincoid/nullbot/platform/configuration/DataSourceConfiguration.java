package com.zincoid.nullbot.platform.configuration;

import com.zaxxer.hikari.HikariDataSource;
import com.zincoid.nullbot.platform.exception.PlatformException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据源配置：优先使用 MySQL，配置缺失或无法连接时回退到 SQLite。
 * <p>Data source configuration: prefers MySQL and falls back to SQLite when it is missing or unreachable.</p>
 */
@Slf4j
@Configuration
public class DataSourceConfiguration {

    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    private static final String SQLITE_SCHEMA = "schema/schema-sqlite.sql";

    /**
     * 创建数据源：优先 MySQL，配置缺失或无法连接时按设置回退到 SQLite。
     * <p>Creates the data source: prefers MySQL and falls back to SQLite when allowed.</p>
     *
     * @param environment the environment
     * @param properties  the SQLite fallback properties
     * @return the data source
     */
    @Bean
    public DataSource dataSource(Environment environment, SqliteProperties properties) {
        String url = environment.getProperty("spring.datasource.url", "");
        String username = environment.getProperty("spring.datasource.username", "");
        String password = environment.getProperty("spring.datasource.password", "");
        String driver = environment.getProperty("spring.datasource.driver-class-name", MYSQL_DRIVER);
        if (!url.isBlank() && !username.isBlank()) {
            HikariDataSource mysql = mysql(url, username, password, driver);
            try {
                log.info("MySQL connected: {}", probe(mysql));
                return mysql;
            } catch (Exception exception) {
                mysql.close();
                if (!properties.isFallback()) throw new PlatformException("MySQL connection failed and SQLite fallback is disabled", exception);
                log.warn("MySQL unavailable ({}), using SQLite at {}", rootMessage(exception), properties.getPath());
            }
        } else {
            if (!properties.isFallback()) throw new PlatformException("MySQL is not configured and SQLite fallback is disabled");
            log.warn("MySQL is not configured, using SQLite at {}", properties.getPath());
        }
        return sqlite(sqliteUrl(properties.getPath()));
    }

    /**
     * 创建 MySQL 连接池。
     * <p>Creates the MySQL connection pool.</p>
     *
     * @param url      the JDBC URL
     * @param username the database user
     * @param password the database password
     * @param driver   the JDBC driver class name
     * @return the MySQL data source
     */
    private static HikariDataSource mysql(String url, String username, String password, String driver) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("nullbot-mysql");
        dataSource.setDriverClassName(driver);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setConnectionTimeout(5000);
        return dataSource;
    }

    /**
     * 验证 MySQL 连接并返回当前数据库名。
     * <p>Validates the MySQL connection and returns the selected database name.</p>
     *
     * @param dataSource the data source
     * @return the selected database name
     * @throws SQLException when the connection is invalid or no database is selected
     */
    private static String probe(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) throw new SQLException("Connection is not valid");
            String catalog = connection.getCatalog();
            if (catalog == null || catalog.isBlank()) throw new SQLException("No database selected");
            return catalog;
        }
    }

    /**
     * 创建 SQLite 连接池并初始化表结构，单连接避免写锁竞争。
     * <p>Creates the SQLite connection pool and initializes the schema; one connection avoids write-lock contention.</p>
     *
     * @param url the JDBC URL
     * @return the SQLite data source
     */
    private static HikariDataSource sqlite(String url) {
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
        return dataSource;
    }

    /**
     * 把文件路径转换成带 WAL 与锁等待的 SQLite JDBC 地址。
     * <p>Converts a file path to a SQLite JDBC URL with WAL and lock timeout enabled.</p>
     *
     * @param path the database file path
     * @return the SQLite JDBC URL
     */
    private static String sqliteUrl(String path) {
        Path file = Path.of(path).toAbsolutePath().normalize();
        Path parent = file.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException exception) {
                throw new PlatformException("Cannot create SQLite directory: " + parent, exception);
            }
        }
        return "jdbc:sqlite:" + file.toString().replace('\\', '/') + "?journal_mode=WAL&busy_timeout=5000";
    }

    /**
     * 取最内层异常的消息用于日志。
     * <p>Returns the innermost exception message for logging.</p>
     *
     * @param throwable the exception
     * @return the innermost message
     */
    private static String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
