package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.po.Quote;

import java.time.LocalDateTime;

/**
 * 控制台语录视图。
 * <p>Console view of a quote.</p>
 *
 * @param id       the quote id
 * @param time     the record time
 * @param userId   the author platform user id
 * @param nickname the author nickname
 * @param text     the quote content
 */
public record QuoteView(Long id, LocalDateTime time, String userId, String nickname, String text) {

    /**
     * 由语录记录构建视图。
     * <p>Builds the view from a quote record.</p>
     *
     * @param quote the quote record
     * @return the view
     */
    public static QuoteView of(Quote quote) {
        return new QuoteView(
                quote.getId(),
                quote.getTime(),
                quote.getUserId(),
                quote.getNickname(),
                quote.getText());
    }
}
