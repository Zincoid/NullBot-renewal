package com.zincoid.nullbot.bot.plugin;

import com.zincoid.nullbot.platform.util.AtomicFiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 插件资源工具：把插件 JAR 内的 classpath 资源解压到插件数据目录。
 * <p>Plugin resource utility: extracts classpath resources from a plugin jar into a plugin data directory.</p>
 */
public final class PluginResources {

    private PluginResources() {
    }

    /**
     * 解压资源到目标目录，目标文件已存在时跳过。
     * <p>Extracts a resource into the target directory, skipping an existing file.</p>
     *
     * @param anchor   a class loaded by the classloader that owns the resource, usually the plugin class
     * @param resource the absolute classpath resource path, such as {@code /Erangel.png}
     * @param dir      the target directory; the filename is derived from the resource path
     * @return the extracted file path
     * @throws PluginException when the resource is missing or cannot be extracted
     */
    public static Path extract(Class<?> anchor, String resource, Path dir) {
        Path name = Path.of(resource).getFileName();
        if (name == null)
            throw new PluginException("Invalid plugin resource: " + resource);
        Path target = dir.resolve(name);
        if (Files.isRegularFile(target)) return target;
        try (InputStream in = anchor.getResourceAsStream(resource)) {
            if (in == null)
                throw new PluginException("Plugin resource missing: " + resource);
            Files.createDirectories(dir);
            Path temp = Files.createTempFile(dir, ".resource-", ".tmp");
            try {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                AtomicFiles.moveReplace(temp, target);
            } finally {
                Files.deleteIfExists(temp);
            }
            return target;
        } catch (IOException e) {
            throw new PluginException("Cannot extract plugin resource: " + resource, e);
        }
    }
}
