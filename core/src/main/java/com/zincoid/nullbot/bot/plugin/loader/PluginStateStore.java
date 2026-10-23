package com.zincoid.nullbot.bot.plugin.loader;

import com.zincoid.nullbot.bot.plugin.PluginException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 插件禁用状态存储：读写插件目录中的 {@code disabled.txt}。
 * <p>Plugin disabled-state store: reads and writes {@code disabled.txt} in the plugin directory.</p>
 */
@Slf4j
final class PluginStateStore {

    private static final String FILE_NAME = "disabled.txt";
    private static final Pattern JAR_NAME = Pattern.compile("[A-Za-z0-9._-]{1,128}\\.jar");

    private final Path dir;
    private final Set<String> disabled = new HashSet<>();

    private boolean loaded;

    PluginStateStore(Path dir) {
        this.dir = dir;
    }

    /**
     * 读取禁用列表，首次调用时生效。
     * <p>Reads the disabled list on the first call.</p>
     */
    synchronized void load() {
        if (loaded) return;
        Path file = dir.resolve(FILE_NAME);
        try {
            Files.createDirectories(dir);
            if (Files.isRegularFile(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String name = line.trim();
                    if (!name.isEmpty() && !name.startsWith("#") && JAR_NAME.matcher(name).matches())
                        disabled.add(name);
                }
            }
        } catch (IOException e) {
            log.warn("Cannot read plugin state: {}", file, e);
        }
        loaded = true;
    }

    /**
     * 判断 JAR 是否被禁用。
     * <p>Whether the jar is disabled.</p>
     *
     * @param jarName the plugin jar filename
     * @return true when disabled
     */
    synchronized boolean isDisabled(String jarName) {
        load();
        return disabled.contains(jarName);
    }

    /**
     * 设置 JAR 的禁用状态并持久化。
     * <p>Sets and persists the disabled state of a jar.</p>
     *
     * @param jarName the plugin jar filename
     * @param value   true to disable
     */
    synchronized void setDisabled(String jarName, boolean value) {
        load();
        if (disabled.contains(jarName) == value) return;
        Set<String> next = new HashSet<>(disabled);
        if (value) next.add(jarName);
        else next.remove(jarName);
        write(next);
        disabled.clear();
        disabled.addAll(next);
    }

    private void write(Set<String> names) {
        Path file = dir.resolve(FILE_NAME);
        try {
            Files.createDirectories(dir);
            Path temp = Files.createTempFile(dir, "disabled-", ".tmp");
            Files.write(temp, names.stream().sorted().toList(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PluginException("Cannot write plugin state: " + file, e);
        }
    }
}
