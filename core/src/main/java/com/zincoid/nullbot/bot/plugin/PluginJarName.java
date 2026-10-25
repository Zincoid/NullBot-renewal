package com.zincoid.nullbot.bot.plugin;

import java.util.regex.Pattern;

/**
 * 插件 JAR 文件名校验。
 * <p>Validation of plugin jar filenames.</p>
 */
public final class PluginJarName {

    private static final Pattern PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,128}\\.jar");

    private PluginJarName() {
    }

    /**
     * 判断文件名是否为合法的插件 JAR 名称。
     * <p>Checks whether the name is a valid plugin jar filename.</p>
     *
     * @param name the filename to check
     * @return true when the name is a valid plugin jar filename
     */
    public static boolean isValid(String name) {
        return name != null && PATTERN.matcher(name).matches();
    }
}
