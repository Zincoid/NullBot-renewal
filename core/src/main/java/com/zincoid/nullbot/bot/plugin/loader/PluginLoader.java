package com.zincoid.nullbot.bot.plugin.loader;

import com.zincoid.nullbot.bot.cmd.CmdRegistry;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginConfigProperties;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.bot.plugin.PluginException;
import com.zincoid.nullbot.bot.plugin.PluginResources;
import com.zincoid.nullbot.module.ai.tool.ToolRegistry;
import com.zincoid.nullbot.platform.configuration.PluginProperties;
import com.zincoid.nullbot.platform.util.AtomicFiles;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileAlreadyExistsException;
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
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 外部插件加载器：扫描插件目录，校验并注册插件指令与 AI 工具，支持运行时启停、重载、目录重扫、上传与删除。
 * <p>External plugin loader: scans the plugin directory, validates and registers plugin commands and AI tools,
 * and supports runtime enable, disable, reload, directory rescan, install, and delete.</p>
 */
@Slf4j
@Component
public class PluginLoader {

    /**
     * 当前插件 API 版本，支持 1 至 5；2 增加 AI 工具，3 增加图片渲染与插件模板，
     * 4 增加 PluginContext.properties()，5 增加 PluginContext.http()。
     * <p>Current plugin API version, supporting 1 through 5; version 2 adds AI tools,
     * version 3 adds image rendering and plugin templates, version 4 adds
     * PluginContext.properties(), and version 5 adds PluginContext.http().</p>
     */
    private static final int API_VERSION = 5;
    private static final int MIN_API_VERSION = 1;
    private static final Pattern PLUGIN_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final String HOST_PACKAGE = "com/zincoid/nullbot/";
    private static final String TEMP_PREFIX = ".plugin-";
    private static final String UPLOAD_TEMP_PREFIX = ".upload-";
    private static final String TEMP_SUFFIX = ".tmp";

