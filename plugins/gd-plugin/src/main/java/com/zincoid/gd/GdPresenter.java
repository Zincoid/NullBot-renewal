package com.zincoid.gd;

import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.platform.persistence.enums.Language;

/**
 * GD 输出呈现器：把视图转换为消息内容，便于之后替换为图片卡片。
 * <p>GD output presenter: turns views into message content, so it can later be replaced by image cards.</p>
 */
interface GdPresenter {

    /**
     * 呈现搜索结果列表。
     * <p>Presents the search result list.</p>
     *
     * @param language the reply language
     * @param view     the search view
     * @return the message content
     */
    MsgContent search(Language language, SearchView view);

    /**
     * 呈现关卡详情。
     * <p>Presents the level detail.</p>
     *
     * @param language the reply language
     * @param view     the level view
     * @return the message content
     */
    MsgContent level(Language language, LevelView view);

    /**
     * 呈现 B20 排行。
     * <p>Presents the B20 rank.</p>
     *
     * @param language the reply language
     * @param view     the rank view
     * @return the message content
     */
    MsgContent rank(Language language, RankView view);
}
