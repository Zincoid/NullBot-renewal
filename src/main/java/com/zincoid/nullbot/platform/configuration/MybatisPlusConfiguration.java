package com.zincoid.nullbot.platform.configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页拦截器配置。
 * <p>MyBatis-Plus pagination interceptor configuration.</p>
 */
@Configuration
public class MybatisPlusConfiguration {

    /**
     * 注册分页拦截器，限制单页最大条数。
     * <p>Registers the pagination interceptor and caps the page size.</p>
     *
     * @return the pagination inner interceptor
     */
    @Bean
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        PaginationInnerInterceptor interceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        interceptor.setMaxLimit(100L);
        return interceptor;
    }
}
