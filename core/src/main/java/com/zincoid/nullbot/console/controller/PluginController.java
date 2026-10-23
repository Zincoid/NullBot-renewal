package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.bot.plugin.loader.PluginLoader;
import com.zincoid.nullbot.console.dto.PluginListView;
import com.zincoid.nullbot.console.dto.PluginView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 控制台插件接口：查询、启用、禁用与重载外部插件。
 * <p>Console plugin API: queries, enabling, disabling, and reloading external plugins.</p>
 */
@RestController
@RequestMapping("/console/plugins")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class PluginController {

    private static final Pattern JAR_NAME = Pattern.compile("[A-Za-z0-9._-]{1,128}\\.jar");

    private final PluginLoader pluginLoader;

    // ════ Query ══════════════════

    /**
     * 列出插件目录中的 JAR 与加载状态。
     * <p>Lists the jars in the plugin directory and their load states.</p>
     *
     * @return the global switch and plugin statuses
     */
    @GetMapping
    public PluginListView list() {
        List<PluginView> items = pluginLoader.list().stream()
                .map(PluginView::of)
                .toList();
        return new PluginListView(pluginLoader.isEnabled(), items);
    }

    // ════ Manage ═════════════════

    /**
     * 启用并加载插件。
     * <p>Enables and loads a plugin.</p>
     *
     * @param jar the plugin jar filename
     * @return an empty response
     */
    @PostMapping("/{jar}/enable")
    public ResponseEntity<Void> enable(@PathVariable String jar) {
        pluginLoader.enable(name(jar));
        return ResponseEntity.noContent().build();
    }

    /**
     * 卸载并禁用插件。
     * <p>Unloads and disables a plugin.</p>
     *
     * @param jar the plugin jar filename
     * @return an empty response
     */
    @PostMapping("/{jar}/disable")
    public ResponseEntity<Void> disable(@PathVariable String jar) {
        pluginLoader.disable(name(jar));
        return ResponseEntity.noContent().build();
    }

    /**
     * 重载已加载的插件。
     * <p>Reloads a loaded plugin.</p>
     *
     * @param jar the plugin jar filename
     * @return an empty response
     */
    @PostMapping("/{jar}/reload")
    public ResponseEntity<Void> reload(@PathVariable String jar) {
        pluginLoader.reload(name(jar));
        return ResponseEntity.noContent().build();
    }

    // ════ Helpers ═════════════════

    private static String name(String jar) {
        if (jar == null || !JAR_NAME.matcher(jar).matches())
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Invalid plugin jar: " + jar);
        return jar;
    }
}
