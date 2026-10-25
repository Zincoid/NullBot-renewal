package com.zincoid.nullbot.bot.plugin.loader;

import com.zincoid.nullbot.bot.plugin.PluginException;
import com.zincoid.nullbot.bot.plugin.PluginJarName;
import com.zincoid.nullbot.platform.util.AtomicFiles;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 插件状态存储：读写插件目录中的 {@code disabled.txt} 与 {@code plugin-ids.txt}。
 * <p>Plugin state store: reads and writes {@code disabled.txt} and {@code plugin-ids.txt}
 * in the plugin directory.</p>
 */
@Slf4j
final class PluginStateStore {

    private static final String FILE_NAME = "disabled.txt";
    private static final String IDS_FILE = "plugin-ids.txt";
    private static final Pattern PLUGIN_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final Path dir;
    private final Set<String> disabled = new HashSet<>();
    private final Map<String, String> ids = new HashMap<>();

    private boolean loaded;

    PluginStateStore(Path dir) {
        this.dir = dir;
    }

    /**
     * 读取插件状态文件，首次调用时生效。
     * <p>Reads the plugin state files on the first call.</p>
     */
    synchronized void load() {
        if (loaded) return;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("Cannot create plugin directory: {}", dir, e);
        }
        loadDisabled();
        loadIds();
        loaded = true;
    }

    private void loadDisabled() {
        Path file = dir.resolve(FILE_NAME);
        try {
            if (!Files.isRegularFile(file)) return;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String name = line.trim();
                if (!name.isEmpty() && !name.startsWith("#") && PluginJarName.isValid(name))
                    disabled.add(name);
            }
        } catch (IOException e) {
            log.warn("Cannot read plugin state: {}", file, e);
        }
    }

    private void loadIds() {
        Path file = dir.resolve(IDS_FILE);
        try {
            if (!Files.isRegularFile(file)) return;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String value = line.trim();
                if (value.isEmpty() || value.startsWith("#")) continue;
                int split = value.indexOf('=');
                if (split <= 0) continue;
                String jar = value.substring(0, split).trim();
                String id = value.substring(split + 1).trim();
                if (PluginJarName.isValid(jar) && PLUGIN_ID.matcher(id).matches())
                    ids.put(jar, id);
            }
        } catch (IOException e) {
            log.warn("Cannot read plugin ids: {}", file, e);
        }
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
     * 返回 JAR 最近一次解析到的插件 ID。
     * <p>Returns the plugin id last resolved for the jar.</p>
     *
     * @param jarName the plugin jar filename
     * @return the plugin id, or null when unknown
     */
    synchronized String pluginId(String jarName) {
        load();
        return ids.get(jarName);
    }

    /**
     * 记录 JAR 与插件 ID 的映射；写失败只记录警告。
     * <p>Records the jar-to-plugin-id mapping; write failures are only logged.</p>
     *
     * @param jarName  the plugin jar filename
     * @param pluginId the plugin id
     */
    synchronized void setId(String jarName, String pluginId) {
        load();
        if (pluginId == null
                || !PluginJarName.isValid(jarName)
                || !PLUGIN_ID.matcher(pluginId).matches()
                || pluginId.equals(ids.get(jarName))) return;
        Map<String, String> next = new HashMap<>(ids);
        next.put(jarName, pluginId);
        ids.put(jarName, pluginId);
        writeIds(next);
    }

    /**
     * 删除 JAR 的插件 ID 映射；写失败只记录警告。
     * <p>Removes the jar-to-plugin-id mapping; write failures are only logged.</p>
     *
     * @param jarName the plugin jar filename
     */
    synchronized void removeId(String jarName) {
        load();
        if (!ids.containsKey(jarName)) return;
        Map<String, String> next = new HashMap<>(ids);
        next.remove(jarName);
        ids.remove(jarName);
        writeIds(next);
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
            writeLines(file, "disabled-", names.stream().sorted().toList());
        } catch (IOException e) {
            throw new PluginException("Cannot write plugin state: " + file, e);
        }
    }

    private void writeIds(Map<String, String> next) {
        Path file = dir.resolve(IDS_FILE);
        try {
            List<String> lines = next.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toList();
            writeLines(file, "ids-", lines);
        } catch (IOException e) {
            log.warn("Cannot write plugin ids: {}", file, e);
        }
    }

    private void writeLines(Path file, String prefix, List<String> lines) throws IOException {
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, prefix, ".tmp");
        try {
            Files.write(temp, lines, StandardCharsets.UTF_8);
            AtomicFiles.moveReplace(temp, file);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
