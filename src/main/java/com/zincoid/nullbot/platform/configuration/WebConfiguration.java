package com.zincoid.nullbot.platform.configuration;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 在 {@code /files/**} 路径下对外提供已存储的文件。
 * <p>Serves the stored files under the {@code /files/**} path.</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebConfiguration implements WebMvcConfigurer {

    private final FileProperties fileProperties;

    /**
     * 注册文件存储根目录的资源映射。
     * <p>Registers the resource mapping for the file storage root.</p>
     *
     * @param registry resource handler registry
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String root = Path.of(fileProperties.getRoot()).toAbsolutePath().normalize().toUri().toString();
        if (!root.endsWith("/")) root += "/";
        registry.addResourceHandler("/files/**")
                .addResourceLocations(root);
    }
}
