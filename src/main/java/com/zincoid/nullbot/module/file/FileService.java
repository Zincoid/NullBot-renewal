package com.zincoid.nullbot.module.file;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 基于平铺磁盘存储与 {@code file} 表的文件管理。
 * <p>File management over flat disk storage and the {@code file} table.</p>
 */
public interface FileService extends IService<File> {

    // ════ Save ════════════════════

    /**
     * 从 URL 下载文件并保存新记录。
     * <p>Downloads a file from a URL and stores it with a new record.</p>
     *
     * @param userId      uploader platform user id
     * @param url         source URL
     * @param original    original filename, null falls back to unnamed
     * @param type        business type
     * @param description optional description
     * @return the persisted record with generated id
     */
    File save(String userId, String url, String original, BizType type, String description);

    /**
     * 从 URL 下载文件并保存新记录，不附带描述。
     * <p>Downloads a file from a URL and stores it with a new record, without a description.</p>
     *
     * @param userId   uploader platform user id
     * @param url      source URL
     * @param original original filename, null falls back to unnamed
     * @param type     business type
     * @return the persisted record with generated id
     */
    default File save(String userId, String url, String original, BizType type) {
        return save(userId, url, original, type, null);
    }

    /**
     * 保存上传的 multipart 文件并写入新记录。
     * <p>Stores an uploaded multipart file with a new record.</p>
     *
     * @param userId      uploader platform user id
     * @param file        uploaded file
     * @param type        business type
     * @param description optional description
     * @return the persisted record with generated id
     */
    File save(String userId, MultipartFile file, BizType type, String description);

    /**
     * 保存上传的 multipart 文件并写入新记录，不附带描述。
     * <p>Stores an uploaded multipart file with a new record, without a description.</p>
     *
     * @param userId uploader platform user id
     * @param file   uploaded file
     * @param type   business type
     * @return the persisted record with generated id
     */
    default File save(String userId, MultipartFile file, BizType type) {
        return save(userId, file, type, null);
    }

    // ════ Read ════════════════════

    /**
     * 读取已存储文件的字节内容。
     * <p>Reads the stored bytes of a file.</p>
     *
     * @param id record id
     * @return the stored bytes
     */
    byte[] read(Long id);

    /**
     * 返回已存储文件的本地 file URI。
     * <p>Returns the local file URI of a stored file.</p>
     *
     * @param file file record
     * @return the file URI
     */
    String uri(File file);

    /**
     * 按业务类型与关键词查询文件。
     * <p>Queries files by business type and keyword.</p>
     *
     * @param type    business type
     * @param keyword keyword matched against description or original filename, nullable
     * @param limit   maximum number of results
     * @return the matching files, newest first
     */
    List<File> search(BizType type, String keyword, int limit);

    // ════ Random ══════════════════

    /**
     * 按业务类型随机读取一个文件。
     * <p>Returns a random file of the given business type.</p>
     *
     * @param type business type
     * @return a random file, or null when none exists
     */
    File random(BizType type);

    // ════ Update ══════════════════

    /**
     * 修改原始文件名与描述。
     * <p>Renames the original filename and updates the description.</p>
     *
     * @param id          record id
     * @param original    new original filename
     * @param description new description, {@code null} clears it
     */
    void update(Long id, String original, String description);

    // ════ Delete ══════════════════

    /**
     * 删除记录及其磁盘文件。
     * <p>Deletes the record and its stored file.</p>
     *
     * @param id record id
     */
    void delete(Long id);
}
