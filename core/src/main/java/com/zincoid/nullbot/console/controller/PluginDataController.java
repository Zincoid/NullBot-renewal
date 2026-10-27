package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.PluginDataDelete;
import com.zincoid.nullbot.console.dto.PluginDataFolder;
import com.zincoid.nullbot.console.dto.PluginDataResult;
import com.zincoid.nullbot.console.dto.PluginDataView;
import com.zincoid.nullbot.console.service.PluginDataService;
import com.zincoid.nullbot.console.util.ConsoleDownloads;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * 控制台插件数据接口：浏览、新建文件夹、上传、删除与下载插件数据文件。
 * <p>Console plugin data API: browses, creates folders, uploads, deletes,
 * and downloads plugin data files.</p>
 */
@RestController
@RequestMapping("/console/plugins/{jar}/data")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class PluginDataController {

    private final PluginDataService dataService;

    // ════ Query ══════════════════

    /**
     * 列出插件数据目录。
     * <p>Lists a plugin data directory.</p>
     *
     * @param jar  the plugin jar filename
     * @param path the directory path relative to the data root
     * @return the directory view
     */
    @GetMapping
    public PluginDataView list(@PathVariable String jar,
                               @RequestParam(required = false) String path) {
        return dataService.list(ConsoleValidation.pluginJar(jar), path);
    }

    /**
     * 新建插件数据文件夹。
     * <p>Creates a plugin data folder.</p>
     *
     * @param jar     the plugin jar filename
     * @param request the folder request
     * @return the created entry
     */
    @PostMapping("/folders")
    public ResponseEntity<PluginDataView.Entry> createFolder(
            @PathVariable String jar, @RequestBody PluginDataFolder request) {
        PluginDataView.Entry entry = dataService.createFolder(
                ConsoleValidation.pluginJar(jar), request.path(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    /**
     * 批量上传插件数据文件，同名文件覆盖。
     * <p>Uploads plugin data files in batch, overwriting files with the same name.</p>
     *
     * @param jar   the plugin jar filename
     * @param path  the target directory path relative to the data root
     * @param files the uploaded files
     * @return the per-file results
     */
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PluginDataResult upload(
            @PathVariable String jar,
            @RequestParam(required = false) String path,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        return dataService.upload(ConsoleValidation.pluginJar(jar), path, files);
    }

    // ════ Delete ═════════════════

    /**
     * 批量删除插件数据条目，文件夹递归删除。
     * <p>Deletes plugin data entries in batch, removing folders recursively.</p>
     *
     * @param jar     the plugin jar filename
     * @param request the delete request
     * @return the per-item results
     */
    @PostMapping("/delete")
    public PluginDataResult delete(
            @PathVariable String jar, @RequestBody PluginDataDelete request) {
        return dataService.delete(ConsoleValidation.pluginJar(jar), request.path(), request.names());
    }

    // ════ Download ═══════════════

    /**
     * 下载插件数据文件。
     * <p>Downloads a plugin data file.</p>
     *
     * @param jar  the plugin jar filename
     * @param path the file path relative to the data root
     * @return the file as an attachment
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @PathVariable String jar, @RequestParam(required = false) String path) {
        Path target = dataService.file(ConsoleValidation.pluginJar(jar), path);
        String filename = target.getFileName() == null ? null : target.getFileName().toString();
        return ConsoleDownloads.attachment(target, filename, filename);
    }
}
