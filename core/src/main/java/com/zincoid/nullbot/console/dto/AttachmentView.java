package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.po.Attachment;
import com.zincoid.nullbot.platform.util.MimeUtils;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import java.time.LocalDateTime;

/**
 * 控制台消息附件视图。
 * <p>Console view of a message attachment.</p>
 *
 * @param id          the attachment id
 * @param original    the original filename
 * @param size        the file size in bytes
 * @param contentType the inferred content type
 * @param createdAt   the upload time
 */
public record AttachmentView(
        Long id,
        String original,
        long size,
        String contentType,
        LocalDateTime createdAt
) {

    /**
     * 由附件记录构建视图。
     * <p>Builds the view from an attachment record.</p>
     *
     * @param attachment the attachment record
     * @return the view
     */
    public static AttachmentView of(Attachment attachment) {
        return new AttachmentView(
                attachment.getId(),
                attachment.getOriginal(),
                attachment.getSize() == null ? 0L : attachment.getSize(),
                contentType(attachment),
                attachment.getCreatedAt());
    }

    /**
     * 推断附件的 MIME 类型，优先使用扩展名映射。
     * <p>Infers the attachment MIME type, preferring the extension mapping.</p>
     *
     * @param attachment the attachment record
     * @return the content type
     */
    public static String contentType(Attachment attachment) {
        String mime = MimeUtils.mime(attachment.getStored());
        if (mime == null) mime = MimeUtils.mime(attachment.getOriginal());
        if (mime != null) return mime;
        return MediaTypeFactory.getMediaType(attachment.getStored())
                .orElse(MediaType.APPLICATION_OCTET_STREAM)
                .toString();
    }
}
