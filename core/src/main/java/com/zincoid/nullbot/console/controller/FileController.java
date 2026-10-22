package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.FileUpdate;
import com.zincoid.nullbot.console.dto.FileView;
import com.zincoid.nullbot.console.dto.PageView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 控制台文件接口：分页、上传、修改、删除与内容读取。
 * <p>Console file API: paging, upload, update, delete, and content reads.</p>
 */
@RestController
@RequestMapping("/console/files")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class FileController {

    private static final String UPLOADER = "console";

    private final FileService fileService;

    // ════ Query ══════════════════

    /**
     * 分页查询文件。
     * <p>Pages stored files.</p>
     *
     * @param type     business type value, nullable for all types
     * @param tag      keyword matched against the tag, nullable
     * @param original keyword matched against the original filename, nullable
     * @param page     one-based page number
     * @param size     page size
     * @return the file page
     */
    @GetMapping
    public PageView<FileView> list(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String original,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        BizType bizType = type == null ? null : bizType(type);
        return PageView.of(fileService.page(bizType, tag, original,
                ConsoleValidation.page(page), ConsoleValidation.size(size)), FileView::of);
    }

    // ════ Upload ═════════════════

    /**
     * 上传单个文件。
     * <p>Uploads a single file.</p>
     *
     * @param type business type value
     * @param tag  optional tag
     * @param file uploaded file
     * @return the created file
     */
    @PostMapping
    public ResponseEntity<FileView> upload(
            @RequestParam Integer type,
            @RequestParam(required = false) String tag,
            @RequestParam("file") MultipartFile file) {
        BizType bizType = bizType(type);
        if (file.isEmpty()) throw new ConsoleException(HttpStatus.BAD_REQUEST, "Empty file");
        String text = ConsoleValidation.text(tag, 255, "tag");
        File saved = fileService.save(UPLOADER, file, bizType, text);
        return ResponseEntity.status(HttpStatus.CREATED).body(FileView.of(saved));
    }

    // ════ Update ═════════════════

    /**
     * 修改文件的原始文件名与描述。
     * <p>Updates the original filename and tag of a file.</p>
     *
     * @param id   record id
     * @param body the new file information
     * @return the updated file
     */
    @PutMapping("/{id}")
    public FileView update(@PathVariable Long id, @Valid @RequestBody FileUpdate body) {
        File file = file(id);
        String original = body.original().trim();
        String tag = ConsoleValidation.text(body.tag(), 255, "tag");
        fileService.update(id, original, tag);
        file.setOriginal(original);
        file.setTag(tag);
        return FileView.of(file);
    }

    // ════ Content ════════════════

    /**
     * 读取已存储文件的内容。
     * <p>Reads the stored content of a file.</p>
     *
     * @param id record id
     * @return the file bytes as an attachment
     */
    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@PathVariable Long id) {
        File file = file(id);
        MediaType mediaType = MediaTypeFactory.getMediaType(file.getStored())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        String name = file.getOriginal() == null
                ? ""
                : file.getOriginal().replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) name = "file";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(name, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(fileService.read(id));
    }

    // ════ Delete ═════════════════

    /**
     * 删除文件记录与磁盘文件。
     * <p>Deletes a file record and its stored file.</p>
     *
     * @param id record id
     * @return an empty response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!fileService.delete(id))
            throw new ConsoleException(HttpStatus.NOT_FOUND, "File not found: " + id);
        return ResponseEntity.noContent().build();
    }

    // ════ Helpers ═════════════════

    private File file(Long id) {
        File file = fileService.getById(id);
        if (file == null) throw new ConsoleException(HttpStatus.NOT_FOUND, "File not found: " + id);
        return file;
    }

    private static BizType bizType(Integer value) {
        BizType type = BizType.fromValue(value);
        if (type == null)
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Unknown biz type: " + value);
        return type;
    }
}
