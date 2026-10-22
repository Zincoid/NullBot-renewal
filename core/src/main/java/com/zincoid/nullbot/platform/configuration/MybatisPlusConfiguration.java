package com.zincoid.nullbot.platform.configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.zincoid.nullbot.platform.util.PageUtils;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * MyBatis-Plus 分页拦截器配置。
 * <p>MyBatis-Plus pagination interceptor configuration.</p>
 */
@Configuration
public class MybatisPlusConfiguration {

    /**
     * 注册分页拦截器，按数据库类型选择方言并限制单页最大条数。
     * <p>Registers the pagination interceptor, selects the dialect by database type, and caps the page size.</p>
     *
     * @param properties the database properties
     * @return the pagination inner interceptor
     */
    @Bean
    public PaginationInnerInterceptor paginationInnerInterceptor(DbProperties properties) {
        DbType dbType = properties.getType() == DbProperties.DbType.SQLITE ? DbType.SQLITE : DbType.MYSQL;
        PaginationInnerInterceptor interceptor = new PaginationInnerInterceptor(dbType);
        interceptor.setMaxLimit(PageUtils.MAX_SIZE);
        return interceptor;
    }

    /**
     * 注册数据库厂商标识，供 XML 中的 {@code databaseId} 选择方言 SQL。
     * <p>Registers the vendor database id used by XML {@code databaseId} statements.</p>
     *
     * @return the database id provider
     */
    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("MySQL", "mysql");
        properties.setProperty("SQLite", "sqlite");
        provider.setProperties(properties);
        return provider;
    }
}
