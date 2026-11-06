package com.zincoid.nullbot.module.message;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.Attachment;

import java.util.List;

/**
 * 聊天附件管理：基于平铺磁盘存储与 {@code attachment} 表。
 * <p>Chat attachment management over flat disk storage and the {@code attachment} table.</p>
 */
public interface AttachmentService extends IService<Attachment> {

    /**
     * 从 URL 下载附件并保存为指定消息的附件。
     * <p>Downloads an attachment from a URL and stores it under the given message.</p>
     *
     * @param messageId owning message id
     * @param userId    uploader platform user id
     * @param url       source URL
     * @param original  original filename, null falls back to unnamed
     * @return the persisted record with generated id
     */
    Attachment save(String messageId, String userId, String url, String original);

    /**
     * 读取已存储附件的字节内容。
     * <p>Reads the stored bytes of an attachment.</p>
     *
     * @param attachment attachment record
     * @return the stored bytes
     */
    byte[] read(Attachment attachment);

    /**
     * 返回已存储附件的本地 file URI。
     * <p>Returns the local file URI of a stored attachment.</p>
     *
     * @param attachment attachment record
     * @return the file URI
     */
    String uri(Attachment attachment);

    /**
     * 按消息 ID 返回附件，按写入顺序排列。
     * <p>Returns the attachments of a message in insertion order.</p>
     *
     * @param messageId owning message id
     * @return attachments owned by the message
     */
    List<Attachment> listByMessageId(String messageId);

    /**
     * 删除指定消息的全部附件及其磁盘文件。
     * <p>Deletes all attachments of the given message and their stored files.</p>
     *
     * @param messageId owning message id
     */
    void deleteByMessageId(String messageId);
}
