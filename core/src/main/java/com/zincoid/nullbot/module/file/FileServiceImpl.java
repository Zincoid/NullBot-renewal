package com.zincoid.nullbot.module.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.mapper.FileMapper;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.util.MimeUtils;
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

    public static final int MAX_LIMIT = Integer.MAX_VALUE;

    private final FileStorage fileStorage;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // ════ Save ════════════════════

    @Override
    public File save(String userId, BizType type, String url, String original) {
        validate(userId, type);
        if (url == null || url.isBlank()) throw new FileException("Missing key: url");
        HttpResponse<InputStream> resp = download(url);
        try (InputStream in = resp.body()) {
            return store(userId, original, in, type, null);
        } catch (IOException e) {
            throw new FileException("Failed to read download body", e);
        }
    }

    @Override
    public File save(String userId, BizType type, MultipartFile file, String description) {
        validate(userId, type);
        if (file == null) throw new FileException("Missing key: file");
        String name = file.getOriginalFilename();
        try (InputStream in = file.getInputStream()) {
            return store(userId, name, in, type, description);
        } catch (IOException e) {
            throw new FileException("Failed to read upload: " + name, e);
        }
    }

    // ════ List ════════════════════

    @Override
    public List<File> search(BizType type, String keyword, int limit) {
        if (type == null) throw new FileException("Missing key: type");
        int size = Math.clamp(limit, 1, MAX_LIMIT);
        return baseMapper.selectList(query(type, keyword).last("LIMIT " + size));
    }

    @Override
    public Page<File> page(BizType type, String keyword, long page, long size) {
        return baseMapper.selectPage(PageUtils.of(page, size), query(type, keyword));
    }

    // ════ Read ════════════════════

    @Override
    public byte[] read(File file) {
        if (file == null) throw new FileException("Missing key: file");
        try {
            return fileStorage.read(file.getStored());
        } catch (IOException e) {
            throw new FileException("Failed to read file: " + file.getStored(), e);
        }
    }

    @Override
    public Path path(File file) {
        if (file == null) throw new FileException("Missing key: file");
        return fileStorage.path(file.getStored());
    }

    @Override
    public String uri(File file) {
        if (file == null) throw new FileException("Missing key: file");
        return fileStorage.uri(file.getStored());
    }

    @Override
    public String imageUri(String fileId) {
        if (fileId == null || fileId.isBlank()) return null;
        try {
            File file = getById(Long.valueOf(fileId));
            if (file == null) return null;
            String mime = MimeUtils.mime(file.getStored());
            if (mime == null) mime = MimeUtils.mime(file.getOriginal());
            return mime != null && mime.startsWith("image/") ? uri(file) : null;
        } catch (RuntimeException e) {
            return null;
        }
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
    public void update(Long id, String original, String description) {
        if (original == null || original.isBlank())
            throw new FileException("Missing filename: " + id);
        boolean updated = lambdaUpdate()
                .eq(File::getId, id)
                .set(File::getOriginal, original)
                .set(File::getDescription, description)
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

    private File store(String userId, String original, InputStream in, BizType type, String description) {
        FileStorage.StoredFile saved;
        try {
            saved = fileStorage.save(original, in);
        } catch (IOException e) {
            throw new FileException("Failed to store file: " + original, e);
        }
        File file = File.builder()
                .userId(userId)
                .original(saved.original())
                .stored(saved.stored())
                .description(description)
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

    private static void validate(String userId, BizType type) {
        if (userId == null || userId.isBlank()) throw new FileException("Missing key: userId");
        if (type == null) throw new FileException("Missing key: type");
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LambdaQueryWrapper<File> query(BizType type, String keyword) {
        String key = normalize(keyword);
        return Wrappers.lambdaQuery(File.class)
                .eq(type != null, File::getType, type)
                .and(key != null, wrapper -> wrapper
                        .like(File::getDescription, key)
                        .or()
                        .like(File::getOriginal, key))
                .orderByDesc(File::getId);
    }

    private void deleteQuietly(String stored) {
        try {
            fileStorage.delete(stored);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", stored, e);
        }
    }

    private HttpResponse<InputStream> download(String url) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            throw new FileException("Malformed URL", e);
        }
        try {
            HttpResponse<InputStream> resp = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() / 100 != 2) {
                IOUtils.closeQuietly(resp.body());
                throw new FileException("Download failed: status=" + resp.statusCode());
            }
            return resp;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileException("Download interrupted", e);
        } catch (IOException e) {
            throw new FileException("Download failed", e);
        }
    }
}
