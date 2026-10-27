package com.zincoid.nullbot.bot.plugin;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 插件自己的 config.properties 配置快照。
 * <p>Snapshot of a plugin's own config.properties.</p>
 */
@Slf4j
public final class PluginConfigProperties {

    /**
     * 插件 JAR 内默认配置资源的路径。
     * <p>Classpath path of the default config resource inside a plugin jar.</p>
     */
    public static final String RESOURCE = "/config.properties";

    private final Path file;
    private final Properties properties = new Properties();

    private PluginConfigProperties(Path file) {
        this.file = file;
    }

    /**
     * 从插件数据目录加载配置。
     * <p>Loads the configuration from a plugin data directory.</p>
     *
     * @param dataDir the plugin data directory
     * @return the loaded configuration, empty when the file is absent
     */
    public static PluginConfigProperties load(Path dataDir) {
        PluginConfigProperties config = new PluginConfigProperties(
                dataDir.resolve(Path.of(RESOURCE).getFileName()));
        if (!Files.isRegularFile(config.file)) return config;
        try (Reader reader = Files.newBufferedReader(config.file, StandardCharsets.UTF_8)) {
            config.properties.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Cannot read plugin config: {}", config.file, e);
        }
        return config;
    }

    /**
     * 返回配置项，缺失时使用默认值。
     * <p>Returns a configuration value, falling back to the given default.</p>
     *
     * @param key          the configuration key
     * @param defaultValue the fallback value
     * @return the configured value, or the fallback
     */
    public String get(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null ? defaultValue : value;
    }

    /**
     * 返回整数配置项。
     * <p>Returns an integer configuration value.</p>
     *
     * @param key          the configuration key
     * @param defaultValue the fallback value
     * @return the configured integer, or the fallback when missing or invalid
     */
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 返回浮点配置项。
     * <p>Returns a floating-point configuration value.</p>
     *
     * @param key          the configuration key
     * @param defaultValue the fallback value
     * @return the configured double, or the fallback when missing or invalid
     */
    public double getDouble(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 返回布尔配置项。
     * <p>Returns a boolean configuration value.</p>
     *
     * @param key          the configuration key
     * @param defaultValue the fallback value
     * @return the configured boolean, or the fallback when missing or invalid
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) return defaultValue;
        String normalized = value.trim();
        if ("true".equalsIgnoreCase(normalized)) return true;
        if ("false".equalsIgnoreCase(normalized)) return false;
        return defaultValue;
    }

    /**
     * 返回配置文件路径。
     * <p>Returns the configuration file path.</p>
     *
     * @return the configuration file path
     */
    public Path file() {
        return file;
    }

    /**
     * 配置文件是否存在。
     * <p>Whether the configuration file exists.</p>
     *
     * @return true when the file exists
     */
    public boolean exists() {
        return Files.isRegularFile(file);
    }
}
