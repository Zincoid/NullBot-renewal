package com.zincoid.nullbot.bot.spi.model;

/**
 * Message segment model.
 * <p>消息段模型。</p>
 */
public sealed interface MsgSegment permits
        MsgSegment.Text,
        MsgSegment.Image,
        MsgSegment.Video,
        MsgSegment.Voice,
        MsgSegment.At,
        MsgSegment.Reply,
        MsgSegment.Face,
        MsgSegment.Raw {

    /**
     * Text segment.
     * <p>文本段。</p>
     *
     * @param text the text content
     */
    record Text(String text) implements MsgSegment {
    }

    /**
     * Image segment.
     * <p>图片段。</p>
     *
     * @param url the image url
     */
    record Image(String url) implements MsgSegment {
    }

    /**
     * Video segment.
     * <p>视频段。</p>
     *
     * @param url the video url
     */
    record Video(String url) implements MsgSegment {
    }

    /**
     * Voice segment.
     * <p>语音段。</p>
     *
     * @param url the voice url
     */
    record Voice(String url) implements MsgSegment {
    }

    /**
     * Mention segment.
     * <p>提及段。</p>
     *
     * @param userId mentioned user's id
     */
    record At(long userId) implements MsgSegment {
    }

    /**
     * Reply segment.
     * <p>回复段。</p>
     *
     * @param messageId the quoted message id
     */
    record Reply(long messageId) implements MsgSegment {
    }

    /**
     * Face segment.
     * <p>表情段。</p>
     *
     * @param id the face id
     */
    record Face(int id) implements MsgSegment {
    }

    /**
     * Unmodeled segment fallback.
     * <p>未建模段回退。</p>
     *
     * @param type the original type
     * @param cq   the raw CQ code
     */
    record Raw(String type, String cq) implements MsgSegment {
    }
}
