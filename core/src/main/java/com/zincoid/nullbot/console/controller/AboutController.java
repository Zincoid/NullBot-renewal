package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.AboutView;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制台关于接口：公开返回应用与构建信息。
 * <p>Console about API: publicly returns application and build information.</p>
 */
@RestController
@RequestMapping("/console/about")
public class AboutController {

    private static final String AUTHOR = "Zincoid";

    private final AboutView about;

    /**
     * 汇总应用名称与构建信息，缺失构建信息时回退为开发模式。
     * <p>Collects the application name and build information, falling back to development mode.</p>
     *
     * @param builds the build properties provider
     * @param name   the configured application name
     */
    public AboutController(ObjectProvider<BuildProperties> builds,
                           @Value("${spring.application.name:NullBot}") String name) {
        BuildProperties build = builds.getIfAvailable();
        String version = build == null || build.getVersion() == null ? "dev" : build.getVersion();
        String buildTime = build == null || build.getTime() == null ? null : build.getTime().toString();
        this.about = new AboutView(name, AUTHOR, version, buildTime,
                System.getProperty("java.version"), SpringBootVersion.getVersion());
    }

    // ════ Query ══════════════════

    /**
     * 查询应用与构建信息。
     * <p>Returns application and build information.</p>
     *
     * @return the application and build information
     */
    @GetMapping
    public AboutView about() {
        return about;
    }
}
