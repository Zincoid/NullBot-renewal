package com.zincoid.nullbot.module.file;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import org.springframework.web.multipart.MultipartFile;

/**
 * File management over flat disk storage and the {@code file} table.
 * <p>基于平铺磁盘存储与 {@code file} 表的文件管理。</p>
 */
public interface FileService extends IService<File> {

    // ════ Save ════════════════════

    /**
     * Downloads a file from a URL and stores it with a new record.
     * <p>从 URL 下载文件并保存新记录。</p>
     *
     * @param userId      uploader platform user ID
     * @param url         source URL
     * @param type        business type
     * @param description optional description
     * @return the persisted record with generated id
     */
    File save(String userId, String url, BizType type, String description);

    /**
     * Stores an uploaded multipart file with a new record.
     * <p>保存上传的 multipart 文件并写入新记录。</p>
     *
     * @param userId      uploader platform user ID
     * @param file        uploaded file
     * @param type        business type
     * @param description optional description
     * @return the persisted record with generated id
     */
    File save(String userId, MultipartFile file, BizType type, String description);

    // ════ Update ══════════════════

    /**
     * Renames the original file name and updates the description.
     * <p>修改原始文件名与描述。</p>
     *
     * @param id          record id
     * @param original    new original file name
     * @param description new description, {@code null} clears it
     */
    void update(Long id, String original, String description);

    // ════ Delete ══════════════════

    /**
     * Deletes the record and its stored file.
     * <p>删除记录及其磁盘文件。</p>
     *
     * @param id record id
     */
    void delete(Long id);
}
