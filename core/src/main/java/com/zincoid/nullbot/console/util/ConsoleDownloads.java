package com.zincoid.nullbot.console.util;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * 控制台附件下载响应工具。
 * <p>Console attachment download response utility.</p>
 */
public final class ConsoleDownloads {

    private ConsoleDownloads() {
    }

    /**
     * 构建附件下载响应，保留 UTF-8 文件名与防嗅探头。
     * <p>Builds an attachment response with a UTF-8 filename and nosniff header.</p>
     *
     * @param path      the file path
     * @param filename  the download filename, nullable
     * @param mediaName the filename used to infer the content type, nullable
     * @return the attachment response
     */
    public static ResponseEntity<Resource> attachment(Path path, String filename, String mediaName) {
        String name = clean(filename);
        MediaType mediaType = mediaName == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaTypeFactory.getMediaType(mediaName).orElse(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(name, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }

    /**
     * 构建内联内容响应，供控制台按需预览附件。
     * <p>Builds an inline content response for on-demand console attachment previews.</p>
     *
     * @param path        the file path
     * @param filename    the display filename, nullable
     * @param contentType the MIME type, nullable
     * @return the inline response
     */
    public static ResponseEntity<Resource> inline(Path path, String filename, String contentType) {
        String name = clean(filename);
        MediaType mediaType = mediaType(contentType);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(name, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }

    private static MediaType mediaType(String contentType) {
        if (contentType == null || contentType.isBlank())
            return MediaType.APPLICATION_OCTET_STREAM;
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String clean(String filename) {
        String name = filename == null ? "" : filename.replaceAll("\\p{Cntrl}", "").trim();
        return name.isEmpty() ? "file" : name;
    }
}
