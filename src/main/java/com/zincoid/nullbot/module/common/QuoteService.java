package com.zincoid.nullbot.module.common;

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
     * @param userId   author platform user ID
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
     * @param userId author platform user ID
     * @return the quote, or null when the author has none
     */
    Quote random(String userId);
}
