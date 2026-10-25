package com.zincoid.nullbot.bot.plugin.loader;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * 插件 JAR 信息：源文件名、私有副本与源文件指纹。
 * <p>Plugin jar info: source filename, private copy, and source file stamp.</p>
 *
 * @param name     the source jar filename
 * @param copy     the private jar copy used by the classloader
 * @param modified the source jar last-modified time
 * @param size     the source jar size in bytes
 */
record PluginJar(String name, Path copy, FileTime modified, long size) {
}
