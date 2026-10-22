package com.zincoid.nullbot.module.file;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.configuration.FileProperties;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.mapper.FileMapper;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.util.FileUtils;
import com.zincoid.nullbot.platform.util.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
    public File save(String userId, String url, String original, BizType type, String tag) {
        if (original == null || original.isBlank()) original = "unnamed";
        HttpResponse<InputStream> resp = download(url);
        try (InputStream in = resp.body()) {
            return store(userId, original, in, type, tag);
        } catch (IOException e) {
            throw new FileException("Failed to read download body: " + url, e);
        }
    }

    @Override
    public File save(String userId, MultipartFile file, BizType type, String tag) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) original = "unnamed";
        try (InputStream in = file.getInputStream()) {
            return store(userId, original, in, type, tag);
        } catch (IOException e) {
            throw new FileException("Failed to read upload: " + original, e);
        }
    }

    // ════ Read ════════════════════

    @Override
    public byte[] read(Long id) {
        if (id == null) throw new FileException("Missing key: id");
        File file = getById(id);
        if (file == null) throw new FileException("File not found: " + id);
        try {
            return Files.readAllBytes(Path.of(fileProperties.getRoot()).resolve(file.getStored()));
        } catch (IOException e) {
            throw new FileException("Failed to read file: " + file.getStored(), e);
        }
    }

    @Override
    public String uri(File file) {
        if (file == null) throw new FileException("Missing key: file");
        return Path.of(fileProperties.getRoot())
                .resolve(file.getStored())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
    }

    @Override
    public List<File> search(BizType type, String tag, String original, int limit) {
        if (type == null) throw new FileException("Missing key: type");
        int size = Math.clamp(limit, 1, 50);
        String tagKey = normalize(tag);
        String originalKey = normalize(original);
        return lambdaQuery()
                .eq(File::getType, type)
                .like(tagKey != null, File::getTag, tagKey)
                .like(originalKey != null, File::getOriginal, originalKey)
                .orderByDesc(File::getId)
                .last("LIMIT " + size)
                .list();
    }

    @Override
    public Page<File> page(BizType type, String tag, String original, long page, long size) {
        String tagKey = normalize(tag);
        String originalKey = normalize(original);
        return lambdaQuery()
                .eq(type != null, File::getType, type)
                .like(tagKey != null, File::getTag, tagKey)
                .like(originalKey != null, File::getOriginal, originalKey)
                .orderByDesc(File::getId)
                .page(PageUtils.of(page, size));
    }

    // ════ Random ══════════════════

    @Override
    public File random(BizType type) {
        if (type == null) throw new FileException("Missing key: type");
        long count = lambdaQuery().eq(File::getType, type).count();
        if (count == 0) return null;
        long offset = ThreadLocalRandom.current().nextLong(count);
        return lambdaQuery()
                .eq(File::getType, type)
                .last("LIMIT 1 OFFSET " + offset)
                .one();
    }

    // ════ Update ══════════════════

    @Override
    public void update(Long id, String original, String tag) {
        if (original == null || original.isBlank())
            throw new FileException("Missing filename: " + id);
        boolean updated = lambdaUpdate()
                .eq(File::getId, id)
                .set(File::getOriginal, original)
                .set(File::getTag, tag)
                .update();
        if (!updated) throw new FileException("File not found: " + id);
    }

    // ════ Delete ══════════════════

    @Override
    public boolean delete(Long id) {
        File file = getById(id);
        if (file == null) return false;
        if (!removeById(id)) return false;
        deleteQuietly(file.getStored());
        log.info("File deleted: {}", id);
        return true;
    }

    // ════ Core ════════════════════

    private File store(String userId, String original, InputStream in, BizType type, String tag) {
        if (userId == null || userId.isBlank()) throw new FileException("Missing key: userId");
        if (type == null) throw new FileException("Missing key: type");
        FileUtils.StoredFile saved;
        try {
            saved = FileUtils.save(Path.of(fileProperties.getRoot()), original, in);
        } catch (IOException e) {
            throw new FileException("Failed to store file: " + original, e);
        }
        File file = File.builder()
                .userId(userId)
                .original(saved.original())
                .stored(saved.stored())
                .tag(tag)
                .size(saved.size())
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();
        try {
            save(file);
        } catch (RuntimeException e) {
            deleteQuietly(saved.stored());
            throw new FileException("Failed to save file record: " + saved.original(), e);
        }
        log.info("File saved: {}", file.getId());
        return file;
    }

    // ════ Helpers ═════════════════

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
