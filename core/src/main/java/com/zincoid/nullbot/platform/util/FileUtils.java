package com.zincoid.nullbot.platform.util;

import org.apache.commons.io.FilenameUtils;

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
 * 平铺存储磁盘工具：存储名生成与原始字节读写。
 * <p>Flat-storage disk helpers: stored naming and raw byte IO.</p>
 */
public final class FileUtils {

    private FileUtils() {
    }

    // ════ Name ════════════════════

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

    // ════ IO ══════════════════════

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

    /**
     * 读取文件头推断扩展名，以生成的存储名写入 {@code root}。
     * <p>Infers the extension from the file header and writes the stream under a generated stored name.</p>
     *
     * @param root     storage root directory, created when absent
     * @param original original filename, may be null or blank
     * @param in       source stream, not closed by this method
     * @return the stored filename, corrected original filename, and bytes written
     * @throws IOException when the file cannot be written
     */
    public static StoredFile save(Path root, String original, InputStream in) throws IOException {
        byte[] header = in.readNBytes(16);
        String mime = MimeUtils.mime(header);
        if (mime == null) mime = MimeUtils.mime(original);
        String extension = MimeUtils.extension(mime);
        String corrected = corrected(original, extension);
        String stored = stored(corrected);
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

    /**
     * 删除已存储的文件（若存在）。
     * <p>Removes the stored file when present.</p>
     *
     * @param root   storage root directory
     * @param stored stored filename
     * @throws IOException when the file cannot be removed
     */
    public static void delete(Path root, String stored) throws IOException {
        Files.deleteIfExists(root.resolve(stored));
    }
}
