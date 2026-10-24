package com.zincoid.nullbot.module.file;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
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
     * @param userId   uploader platform user id
     * @param type     business type
     * @param url      source URL
     * @param original original filename, null falls back to unnamed
     * @return the persisted record with generated id
     */
    File save(String userId, BizType type, String url, String original);

    /**
     * 保存上传的 multipart 文件，使用上传文件自带的原始文件名。
     * <p>Stores an uploaded multipart file using its original filename.</p>
     *
     * @param userId      uploader platform user id
     * @param type        business type
     * @param file        uploaded file
     * @param description optional description, nullable
     * @return the persisted record with generated id
     */
    File save(String userId, BizType type, MultipartFile file, String description);

    // ════ List ════════════════════

    /**
     * 按业务类型与关键词搜索文件，关键词同时匹配描述与原始文件名。
     * <p>Searches files by business type and keyword; the keyword matches both the description and the original filename.</p>
     *
     * @param type    business type
     * @param keyword keyword matched against the description and original filename, nullable
     * @param limit   maximum number of results
     * @return the matching files, newest first
     */
    List<File> search(BizType type, String keyword, int limit);

    /**
     * 按业务类型与关键词分页查询文件，关键词同时匹配描述与原始文件名。
     * <p>Pages files by business type and keyword; the keyword matches both the description and the original filename.</p>
     *
     * @param type    business type, nullable to include all types
     * @param keyword keyword matched against the description and original filename, nullable
     * @param page    one-based page number
     * @param size    page size
     * @return the file page, newest first
     */
    Page<File> page(BizType type, String keyword, long page, long size);

    // ════ Read ════════════════════

    /**
     * 读取已存储文件的字节内容。
     * <p>Reads the stored bytes of a file.</p>
     *
     * @param file file record
     * @return the stored bytes
     */
    byte[] read(File file);

    /**
     * 返回已存储文件的路径，供流式下载使用。
     * <p>Returns the path of a stored file for streaming downloads.</p>
     *
     * @param file file record
     * @return the stored file path
     */
    Path path(File file);

    /**
     * 返回已存储文件的本地 file URI。
     * <p>Returns the local file URI of a stored file.</p>
     *
     * @param file file record
     * @return the file URI
     */
    String uri(File file);

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
     * @return true when the record was deleted
     */
    boolean delete(Long id);
}
