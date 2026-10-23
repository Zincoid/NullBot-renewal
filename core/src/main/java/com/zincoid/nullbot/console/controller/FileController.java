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
import org.apache.commons.io.FilenameUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]*)}");

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
     * 上传单个文件，可选按正则和模板组装文件名与标签。
     * <p>Uploads a single file, optionally building its filename and tag from a regex and templates.</p>
     *
     * @param type             business type value
     * @param tag              optional tag, used when tagTemplate is absent
     * @param regex            optional pattern matched against the extension-less filename
     * @param filenameTemplate optional filename template with zero-based ${0}, ${1}, ... placeholders
     * @param tagTemplate      optional tag template with zero-based ${0}, ${1}, ... placeholders
     * @param file             uploaded file
     * @return the created file
     */
    @PostMapping
    public ResponseEntity<FileView> upload(
            @RequestParam Integer type,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String regex,
            @RequestParam(required = false) String filenameTemplate,
            @RequestParam(required = false) String tagTemplate,
            @RequestParam("file") MultipartFile file) {
        BizType bizType = bizType(type);
        if (file.isEmpty()) throw new ConsoleException(HttpStatus.BAD_REQUEST, "Empty file");
        String text = ConsoleValidation.text(tag, 255, "tag");
        String pattern = ConsoleValidation.text(regex, 255, "regex");
        File saved;
        if (pattern == null) {
            saved = fileService.save(UPLOADER, file, bizType, text);
        } else {
            String nameTemplate = ConsoleValidation.text(filenameTemplate, 255, "filenameTemplate");
            String tagTemplateText = ConsoleValidation.text(tagTemplate, 255, "tagTemplate");
            ParsedFilename parsed = parseFilename(pattern, file.getOriginalFilename(),
                    nameTemplate, tagTemplateText, text);
            saved = fileService.save(UPLOADER, file, parsed.original(), bizType, parsed.tag());
        }
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

    /**
     * 文件名解析结果。
     * <p>Result of splitting a filename with a pattern.</p>
     *
     * @param original the assembled filename
     * @param tag      the assembled tag, nullable
     */
    private record ParsedFilename(String original, String tag) {
    }

    /**
     * 按正则匹配去掉扩展名的文件名，并用模板组装文件名与标签。
     * <p>Matches the extension-less filename and assembles the filename and tag from templates.</p>
     *
     * @param regex            the filename pattern
     * @param filename         the original filename
     * @param filenameTemplate filename template with zero-based placeholders, null keeps the whole match
     * @param tagTemplate      tag template with zero-based placeholders, null uses the fallback tag
     * @param fallbackTag      tag used when no tag template is given, nullable
     * @return the assembled filename and tag, with the original extension preserved
     */
    private static ParsedFilename parseFilename(String regex, String filename,
                                                String filenameTemplate, String tagTemplate,
                                                String fallbackTag) {
        if (filename == null || filename.isBlank())
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Missing filename");
        Pattern compiled;
        try {
            compiled = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Invalid regex: " + e.getDescription());
        }
        String extension = FilenameUtils.getExtension(filename);
        String base = FilenameUtils.removeExtension(filename);
        if (base.isBlank()) base = filename;
        Matcher matcher = compiled.matcher(base);
        if (!matcher.matches())
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Filename does not match regex: " + filename);
        String name = filenameTemplate == null ? matcher.group() : expand(filenameTemplate, matcher);
        name = ConsoleValidation.required(name, 255, "filename");
        name = withExtension(name, extension);
        name = ConsoleValidation.required(name, 255, "filename");
        String tag = tagTemplate == null
                ? fallbackTag
                : ConsoleValidation.text(expand(tagTemplate, matcher), 255, "tag");
        return new ParsedFilename(name, tag);
    }

    /**
     * 展开模板中的捕获组占位符。
     * <p>Expands capture-group placeholders in a template.</p>
     *
     * @param template the template
     * @param matcher  the matched filename
     * @return the expanded template
     */
    private static String expand(String template, Matcher matcher) {
        Matcher placeholders = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (placeholders.find()) {
            String value = placeholder(placeholders.group(1), matcher);
            placeholders.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        placeholders.appendTail(out);
        return out.toString();
    }

    /**
     * 读取单个占位符的值。
     * <p>Reads the value of one placeholder.</p>
     *
     * @param key     the placeholder key
     * @param matcher the matched filename
     * @return the placeholder value
     */
    private static String placeholder(String key, Matcher matcher) {
        if ("match".equals(key)) return matcher.group();
        int index;
        try {
            index = Integer.parseInt(key);
        } catch (NumberFormatException e) {
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Invalid placeholder: ${" + key + "}");
        }
        if (index < 0 || index >= matcher.groupCount())
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Unknown capture group: " + index);
        String value = matcher.group(index + 1);
        return value == null ? "" : value;
    }

    /**
     * 在文件名缺少扩展名时补回原扩展名。
     * <p>Restores the original extension when the filename has none.</p>
     *
     * @param name      the filename
     * @param extension the original extension, may be empty
     * @return the filename with the extension
     */
    private static String withExtension(String name, String extension) {
        if (extension.isEmpty()) return name;
        String suffix = "." + extension;
        if (name.length() >= suffix.length()
                && name.regionMatches(true, name.length() - suffix.length(), suffix, 0, suffix.length()))
            return name;
        return name + suffix;
    }

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
