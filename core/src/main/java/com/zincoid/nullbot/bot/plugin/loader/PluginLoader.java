package com.zincoid.nullbot.bot.plugin.loader;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdRegistry;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginException;
import com.zincoid.nullbot.platform.configuration.PluginProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 外部插件加载器：扫描插件目录，校验并注册插件指令，支持运行时启停、重载与目录重扫。
 * <p>External plugin loader: scans the plugin directory, validates and registers plugin commands,
 * and supports runtime enable, disable, reload, and directory rescan.</p>
 */
@Slf4j
@Component
public class PluginLoader {

    /**
     * 当前插件 API 版本，插件返回不同值会被拒绝。
     * <p>Current plugin API version; plugins returning another value are rejected.</p>
     */
    private static final int API_VERSION = 1;
    private static final Pattern PLUGIN_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Pattern PLUGIN_JAR = Pattern.compile("[A-Za-z0-9._-]{1,128}\\.jar");
    private static final String HOST_PACKAGE = "com/zincoid/nullbot/";

    private final CmdRegistry registry;
    private final PluginProperties properties;
    private final PluginContextFactory contexts;

    private final Path dir;
    private final PluginStateStore state;
    private final List<LoadedPlugin> loaded = new ArrayList<>();
    private final Map<String, String> failures = new HashMap<>();

    /**
     * 创建插件加载器并固定插件目录。
     * <p>Creates the plugin loader and resolves the plugin directory.</p>
     *
     * @param registry   the command registry
     * @param properties the plugin properties
     * @param contexts   the plugin context factory
     */
    public PluginLoader(CmdRegistry registry,
                        PluginProperties properties,
                        PluginContextFactory contexts) {
        this.registry = registry;
        this.properties = properties;
        this.contexts = contexts;
        this.dir = Path.of(properties.getDir()).toAbsolutePath().normalize();
        this.state = new PluginStateStore(dir);
    }

    // ════ Load ════════════════════

    /**
     * 在应用就绪后加载插件目录中的全部 JAR，跳过已禁用的 JAR。
     * <p>Loads all jars in the plugin directory after the application is ready, skipping disabled jars.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void load() {
        state.load();
        if (!properties.isEnabled()) {
            log.info("Plugin loading disabled.");
            return;
        }
        try {
            rescan();
        } catch (PluginException e) {
            log.error("Cannot list plugin directory: {}", dir, e);
            return;
        }
        log.info("Plugins loaded: {}", loaded.size());
    }

    // ════ List ════════════════════

    /**
     * 列出已加载插件，按插件 ID 排序。
     * <p>Lists the loaded plugins, sorted by plugin id.</p>
     *
     * @return the loaded plugin statuses
     */
    public synchronized List<PluginStatus> loaded() {
        return loaded.stream()
                .map(LoadedPlugin::status)
                .sorted(Comparator.comparing(PluginStatus::id))
                .toList();
    }

    /**
     * 列出插件目录中的 JAR 及其加载状态。
     * <p>Lists the jars in the plugin directory and their load states.</p>
     *
     * @return the plugin statuses sorted by jar name
     */
    public synchronized List<PluginStatus> list() {
        Map<String, LoadedPlugin> loadedByJar = new HashMap<>();
        for (LoadedPlugin item : loaded) loadedByJar.put(item.jarName(), item);
        List<PluginStatus> result = new ArrayList<>();
        for (LoadedPlugin item : loadedByJar.values()) result.add(item.status());
        Set<String> seen = new HashSet<>(loadedByJar.keySet());
        for (Path jar : scanJars()) {
            String name = jar.getFileName().toString();
            if (!seen.add(name)) continue;
            if (state.isDisabled(name)) {
                result.add(new PluginStatus(name, null, null, null, List.of(),
                        PluginStatus.State.DISABLED, null));
                continue;
            }
            String error = failures.get(name);
            result.add(new PluginStatus(name, null, null, null, List.of(),
                    error == null ? PluginStatus.State.AVAILABLE : PluginStatus.State.FAILED,
                    error));
        }
        result.sort(Comparator.comparing(PluginStatus::jar));
        return result;
    }

