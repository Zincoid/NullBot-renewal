package com.zincoid.nullbot.module.message;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.module.file.FileStorage;
import com.zincoid.nullbot.module.http.UrlDownloader;
import com.zincoid.nullbot.platform.persistence.mapper.AttachmentMapper;
import com.zincoid.nullbot.platform.persistence.po.Attachment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link AttachmentService} 的默认实现。
 * <p>Default implementation of {@link AttachmentService}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, Attachment>
        implements AttachmentService {

    private final FileStorage fileStorage;
    private final UrlDownloader urlDownloader;

    // ════ Save ════════════════════

    @Override
    public Attachment save(String messageId, String userId, String url, String original) {
        if (messageId == null || messageId.isBlank())
            throw new AttachmentException("Missing key: messageId");
        if (userId == null || userId.isBlank())
            throw new AttachmentException("Missing key: userId");
        FileStorage.StoredFile saved;
        try (InputStream in = urlDownloader.open(url)) {
            saved = fileStorage.save(original, in);
        } catch (IOException e) {
            throw new AttachmentException("Failed to store attachment: " + original, e);
        }
        Attachment attachment = Attachment.builder()
                .messageId(messageId)
                .userId(userId)
                .original(saved.original())
                .stored(saved.stored())
                .size(saved.size())
                .createdAt(LocalDateTime.now())
                .build();
        try {
            save(attachment);
        } catch (RuntimeException e) {
            deleteQuietly(saved.stored());
            throw new AttachmentException("Failed to save attachment record: " + saved.original(), e);
        }
        log.info("Attachment saved: {}", attachment.getId());
        return attachment;
    }

    // ════ Read ════════════════════

    @Override
    public byte[] read(Attachment attachment) {
        if (attachment == null) throw new AttachmentException("Missing key: attachment");
        try {
            return fileStorage.read(attachment.getStored());
        } catch (IOException e) {
            throw new AttachmentException("Failed to read attachment: " + attachment.getStored(), e);
        }
    }

    @Override
    public String uri(Attachment attachment) {
        if (attachment == null) throw new AttachmentException("Missing key: attachment");
        return fileStorage.uri(attachment.getStored());
    }

    // ════ Message ═════════════════

    @Override
    public List<Attachment> listByMessageId(String messageId) {
        if (messageId == null || messageId.isBlank())
            throw new AttachmentException("Missing key: messageId");
        return lambdaQuery()
                .eq(Attachment::getMessageId, messageId)
                .orderByAsc(Attachment::getId)
                .list();
    }

    @Override
    public void deleteByMessageId(String messageId) {
        List<Attachment> attachments = listByMessageId(messageId);
        if (attachments.isEmpty()) return;
        for (Attachment attachment : attachments) {
            removeById(attachment.getId());
            deleteQuietly(attachment.getStored());
        }
        log.info("Attachments deleted: message={}, count={}", messageId, attachments.size());
    }

    // ════ Helpers ═════════════════

    private void deleteQuietly(String stored) {
        try {
            fileStorage.delete(stored);
        } catch (IOException e) {
            log.warn("Failed to delete attachment file: {}", stored, e);
        }
    }
}
