package com.zincoid.nullbot.console.configuration;

import com.zincoid.nullbot.console.interceptor.ConsoleAuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 注册控制台接口的令牌拦截器与页面入口。
 * <p>Registers the token interceptor and page entry of the console.</p>
 */
@Slf4j
@Configuration
public class ConsoleConfiguration implements WebMvcConfigurer {

    private final String token;

    /**
     * 读取配置的令牌；缺失时生成一次性令牌。
     * <p>Reads the configured token, generating an ephemeral one when absent.</p>
     *
     * @param properties console properties
     */
    public ConsoleConfiguration(ConsoleProperties properties) {
        String configured = properties.getToken();
        this.token = configured == null || configured.isBlank()
                ? generateToken()
                : configured.trim();
    }

    /**
     * 把令牌拦截器挂到 {@code /console/**}，页面静态资源除外。
     * <p>Attaches the token interceptor to {@code /console/**}, except the page assets.</p>
     *
     * @param registry interceptor registry
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new ConsoleAuthInterceptor(token))
                .addPathPatterns("/console/**")
                .excludePathPatterns("/console/ui", "/console/ui/**", "/console/about");
    }

    /**
     * 控制台静态资源使用协商缓存，更新后无需强制刷新。
     * <p>Serves console static resources with revalidation, so updates need no hard refresh.</p>
     *
     * @param registry resource handler registry
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/console/ui/**")
                .addResourceLocations("classpath:/static/console/ui/")
                .setCacheControl(CacheControl.noCache());
    }

    /**
     * 把控制台页面入口重定向到静态首页。
     * <p>Redirects the console page entry to the static index.</p>
     *
     * @param registry view controller registry
     */
    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        registry.addRedirectViewController("/console/ui", "/console/ui/index.html")
                .setKeepQueryParams(true);
        registry.addRedirectViewController("/console/ui/", "/console/ui/index.html")
                .setKeepQueryParams(true);
        registry.addRedirectViewController("/console/ui/login", "/console/ui/login.html")
                .setKeepQueryParams(true);
    }

    /**
     * 启动完成后输出带令牌的控制台页面地址。
     * <p>Logs the console page URL with the token after the web server starts.</p>
     *
     * @param event the web server initialized event
     */
    @EventListener
    public void onWebServerReady(WebServerInitializedEvent event) {
        log.info("Console UI: http://localhost:{}/console/ui?token={}",
                event.getWebServer().getPort(),
                UriUtils.encodeQueryParam(token, StandardCharsets.UTF_8));
    }

    // ════ Helpers ═════════════════

    /**
     * 生成 URL 安全的随机令牌。
     * <p>Generates a URL-safe random token.</p>
     *
     * @return the generated token
     */
    private static String generateToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