    /**
     * 是否允许加载插件，由 {@code bot.plugin.enabled} 控制。
     * <p>Whether plugin loading is allowed, controlled by {@code bot.plugin.enabled}.</p>
     *
     * @return true when plugin loading is enabled
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    // ════ Manage ══════════════════

    /**
     * 启用并加载指定 JAR，已加载时仅清除禁用状态。
     * <p>Enables and loads the given jar, clearing only the disabled state when already loaded.</p>
     *
     * @param jarName the plugin jar filename
     */
    public synchronized void enable(String jarName) {
        requireEnabled();
        String name = validateJarName(jarName);
        LoadedPlugin current = findLoaded(name);
        if (current != null) {
            state.setDisabled(name, false);
            return;
        }
        Path jar = jarPath(name);
        if (!Files.isRegularFile(jar))
            throw new PluginException("Plugin not found: " + name);
        state.setDisabled(name, false);
        loadPlugin(name, jar);
    }

    /**
     * 卸载并禁用指定 JAR，禁用状态写入插件目录的 {@code disabled.txt}。
     * <p>Unloads and disables the given jar, persisting the state to {@code disabled.txt} in the plugin directory.</p>
     *
     * @param jarName the plugin jar filename
     */
    public synchronized void disable(String jarName) {
        requireEnabled();
        String name = validateJarName(jarName);
        LoadedPlugin current = findLoaded(name);
        if (current == null && !Files.isRegularFile(jarPath(name)))
            throw new PluginException("Plugin not found: " + name);
        state.setDisabled(name, true);
        failures.remove(name);
        if (current != null) unloadInternal(current);
        log.info("Plugin disabled: {}", name);
    }

    /**
     * 重新加载已加载的插件 JAR。
     * <p>Reloads a loaded plugin jar.</p>
     *
     * @param jarName the plugin jar filename
     */
    public synchronized void reload(String jarName) {
        requireEnabled();
        String name = validateJarName(jarName);
        LoadedPlugin current = findLoaded(name);
        if (current == null)
            throw new PluginException("Plugin is not loaded: " + name);
        Path jar = jarPath(name);
        if (!Files.isRegularFile(jar))
            throw new PluginException("Plugin not found: " + name);
        unloadInternal(current);
        loadPlugin(name, jar);
    }

    /**
     * 重新扫描插件目录：加载新增、重载变更、卸载已删除的插件。
     * <p>Rescans the plugin directory: loads new jars, reloads changed jars,
     * and unloads deleted jars.</p>
     */
    public synchronized void reloadAll() {
        requireEnabled();
        state.load();
        rescan();
    }

    // ════ Unload ══════════════════

    /**
     * 关闭时逆序停止插件并关闭各自的类加载器。
     * <p>Stops plugins in reverse order and closes their classloaders on shutdown.</p>
     */
    @PreDestroy
    public synchronized void unload() {
        List<LoadedPlugin> snapshot = new ArrayList<>(loaded);
        for (int i = snapshot.size() - 1; i >= 0; i--) unloadInternal(snapshot.get(i));
        loaded.clear();
    }

    private void unloadInternal(LoadedPlugin item) {
        registry.unregister(item.cmds());
        try {
            item.plugin().stop();
        } catch (Exception e) {
            log.warn("Plugin stop failed: {}", item.id(), e);
        }
        closeQuietly(item.loader());
        deleteQuietly(item.jarCopy());
        loaded.remove(item);
        log.info("Plugin unloaded: {}", item.id());
    }

    // ════ Load helpers ════════════

    private void loadPlugin(String name, Path jar) {
        try {
            LoadedPlugin item = loadJar(jar);
            loaded.add(item);
            failures.remove(name);
            log.info("Plugin loaded: id={}, version={}, cmds={}",
                    item.id(), item.version(), item.cmds().size());
        } catch (PluginException e) {
            failures.put(name, message(e));
            log.error("Plugin load failed: {} - {}", name, message(e), e);
            throw e;
        }
    }

