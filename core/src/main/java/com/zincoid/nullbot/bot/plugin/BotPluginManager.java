package com.zincoid.nullbot.bot.plugin;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdRegistry;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.LanguageService;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.module.setting.UserSettingService;
import com.zincoid.nullbot.platform.configuration.PluginProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 外部插件加载器：扫描插件目录，校验并注册插件指令，卸载时停止插件。
 * <p>External plugin loader: scans the plugin directory, validates and registers plugin commands, and stops plugins on unload.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotPluginManager {

    /**
     * 当前插件 API 版本，插件返回不同值会被拒绝。
     * <p>Current plugin API version; plugins returning another value are rejected.</p>
     */
    private static final int API_VERSION = 1;

    private static final Pattern PLUGIN_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private static final String HOST_PACKAGE = "com/zincoid/nullbot/";

    private final CmdRegistry registry;
    private final PluginProperties properties;
    private final FileService fileService;
    private final UserService userService;
    private final GroupService groupService;
    private final GroupSettingService groupSettingService;
    private final UserSettingService userSettingService;
    private final QuoteService quoteService;
    private final LanguageService languageService;

    private final List<LoadedPlugin> loaded = new CopyOnWriteArrayList<>();

    // ════ Load ════════════════════

    /**
     * 在应用就绪后加载插件目录中的全部 JAR。
     * <p>Loads all JARs in the plugin directory after the application is ready.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        if (!properties.isEnabled()) {
            log.info("Plugin loading disabled.");
            return;
        }
        Path dir = Path.of(properties.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.error("Cannot create plugin directory: {}", dir, e);
            return;
        }
        List<Path> jars;
        try (Stream<Path> files = Files.list(dir)) {
            jars = files
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.error("Cannot list plugin directory: {}", dir, e);
            return;
        }
        for (Path jar : jars) loadJar(jar, dir);
        log.info("Plugins loaded: {}", loaded.size());
    }

    private void loadJar(Path jar, Path dir) {
        URLClassLoader loader = null;
        BotPlugin plugin = null;
        boolean started = false;
        try {
            checkApiBundled(jar);
            loader = new URLClassLoader(
                    new URL[]{jar.toUri().toURL()},
                    BotPlugin.class.getClassLoader());
            List<BotPlugin> providers = new ArrayList<>();
            ServiceLoader.load(BotPlugin.class, loader).forEach(providers::add);
            if (providers.isEmpty()) {
                log.warn("Plugin provider missing: {}", jar.getFileName());
                closeQuietly(loader);
                return;
            }
            if (providers.size() > 1) {
                log.error("Multiple plugin providers in one JAR: {}", jar.getFileName());
                closeQuietly(loader);
                return;
            }
            plugin = providers.getFirst();
            String id = plugin.id();
            if (id == null || !PLUGIN_ID.matcher(id).matches())
                throw new PluginException("Invalid plugin id: " + id);
            if (loaded.stream().anyMatch(item -> item.id().equals(id)))
                throw new PluginException("Duplicate plugin id: " + id);
            if (plugin.apiVersion() != API_VERSION)
                throw new PluginException("Unsupported apiVersion: " + plugin.apiVersion()
                        + ", expected " + API_VERSION);
            List<Cmd> cmds = plugin.cmds() == null ? List.of() : plugin.cmds();
            Path dataDir = dir.resolve(id);
            Files.createDirectories(dataDir);
            plugin.start(new DefaultPluginContext(
                    fileService, userService, groupService, groupSettingService,
                    userSettingService, quoteService, languageService, dataDir));
            started = true;
            if (!registry.register(cmds))
                throw new PluginException("Command name conflict");
            loaded.add(new LoadedPlugin(id, plugin.version(), jar, loader, plugin));
            log.info("Plugin loaded: id={}, version={}, cmds={}", id, plugin.version(), cmds.size());
        } catch (Exception | LinkageError e) {
            if (started) {
                try {
                    plugin.stop();
                } catch (Exception stopError) {
                    log.warn("Plugin stop failed after load error: {}", plugin.id(), stopError);
                }
            }
            log.error("Plugin load failed: {} - {}", jar.getFileName(), e.getMessage(), e);
            closeQuietly(loader);
        }
    }

    // ════ Unload ══════════════════

    /**
     * 关闭时逆序停止插件并关闭各自的类加载器。
     * <p>Stops plugins in reverse order and closes their classloaders on shutdown.</p>
     */
    @PreDestroy
    public void unload() {
        for (int i = loaded.size() - 1; i >= 0; i--) {
            LoadedPlugin item = loaded.get(i);
            try {
                item.plugin().stop();
            } catch (Exception e) {
                log.warn("Plugin stop failed: {}", item.id(), e);
            }
            closeQuietly(item.loader());
            log.info("Plugin unloaded: {}", item.id());
        }
        loaded.clear();
    }

    // ════ Helpers ═════════════════

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

    private static void closeQuietly(URLClassLoader loader) {
        if (loader == null) return;
        try {
            loader.close();
        } catch (IOException e) {
            log.warn("Plugin classloader close failed", e);
        }
    }

    private record LoadedPlugin(
            String id,
            String version,
            Path jar,
            URLClassLoader loader,
            BotPlugin plugin
    ) {
    }
}