    private final CmdRegistry cmdRegistry;
    private final ToolRegistry toolRegistry;
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
     * @param cmdRegistry  the command registry
     * @param toolRegistry the AI tool registry
     * @param properties   the plugin properties
     * @param contexts     the plugin context factory
     */
    public PluginLoader(CmdRegistry cmdRegistry,
                        ToolRegistry toolRegistry,
                        PluginProperties properties,
                        PluginContextFactory contexts) {
        this.cmdRegistry = cmdRegistry;
        this.toolRegistry = toolRegistry;
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
        cleanStaleCopies();
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
            String id = state.pluginId(name);
            if (state.isDisabled(name)) {
                result.add(PluginStatus.disabled(name, id));
                continue;
            }
            String error = failures.get(name);
            result.add(error == null
                    ? PluginStatus.available(name, id)
                    : PluginStatus.failed(name, id, error));
        }
        result.sort(Comparator.comparing(PluginStatus::jar));
        return result;
    }

    /**
     * 返回插件数据目录，不创建目录。
     * <p>Returns the plugin data directory without creating it.</p>
     *
     * @param jarName the plugin jar filename
     * @return the data directory, or empty when the plugin id is unknown
     */
    public synchronized Optional<Path> dataDir(String jarName) {
        String name = validateJarName(jarName);
        LoadedPlugin current = findLoaded(name);
        String id = current == null ? state.pluginId(name) : current.id();
        return Optional.ofNullable(id).map(dir::resolve);
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

    /**
     * 安装上传的插件 JAR，写入插件目录并立即加载；加载失败时保留 JAR。
     * <p>Installs an uploaded plugin jar, writes it to the plugin directory, and loads it immediately;
     * the jar is kept when loading fails.</p>
     *
     * @param jarName the plugin jar filename
     * @param source  the uploaded jar content; the caller closes the stream
     * @return the installed plugin status
     */
    public synchronized PluginStatus install(String jarName, InputStream source) {
        requireEnabled();
        String name = validateJarName(jarName);
        Path target = jarPath(name);
        if (findLoaded(name) != null || Files.exists(target))
            throw new PluginException("Plugin already exists: " + name);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new PluginException("Cannot create plugin directory: " + dir, e);
        }
        Path temp = null;
        try {
            temp = createTempFile(dir, UPLOAD_TEMP_PREFIX);
            Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
            if (findLoaded(name) != null || Files.exists(target))
                throw new PluginException("Plugin already exists: " + name);
            AtomicFiles.moveNew(temp, target);
            temp = null;
        } catch (FileAlreadyExistsException e) {
            throw new PluginException("Plugin already exists: " + name);
        } catch (IOException e) {
            throw new PluginException("Cannot install plugin jar: " + name, e);
        } finally {
            deleteQuietly(temp);
        }
        state.setDisabled(name, false);
        failures.remove(name);
        try {
            loadPlugin(name, target);
        } catch (PluginException e) {
            PluginStatus status = PluginStatus.failed(name, state.pluginId(name),
                    failures.getOrDefault(name, message(e)));
            log.info("Plugin installed: {} ({})", name, status.state());
            return status;
        }
        PluginStatus status = findLoaded(name).status();
        log.info("Plugin installed: {} ({})", name, status.state());
        return status;
    }

    /**
     * 删除插件 JAR，已加载时先卸载；插件数据目录保留。
     * <p>Deletes a plugin jar, unloading it first when loaded; the plugin data directory is kept.</p>
     *
     * @param jarName the plugin jar filename
     * @return true when a plugin jar was deleted
     */
    public synchronized boolean delete(String jarName) {
        requireEnabled();
        String name = validateJarName(jarName);
        LoadedPlugin current = findLoaded(name);
        Path jar = jarPath(name);
        if (current == null && !Files.isRegularFile(jar))
            return false;
        try {
            Files.deleteIfExists(jar);
        } catch (IOException e) {
            throw new PluginException("Cannot delete plugin jar: " + name, e);
        }
        if (current != null) unloadInternal(current);
        failures.remove(name);
        state.removeId(name);
        try {
            state.setDisabled(name, false);
        } catch (PluginException e) {
            log.warn("Cannot clear plugin state: {}", name, e);
        }
        log.info("Plugin deleted: {}", name);
        return true;
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
    }

    private void unloadInternal(LoadedPlugin item) {
        item.contributions().unregister(cmdRegistry, toolRegistry);
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
            log.info("Plugin loaded: id={}, version={}, api={}, cmds={}, tools={}",
                    item.id(), item.version(), item.apiVersion(), item.cmds().size(), item.tools().size());
        } catch (PluginException e) {
            failures.put(name, message(e));
            log.error("Plugin load failed: {} - {}", name, message(e), e);
            throw e;
        }
    }

    private LoadedPlugin loadJar(Path jar) {
        OpenedPlugin opened = openJar(jar);
        try {
            Path dataDir = dir.resolve(opened.id());
            Files.createDirectories(dataDir);
            state.setId(opened.jar().name(), opened.id());
            extractDefaultConfig(opened, dataDir);
            opened.start(contexts.create(dataDir, opened.loader(), opened.jar().copy()));
            PluginContributions contributions = PluginContributions.collect(opened.plugin(), opened.apiVersion());
            LoadedPlugin item = new LoadedPlugin(opened.id(), opened.version(), opened.apiVersion(),
                    opened.jar(), opened.loader(), opened.plugin(), contributions);
            contributions.register(cmdRegistry, toolRegistry);
            return item;
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            opened.close();
            if (e instanceof PluginException pluginException) throw pluginException;
            throw new PluginException("Plugin load failed: " + message(e), e);
        }
    }

    private void extractDefaultConfig(OpenedPlugin opened, Path dataDir) {
        if (opened.plugin().getClass().getResource(PluginConfigProperties.RESOURCE) == null) return;
        try {
            PluginResources.extract(opened.plugin().getClass(),
                    PluginConfigProperties.RESOURCE, dataDir);
        } catch (PluginException e) {
            log.warn("Plugin default config unavailable: {}", opened.id(), e);
        }
    }

    private OpenedPlugin openJar(Path jar) {
        URLClassLoader loader = null;
        Path copy = null;
        try {
            FileTime modified = Files.getLastModifiedTime(jar);
            long size = Files.size(jar);
            copy = createTempFile(dir, TEMP_PREFIX);
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
            Plugin plugin = providers.getFirst();
            String id = plugin.id();
            if (id == null || !PLUGIN_ID.matcher(id).matches())
                throw new PluginException("Invalid plugin id: " + id);
            if (loaded.stream().anyMatch(item -> item.id().equals(id)))
                throw new PluginException("Duplicate plugin id: " + id);
            int apiVersion = plugin.apiVersion();
            if (apiVersion < MIN_API_VERSION || apiVersion > API_VERSION)
                throw new PluginException("Unsupported apiVersion: " + apiVersion
                        + ", expected " + MIN_API_VERSION + "-" + API_VERSION);
            PluginJar jarInfo = new PluginJar(jar.getFileName().toString(), copy, modified, size);
            return new OpenedPlugin(id, plugin.version(), apiVersion, jarInfo, loader, plugin);
        } catch (Exception | LinkageError | ServiceConfigurationError e) {
            closeQuietly(loader);
            deleteQuietly(copy);
            if (e instanceof PluginException pluginException) throw pluginException;
            throw new PluginException("Plugin load failed: " + message(e), e);
        }
    }

    /**
     * 已打开但未注册的插件：JAR、类加载器与插件实例，加载失败时统一关闭。
     * <p>Opened but unregistered plugin: jar, classloader, and plugin instance, closed uniformly on load failure.</p>
     */
    @RequiredArgsConstructor
    private static final class OpenedPlugin implements AutoCloseable {

        private final String id;
        private final String version;
        private final int apiVersion;
        private final PluginJar jar;
        private final URLClassLoader loader;
        private final Plugin plugin;
        private boolean started;

        String id() {
            return id;
        }

        String version() {
            return version;
        }

        int apiVersion() {
            return apiVersion;
        }

        PluginJar jar() {
            return jar;
        }

        URLClassLoader loader() {
            return loader;
        }

        Plugin plugin() {
            return plugin;
        }

        void start(PluginContext context) {
            plugin.start(context);
            started = true;
        }

        @Override
        public void close() {
            if (started) {
                try {
                    plugin.stop();
                } catch (Exception e) {
                    log.warn("Plugin stop failed after load error: {}", id, e);
                }
            }
            closeQuietly(loader);
            deleteQuietly(jar.copy());
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
        log.info("Plugin scan complete: scanned={}, unloaded={}, loaded={}, failed={}",
                byName.size(), unloaded, loaded.size() - before, failed);
    }

    // ════ Helpers ═════════════════

    /**
     * 清理上次异常退出遗留的插件 JAR 副本，仅在启动加载前调用。
     * <p>Removes plugin jar copies left by abnormal exits; called only before startup loading.</p>
     */
    private void cleanStaleCopies() {
        if (!loaded.isEmpty() || !Files.isDirectory(dir)) return;
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> stale = files.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return (name.startsWith(TEMP_PREFIX)
                                || name.startsWith(UPLOAD_TEMP_PREFIX))
                                && name.endsWith(TEMP_SUFFIX);
                    })
                    .toList();
            for (Path path : stale) deleteQuietly(path);
            if (!stale.isEmpty()) log.info("Stale plugin temp files removed: {}", stale.size());
        } catch (IOException e) {
            log.warn("Cannot clean plugin temp files: {}", dir, e);
        }
    }

    private static Path createTempFile(Path dir, String prefix) throws IOException {
        for (int attempt = 0; attempt < 10; attempt++) {
            String name = prefix + UUID.randomUUID().toString().substring(0, 8) + TEMP_SUFFIX;
            try {
                return Files.createFile(dir.resolve(name));
            } catch (FileAlreadyExistsException ignored) {
                // Try another short random name.
            }
        }
        throw new IOException("Cannot create plugin temp file in " + dir);
    }

    private List<Path> scanJars() {
        if (!Files.isDirectory(dir)) return List.of();
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
        if (!PluginJarName.isValid(jarName))
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
