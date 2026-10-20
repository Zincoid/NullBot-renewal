package com.zincoid.nullbot.platform.util;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Flat-storage disk helpers: generated stored names and raw byte IO.
 * <p>平铺存储磁盘工具：生成存储名与原始字节读写。</p>
 */
public final class FileUtils {

    private FileUtils() {
    }

    /**
     * Builds a random stored file name from the original name.
     * <p>根据原始文件名生成随机存储文件名。</p>
     *
     * @param original original file name, may be blank
     * @return UUID-based stored name, with lower-cased extension when derivable, otherwise bare UUID
     */
    public static String stored(String original) {
        String ext = original == null ? "" : FilenameUtils.getExtension(original);
        String id = UUID.randomUUID().toString();
        return ext.isEmpty() ? id : id + "." + ext.toLowerCase(Locale.ROOT);
    }

    /**
     * Writes the stream to {@code root/stored} and returns the byte count.
     * <p>将流写入 {@code root/stored} 并返回字节数。</p>
     *
     * @param root   storage root directory, created when absent
     * @param stored stored file name produced by {@link #stored}
     * @param in     source stream, not closed by this method
     * @return bytes written
     * @throws IOException when the file cannot be written
     */
    public static long save(Path root, String stored, InputStream in) throws IOException {
        Files.createDirectories(root);
        return Files.copy(in, root.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Removes the stored file when present.
     * <p>删除已存储的文件（若存在）。</p>
     *
     * @param root   storage root directory
     * @param stored stored file name
     * @throws IOException when the file cannot be removed
     */
    public static void delete(Path root, String stored) throws IOException {
        Files.deleteIfExists(root.resolve(stored));
    }
}
