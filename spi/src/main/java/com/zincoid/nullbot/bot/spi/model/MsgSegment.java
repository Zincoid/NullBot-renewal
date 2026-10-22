package com.zincoid.nullbot.bot.spi.model;

/**
 * 消息段模型。
 * <p>Message segment model.</p>
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
     * 文本段。
     * <p>Text segment.</p>
     *
     * @param text the text content
     */
    record Text(String text) implements MsgSegment {
    }

    /**
     * 图片段。
     * <p>Image segment.</p>
     *
     * @param url  the image url
     * @param file original filename, nullable
     */
    record Image(String url, String file) implements MsgSegment {

        public Image(String url) {
            this(url, null);
        }
    }

    /**
     * 视频段。
     * <p>Video segment.</p>
     *
     * @param url  the video url
     * @param file original filename, nullable
     */
    record Video(String url, String file) implements MsgSegment {

        public Video(String url) {
            this(url, null);
        }
    }

    /**
     * 语音段。
     * <p>Voice segment.</p>
     *
     * @param url  the voice url
     * @param file original filename, nullable
     */
    record Voice(String url, String file) implements MsgSegment {

        public Voice(String url) {
            this(url, null);
        }
    }

    /**
     * 提及段。
     * <p>Mention segment.</p>
     *
     * @param userId mentioned user's id
     */
    record At(String userId) implements MsgSegment {
    }

    /**
     * 回复段。
     * <p>Reply segment.</p>
     *
     * @param messageId the quoted message id
     */
    record Reply(String messageId) implements MsgSegment {
    }

    /**
     * 表情段。
     * <p>Face segment.</p>
     *
     * @param id the face id
     */
    record Face(String id) implements MsgSegment {
    }

    /**
     * 未建模段回退。
     * <p>Unmodeled segment fallback.</p>
     *
     * @param type the original type
     * @param cq   the raw CQ code
     */
    record Raw(String type, String cq) implements MsgSegment {
    }
}