    private LoadedPlugin loadJar(Path jar) {
        URLClassLoader loader = null;
        Path copy = null;
        Plugin plugin = null;
        boolean started = false;
        try {
            FileTime modified = Files.getLastModifiedTime(jar);
            long size = Files.size(jar);
            copy = Files.createTempFile(dir, ".nullbot-plugin-", ".jar.tmp");
            Files.copy(jar, copy, StandardCopyOption.REPLACE_EXISTING);
            checkApiBundled(copy);
            loader = new URLClassLoader(
                    new URL[]{copy.toUri().toURL()},
                    Plugin.class.getClassLoader());
            List<Plugin> providers = new ArrayList<>();
            ServiceLoader.load(Plugin.class, loader).forEach(providers::add);
            if (providers.isEmpty())
                throw new PluginException("Plugin provider missing");
            if (providers.size() > 1)
                throw new PluginException("Multiple plugin providers in one jar");
            plugin = providers.getFirst();
            String id = plugin.id();
            if (id == null || !PLUGIN_ID.matcher(id).matches())
                throw new PluginException("Invalid plugin id: " + id);
            if (loaded.stream().anyMatch(item -> item.id().equals(id)))
                throw new PluginException("Duplicate plugin id: " + id);
            if (plugin.apiVersion() != API_VERSION)
                throw new PluginException("Unsupported apiVersion: " + plugin.apiVersion()
                        + ", expected " + API_VERSION);
            List<Cmd> provided = plugin.cmds();
            List<Cmd> cmds = provided == null ? List.of() : List.copyOf(provided);
            Path dataDir = dir.resolve(id);
            Files.createDirectories(dataDir);
            plugin.start(contexts.create(dataDir));
            started = true;
            if (!registry.register(cmds))
                throw new PluginException("Command name conflict");
            return new LoadedPlugin(id, plugin.version(), plugin.apiVersion(),
                    jar.getFileName().toString(), copy, modified, size, loader, plugin, cmds);
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            if (started) {
                try {
                    plugin.stop();
                } catch (Exception stopError) {
                    log.warn("Plugin stop failed after load error: {}", plugin.id(), stopError);
                }
            }
            closeQuietly(loader);
            deleteQuietly(copy);
            if (e instanceof PluginException pluginException) throw pluginException;
            throw new PluginException("Plugin load failed: " + message(e), e);
        }
    }

    private static void checkApiBundled(Path jar) throws IOException {
        try (JarFile file = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(HOST_PACKAGE) && !name.endsWith("/"))
                    throw new PluginException("Plugin bundles host API: " + name);
            }
        }
    }

    // ════ Rescan ══════════════════

    /**
     * 重新扫描目录并同步插件状态，调用方需持有当前加载器的监视器。
     * <p>Rescans the directory and synchronizes plugin state; callers must hold the loader monitor.</p>
     */
    private void rescan() {
        List<Path> jars = scanJars();
        Map<String, Path> byName = new LinkedHashMap<>();
        for (Path jar : jars) byName.put(jar.getFileName().toString(), jar);

        int unloaded = 0;
        List<LoadedPlugin> snapshot = new ArrayList<>(loaded);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            LoadedPlugin item = snapshot.get(i);
            Path source = byName.get(item.jarName());
            if (source == null
                    || state.isDisabled(item.jarName())
                    || isModified(item, source)) {
                unloadInternal(item);
                unloaded++;
            }
        }

        failures.keySet().removeIf(name ->
                !byName.containsKey(name) || state.isDisabled(name));

        int before = loaded.size();
        int failed = 0;
        for (Map.Entry<String, Path> entry : byName.entrySet()) {
            String name = entry.getKey();
            if (state.isDisabled(name)) {
                log.info("Plugin disabled: {}", name);
                continue;
            }
            if (findLoaded(name) != null) continue;
            Path source = entry.getValue();
            if (!Files.isRegularFile(source)) {
                failures.remove(name);
                continue;
            }
            try {
                loadPlugin(name, source);
            } catch (PluginException ignored) {
                failed++;
            }
        }
        log.info("Plugin rescan complete: scanned={}, unloaded={}, loaded={}, failed={}",
                byName.size(), unloaded, loaded.size() - before, failed);
    }

    // ════ Helpers ═════════════════

    private List<Path> scanJars() {
        state.load();
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new PluginException("Cannot list plugin directory: " + dir, e);
        }
    }

    private boolean isModified(LoadedPlugin item, Path jar) {
        try {
            return !item.jarModified().equals(Files.getLastModifiedTime(jar))
                    || item.jarSize() != Files.size(jar);
        } catch (IOException e) {
            log.warn("Cannot stat plugin jar: {}", jar, e);
            return true;
        }
    }

    private Path jarPath(String jarName) {
        return dir.resolve(validateJarName(jarName)).normalize();
    }

    private static String validateJarName(String jarName) {
        if (jarName == null || !PLUGIN_JAR.matcher(jarName).matches())
            throw new PluginException("Invalid plugin jar: " + jarName);
        return jarName;
    }

    private LoadedPlugin findLoaded(String jarName) {
        return loaded.stream()
                .filter(item -> item.jarName().equals(jarName))
                .findFirst()
                .orElse(null);
    }

    private void requireEnabled() {
        if (!properties.isEnabled())
            throw new PluginException("Plugin loading is disabled");
    }

    private static String message(Throwable e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private static void closeQuietly(URLClassLoader loader) {
        if (loader == null) return;
        try {
            loader.close();
        } catch (IOException e) {
            log.warn("Plugin classloader close failed", e);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Plugin temp file delete failed: {}", path, e);
            path.toFile().deleteOnExit();
        }
    }
}
