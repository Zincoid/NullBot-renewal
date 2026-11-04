package com.zincoid.nullbot.module.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.Quote;

/**
 * 语录服务：保存、删除与随机读取。
 * <p>Quote service: saving, deleting, and random reads.</p>
 */
public interface QuoteService extends IService<Quote> {

    // ════ Add ═════════════════════

    /**
     * 保存一条语录。
     * <p>Saves a quote.</p>
     *
     * @param userId   author platform user id
     * @param nickname author nickname
     * @param text     quote content
     * @return the persisted record
     */
    Quote add(String userId, String nickname, String text);

    // ════ Delete ══════════════════

    /**
     * 删除一条语录。
     * <p>Deletes a quote.</p>
     *
     * @param id quote id
     */
    void delete(Long id);

    // ════ Random ══════════════════

    /**
     * 随机读取一条语录。
     * <p>Returns a random quote.</p>
     *
     * @return the quote, or null when the table is empty
     */
    Quote random();

    /**
     * 随机读取指定作者的一条语录。
     * <p>Returns a random quote of the given author.</p>
     *
     * @param userId author platform user id
     * @return the quote, or null when the author has none
     */
    Quote random(String userId);

    // ════ Format ══════════════════

    /**
     * 格式化一条语录。
     * <p>Formats one quote.</p>
     *
     * @param quote the quote
     * @return the formatted text
     */
    String format(Quote quote);

    /**
     * 判断文本是否已是语录格式。
     * <p>Whether the text already has the quote format.</p>
     *
     * @param text the text
     * @return true when already formatted as a quote
     */
    boolean isFormatted(String text);

    // ════ Page ════════════════════

    /**
     * 按关键词分页查询语录。
     * <p>Pages quotes by keyword.</p>
     *
     * @param keyword keyword matched against text, nickname, or user id, nullable
     * @param page    one-based page number
     * @param size    page size
     * @return the quote page, newest first
     */
    Page<Quote> page(String keyword, long page, long size);
}
