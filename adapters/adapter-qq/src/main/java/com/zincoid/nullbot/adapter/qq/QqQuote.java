package com.zincoid.nullbot.adapter.qq;

import com.zincoid.nullbot.bot.spi.model.MsgDetail;

/**
 * 一条被引用消息。
 * <p>A quoted message.</p>
 *
 * @param shortId  the internal short id
 * @param actualId the platform message id or scene index
 * @param detail   the message detail
 */
public record QqQuote(String shortId, String actualId, MsgDetail detail) {

    /**
     * 按平台消息 ID 创建引用消息。
     * <p>Creates a quoted message from the platform message id.</p>
     *
     * @param actualId the platform message id or scene index
     * @param detail   the message detail
     * @return the quoted message
     */
    public static QqQuote of(String actualId, MsgDetail detail) {
        String shortId = QqIds.shortId(actualId);
        return new QqQuote(shortId, actualId, new MsgDetail(
                shortId, detail.userId(), detail.nickname(), detail.content()));
    }
}
