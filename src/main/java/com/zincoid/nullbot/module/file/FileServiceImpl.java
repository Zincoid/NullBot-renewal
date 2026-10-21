package com.zincoid.nullbot.module.file;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.configuration.FileProperties;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.mapper.FileMapper;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * {@link FileService} 的默认实现。
 * <p>Default implementation of {@link FileService}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    private final FileProperties fileProperties;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // ════ Save ════════════════════

    @Override
    public File save(String userId, String url, BizType type, String description) {
        String original = originalFromUrl(url);
        HttpResponse<InputStream> resp = download(url);
        try (InputStream in = resp.body()) {
            return store(userId, original, in, type, description);
        } catch (IOException e) {
            throw new FileException("Failed to read download body: " + url, e);
        }
    }

    @Override
    public File save(String userId, MultipartFile file, BizType type, String description) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) original = "unnamed";
        try (InputStream in = file.getInputStream()) {
            return store(userId, original, in, type, description);
        } catch (IOException e) {
            throw new FileException("Failed to read upload: " + original, e);
        }
    }

    // ════ Update ══════════════════

    @Override
    public void update(Long id, String original, String description) {
        if (original == null || original.isBlank())
            throw new FileException("Missing file name: " + id);
        boolean updated = lambdaUpdate()
                .eq(File::getId, id)
                .set(File::getOriginal, original)
                .set(File::getDescription, description)
                .update();
        if (!updated) throw new FileException("File not found: " + id);
    }

    // ════ Delete ══════════════════

    @Override
    public void delete(Long id) {
        File file = getById(id);
        if (file == null) throw new FileException("File not found: " + id);
        if (!removeById(id)) throw new FileException("File not found: " + id);
        deleteQuietly(file.getStored());
        log.info("File deleted: {}", id);
    }

    // ════ Core ════════════════════

    private File store(String userId, String original, InputStream in, BizType type, String description) {
        if (userId == null || userId.isBlank()) throw new FileException("Missing key: userId");
        if (type == null) throw new FileException("Missing key: type");
        String stored = FileUtils.stored(original);
        long size;
        try {
            size = FileUtils.save(Path.of(fileProperties.getRoot()), stored, in);
        } catch (IOException e) {
            deleteQuietly(stored);
            throw new FileException("Failed to store file: " + stored, e);
        }
        File file = File.builder()
                .userId(userId)
                .original(original)
                .stored(stored)
                .description(description)
                .size(size)
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();
        try {
            save(file);
        } catch (RuntimeException e) {
            deleteQuietly(stored);
            throw new FileException("Failed to save file record: " + original, e);
        }
        log.info("File saved: {}", file.getId());
        return file;
    }

    // ════ Helpers ═════════════════

    private static String originalFromUrl(String url) {
        String path;
        try {
            path = URI.create(url).getPath();
        } catch (IllegalArgumentException e) {
            throw new FileException("Malformed url: " + url, e);
        }
        String name = path == null ? "" :
                path.substring(path.lastIndexOf('/') + 1);
        try {
            name = URLDecoder.decode(name, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            // malformed percent-encoding: keep the raw segment
        }
        return name.isBlank() ? "unnamed" : name;
    }

    private HttpResponse<InputStream> download(String url) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            throw new FileException("Malformed url: " + url, e);
        }
        try {
            HttpResponse<InputStream> resp = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() / 100 != 2) {
                IOUtils.closeQuietly(resp.body());
                throw new FileException("Download failed: status=" + resp.statusCode() + ", url=" + url);
            }
            return resp;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileException("Download interrupted: " + url, e);
        } catch (IOException e) {
            throw new FileException("Download failed: " + url, e);
        }
    }

    private void deleteQuietly(String stored) {
        try {
            FileUtils.delete(Path.of(fileProperties.getRoot()), stored);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", stored, e);
        }
    }
}
