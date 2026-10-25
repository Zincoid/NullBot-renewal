package com.zincoid.nullbot.platform.util;

/**
 * 运行时环境信息。
 * <p>Runtime environment information.</p>
 */
public final class SystemInfo {

    private SystemInfo() {
    }

    /**
     * Java 版本。
     * <p>Java version.</p>
     *
     * @return the Java version
     */
    public static String javaVersion() {
        return System.getProperty("java.version", "unknown");
    }

    /**
     * Java 供应商。
     * <p>Java vendor.</p>
     *
     * @return the Java vendor
     */
    public static String javaVendor() {
        return System.getProperty("java.vendor", "unknown");
    }

    /**
     * Java 版本与供应商。
     * <p>Java version and vendor.</p>
     *
     * @return the Java description
     */
    public static String java() {
        return "%s (%s)".formatted(javaVersion(), javaVendor());
    }

    /**
     * 操作系统名称、版本与架构。
     * <p>Operating system name, version, and architecture.</p>
     *
     * @return the OS description
     */
    public static String os() {
        return "%s %s (%s)".formatted(
                System.getProperty("os.name", "unknown"),
                System.getProperty("os.version", "unknown"),
                System.getProperty("os.arch", "unknown"));
    }
}
