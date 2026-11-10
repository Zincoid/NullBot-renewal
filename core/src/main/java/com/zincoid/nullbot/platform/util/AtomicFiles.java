package com.zincoid.nullbot.platform.util;

import com.zincoid.nullbot.platform.exception.PlatformException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 文件原子写入与移动工具：优先原子移动，文件系统不支持时回退为普通移动。
 * <p>Atomic file write and move helper: prefers an atomic move and falls back to a normal
 * move when the file system does not support it.</p>
 */
public final class AtomicFiles {

    private AtomicFiles() {
    }

    /**
     * 把输入流写入目标文件，先写同目录临时文件，成功后原子替换目标。
     * <p>Writes an input stream to a target file, using a sibling temporary file
     * and atomically replacing the target on success.</p>
     * <p>父目录不存在时自动创建；调用方负责关闭输入流。</p>
     * <p>Missing parent directories are created; the caller owns closing the input stream.</p>
     *
     * @param source the input stream
     * @param target the target file
     * @throws IOException when the file cannot be written or moved
     */
    public static void copy(InputStream source, Path target) throws IOException {
        if (source == null) throw new PlatformException("Missing source");
        if (target == null) throw new PlatformException("Missing target");
        Path file = target.toAbsolutePath().normalize();
        Path dir = file.getParent();
        if (dir == null) throw new IOException("Invalid target: " + target);
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, ".copy-", ".tmp");
        try {
            Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
            moveReplace(temp, file);
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // keep the original failure
            }
        }
    }

    /**
     * 用临时文件替换目标文件，目标已存在时覆盖。
     * <p>Replaces the target file with the temporary file, overwriting an existing target.</p>
     *
     * @param source the temporary file
     * @param target the target file
     * @throws IOException when the move fails
     */
    public static void moveReplace(Path source, Path target) throws IOException {
        move(source, target, true);
    }

    /**
     * 把临时文件移动到不存在的目标位置。
     * <p>Moves the temporary file to a target that must not exist.</p>
     *
     * @param source the temporary file
     * @param target the target file
     * @throws IOException when the move fails or the target already exists
     */
    public static void moveNew(Path source, Path target) throws IOException {
        move(source, target, false);
    }

    private static void move(Path source, Path target, boolean replace) throws IOException {
        if (source == null) throw new PlatformException("Missing source");
        if (target == null) throw new PlatformException("Missing target");
        try {
            if (replace) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } else {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException e) {
            if (replace) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target);
            }
        }
    }
}
