package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.bot.plugin.PluginException;
import com.zincoid.nullbot.bot.plugin.loader.PluginLoader;
import com.zincoid.nullbot.bot.plugin.loader.PluginStatus;
import com.zincoid.nullbot.console.dto.PluginListView;
import com.zincoid.nullbot.console.dto.PluginView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 控制台插件接口：查询、启用、禁用、重载、重扫、上传与删除外部插件。
 * <p>Console plugin API: queries, enabling, disabling, reloading, rescanning,
 * uploading, and deleting external plugins.</p>
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
     * 上传插件 JAR 并立即加载；同名 JAR 已存在时返回冲突。
     * <p>Uploads and loads a plugin jar immediately; returns a conflict when the jar name exists.</p>
     *
     * @param file the uploaded jar
     * @return the installed plugin status
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PluginView> install(
            @RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Empty file");
        String original = FilenameUtils.getName(file.getOriginalFilename());
        String jarName = name(original == null ? null : original.trim());
        try (InputStream in = file.getInputStream()) {
            PluginStatus status = pluginLoader.install(jarName, in);
            return ResponseEntity.status(HttpStatus.CREATED).body(PluginView.of(status));
        } catch (PluginException e) {
            throw pluginError(e);
        } catch (IOException e) {
            throw new ConsoleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot read uploaded file: " + e.getMessage());
        }
    }

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

    /**
     * 重新扫描插件目录：加载新增、重载变更、卸载已删除的插件。
     * <p>Rescans the plugin directory: loads new jars, reloads changed jars,
     * and unloads deleted jars.</p>
     *
     * @return an empty response
     */
    @PostMapping("/reload")
    public ResponseEntity<Void> reloadAll() {
        pluginLoader.reloadAll();
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除插件 JAR；已加载时先卸载，插件数据目录保留。
     * <p>Deletes a plugin jar, unloading it first when loaded; the plugin data directory is kept.</p>
     *
     * @param jar the plugin jar filename
     * @return an empty response
     */
    @DeleteMapping("/{jar}")
    public ResponseEntity<Void> delete(@PathVariable String jar) {
        String jarName = name(jar);
        try {
            if (!pluginLoader.delete(jarName))
                throw new ConsoleException(HttpStatus.NOT_FOUND, "Plugin not found: " + jarName);
        } catch (PluginException e) {
            throw pluginError(e);
        }
        return ResponseEntity.noContent().build();
    }

    // ════ Helpers ═════════════════

    private static String name(String jar) {
        if (jar == null || !JAR_NAME.matcher(jar).matches())
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Invalid plugin jar: " + jar);
        return jar;
    }

    private static ConsoleException pluginError(PluginException e) {
        HttpStatus status = e.getCause() instanceof IOException
                ? HttpStatus.INTERNAL_SERVER_ERROR
                : HttpStatus.CONFLICT;
        return new ConsoleException(status, e.getMessage());
    }
}
