package com.zincoid.nullbot.platform.util;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 文件原子移动工具：优先原子移动，文件系统不支持时回退为普通移动。
 * <p>Atomic file move helper: prefers an atomic move and falls back to a normal move
 * when the file system does not support it.</p>
 */
public final class AtomicFiles {

    private AtomicFiles() {
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
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
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
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }
}
