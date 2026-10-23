package com.zincoid.nullbot.console.dto;

/**
 * 控制台关于视图。
 * <p>Console view of application and build information.</p>
 *
 * @param name              the application name
 * @param author            the author
 * @param version           the build version
 * @param buildTime         the build time in ISO-8601, null when unavailable
 * @param javaVersion       the running Java version
 * @param springBootVersion the running Spring Boot version
 */
public record AboutView(
        String name,
        String author,
        String version,
        String buildTime,
        String javaVersion,
        String springBootVersion
) {
}
