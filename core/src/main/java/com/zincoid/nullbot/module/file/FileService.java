package com.zincoid.nullbot.module.file;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     * @param tag         optional tag
     * @return the persisted record with generated id
     */
    File save(String userId, String url, String original, BizType type, String tag);

    /**
     * 从 URL 下载文件并保存新记录，不附带标签。
     * <p>Downloads a file from a URL and stores it with a new record, without a tag.</p>
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
     * 保存上传的 multipart 文件，使用上传文件自带的原始文件名。
     * <p>Stores an uploaded multipart file using its original filename.</p>
     *
     * @param userId uploader platform user id
     * @param file   uploaded file
     * @param type   business type
     * @param tag    optional tag
     * @return the persisted record with generated id
     */
    default File save(String userId, MultipartFile file, BizType type, String tag) {
        return save(userId, file, null, type, tag);
    }

    /**
     * 保存上传的 multipart 文件并使用指定原始文件名写入新记录。
     * <p>Stores an uploaded multipart file with a new record under the given original filename.</p>
     *
     * @param userId   uploader platform user id
     * @param file     uploaded file
     * @param original original filename, null or blank falls back to the multipart filename
     * @param type     business type
     * @param tag      optional tag
     * @return the persisted record with generated id
     */
    File save(String userId, MultipartFile file, String original, BizType type, String tag);

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
     * 按业务类型、标签与原始文件名查询文件，多个条件按与组合。
     * <p>Queries files by business type, tag, and original filename; conditions are combined with AND.</p>
     *
     * @param type     business type
     * @param tag      keyword matched against the tag, nullable
     * @param original keyword matched against the original filename, nullable
     * @param limit    maximum number of results
     * @return the matching files, newest first
     */
    List<File> search(BizType type, String tag, String original, int limit);

    /**
     * 按业务类型、标签与原始文件名分页查询文件，多个条件按与组合。
     * <p>Pages files by business type, tag, and original filename; conditions are combined with AND.</p>
     *
     * @param type     business type, nullable to include all types
     * @param tag      keyword matched against the tag, nullable
     * @param original keyword matched against the original filename, nullable
     * @param page     one-based page number
     * @param size     page size
     * @return the file page, newest first
     */
    Page<File> page(BizType type, String tag, String original, long page, long size);

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
     * 修改原始文件名与标签。
     * <p>Renames the original filename and updates the tag.</p>
     *
     * @param id       record id
     * @param original new original filename
     * @param tag      new tag, {@code null} clears it
     */
    void update(Long id, String original, String tag);

    // ════ Delete ══════════════════

    /**
     * 删除记录及其磁盘文件。
     * <p>Deletes the record and its stored file.</p>
     *
     * @param id record id
     * @return true when the record was deleted
     */
    boolean delete(Long id);
}
