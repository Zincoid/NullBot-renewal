package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;

import java.time.LocalDateTime;

/**
 * 控制台文件视图。
 * <p>Console view of a stored file.</p>
 *
 * @param id        the record id
 * @param userId    the uploader platform user id
 * @param original  the original filename
 * @param tag       the file tag
 * @param size      the file size in bytes
 * @param type      the business type
 * @param createdAt the creation time
 */
public record FileView(
        Long id,
        String userId,
        String original,
        String tag,
        long size,
        BizType type,
        LocalDateTime createdAt
) {

    /**
     * 由文件记录构建视图。
     * <p>Builds the view from a file record.</p>
     *
     * @param file the file record
     * @return the view
     */
    public static FileView of(File file) {
        return new FileView(
                file.getId(),
                file.getUserId(),
                file.getOriginal(),
                file.getTag(),
                file.getSize(),
                file.getType(),
                file.getCreatedAt());
    }
}
