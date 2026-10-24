package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import java.time.LocalDateTime;

/**
 * 控制台文件视图。
 * <p>Console view of a stored file.</p>
 *
 * @param id          the record id
 * @param userId      the uploader platform user id
 * @param original    the original filename
 * @param description the file description
 * @param size        the file size in bytes
 * @param type        the business type
 * @param contentType the stored content type
 * @param createdAt   the creation time
 */
public record FileView(
        Long id,
        String userId,
        String original,
        String description,
        long size,
        BizType type,
        String contentType,
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
                file.getDescription(),
                file.getSize(),
                file.getType(),
                MediaTypeFactory.getMediaType(file.getStored())
                        .orElse(MediaType.APPLICATION_OCTET_STREAM)
                        .toString(),
                file.getCreatedAt());
    }
}
