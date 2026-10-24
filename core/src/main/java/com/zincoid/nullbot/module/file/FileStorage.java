package com.zincoid.nullbot.module.file;

import com.zincoid.nullbot.platform.configuration.FileProperties;
import com.zincoid.nullbot.platform.util.MimeUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * 文件服务的平铺磁盘存储：存储名生成与原始字节读写。
 * <p>Flat disk storage for the file service: stored naming and raw byte IO.</p>
 */
@Component
@RequiredArgsConstructor
public class FileStorage {

    private final FileProperties fileProperties;

    // ════ Save ════════════════════

    /**
     * 读取文件头推断扩展名，以生成的存储名写入存储根目录。
     * <p>Infers the extension from the file header and writes the stream under a generated stored name.</p>
     *
     * @param original original filename, may be null or blank
     * @param in       source stream, not closed by this method
     * @return the stored filename, corrected original filename, and bytes written
     * @throws IOException when the file cannot be written
     */
    public StoredFile save(String original, InputStream in) throws IOException {
        byte[] header = in.readNBytes(16);
        String mime = MimeUtils.mime(header);
        if (mime == null) mime = MimeUtils.mime(original);
        String extension = MimeUtils.extension(mime);
        String corrected = corrected(original, extension);
        String stored = stored(corrected);
        Path root = root();
        Path target = root.resolve(stored);
        Files.createDirectories(root);
        try (InputStream body = new SequenceInputStream(new ByteArrayInputStream(header), in)) {
            long size = Files.copy(body, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(stored, corrected, size);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // keep the original failure
            }
            throw e;
        }
    }

    // ════ Read ════════════════════

    /**
     * 读取已存储文件的字节内容。
     * <p>Reads the stored bytes of a file.</p>
     *
     * @param stored stored filename
     * @return the stored bytes
     * @throws IOException when the file cannot be read
     */
    public byte[] read(String stored) throws IOException {
        return Files.readAllBytes(path(stored));
    }

    /**
     * 返回已存储文件的路径。
     * <p>Returns the path of a stored file.</p>
     *
     * @param stored stored filename
     * @return the stored file path
     */
    public Path path(String stored) {
        return root().resolve(stored);
    }

    /**
     * 返回已存储文件的本地 file URI。
     * <p>Returns the local file URI of a stored file.</p>
     *
     * @param stored stored filename
     * @return the file URI
     */
    public String uri(String stored) {
        return path(stored).toAbsolutePath().normalize().toUri().toString();
    }

    // ════ Delete ══════════════════

    /**
     * 删除已存储的文件（若存在）。
     * <p>Removes the stored file when present.</p>
     *
     * @param stored stored filename
     * @throws IOException when the file cannot be removed
     */
    public void delete(String stored) throws IOException {
        Files.deleteIfExists(path(stored));
    }

    // ════ Helpers ═════════════════

    /**
     * 返回存储根目录。
     * <p>Returns the storage root directory.</p>
     *
     * @return the storage root
     */
    private Path root() {
        return Path.of(fileProperties.getRoot());
    }

    /**
     * 用推断出的扩展名纠正原始文件名的后缀。
     * <p>Corrects the extension of the original filename with the inferred extension.</p>
     *
     * @param original  original filename, may be null or blank
     * @param extension inferred extension, nullable
     * @return the corrected filename, or the original when no extension was inferred
     */
    private static String corrected(String original, String extension) {
        String name = original == null ? "" : original;
        if (extension == null) return name.isBlank() ? "unnamed" : name;
        String base = FilenameUtils.removeExtension(name);
        if (base.isBlank()) base = "unnamed";
        return base + "." + extension;
    }

    /**
     * 根据纠正后的文件名生成随机存储名，缺少扩展名时使用 bin。
     * <p>Builds a random stored name from the corrected filename, falling back to bin.</p>
     *
     * @param original corrected filename, may be null or blank
     * @return UUID-based stored name that always carries an extension
     */
    private static String stored(String original) {
        String ext = original == null ? "" : FilenameUtils.getExtension(original);
        if (ext.isEmpty()) ext = "bin";
        String id = UUID.randomUUID().toString();
        return id + "." + ext.toLowerCase(Locale.ROOT);
    }

    /**
     * 存储结果：生成的存储名、纠正后缀后的原始文件名与实际写入字节数。
     * <p>Storage result: the generated stored name, the extension-corrected original filename, and the bytes written.</p>
     *
     * @param stored   stored filename
     * @param original extension-corrected original filename
     * @param size     bytes written
     */
    public record StoredFile(String stored, String original, long size) {
    }
}
