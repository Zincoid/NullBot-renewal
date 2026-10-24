package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.AboutView;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;

/**
 * 控制台关于接口：公开返回应用与构建信息。
 * <p>Console about API: publicly returns application and build information.</p>
 */
@RestController
@RequestMapping("/console/about")
public class AboutController {

    private static final String AUTHOR = "Zincoid";

    private final String name;
    private final String version;
    private final String buildTime;
    private final String javaVersion;
    private final String springBootVersion;

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
        this.name = name;
        this.version = build == null || build.getVersion() == null ? "dev" : build.getVersion();
        this.buildTime = build == null || build.getTime() == null ? null : build.getTime().toString();
        this.javaVersion = System.getProperty("java.version");
        this.springBootVersion = SpringBootVersion.getVersion();
    }

    // ════ Query ══════════════════

    /**
     * 查询应用、构建与运行信息。
     * <p>Returns application, build, and runtime information.</p>
     *
     * @return the application, build, and runtime information
     */
    @GetMapping
    public AboutView about() {
        return new AboutView(name, AUTHOR, version, buildTime, javaVersion, springBootVersion,
                ManagementFactory.getRuntimeMXBean().getUptime());
    }
}
