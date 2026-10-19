package com.zincoid.nullbot.bot.spi.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Message content: an ordered list of message segments.
 * <p>消息内容：有序消息段列表。</p>
 *
 * @param segments the ordered message segments
 */
public record MsgContent(List<MsgSegment> segments) {

    public MsgContent {
        segments = List.copyOf(segments);
    }

    public static MsgContent of(String text) {
        return of(new MsgSegment.Text(text));
    }

    public static MsgContent of(MsgSegment... segments) {
        return new MsgContent(List.of(segments));
    }

    public static MsgContent of(List<MsgSegment> segments) {
        return new MsgContent(segments);
    }

    // ════ Builder ═════════════════

    /**
     * Starts a fluent builder.
     * <p>启动流式构建器。</p>
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent message builder.
     * <p>流式消息构建器。</p>
     */
    public static final class Builder {

        private final List<MsgSegment> segments = new ArrayList<>();

        private Builder() {
        }

        /**
         * Appends a text segment.
         * <p>追加文本段。</p>
         *
         * @param text the text content
         */
        public Builder text(String text) {
            segments.add(new MsgSegment.Text(text));
            return this;
        }

        /**
         * Appends an image segment.
         * <p>追加图片段。</p>
         *
         * @param url the image source:
         *            local file (file:///...),
         *            base64 (base64://...),
         *            network url
         */
        public Builder image(String url) {
            segments.add(new MsgSegment.Image(url));
            return this;
        }

        /**
         * Appends a video segment.
         * <p>追加视频段。</p>
         *
         * @param url the video source:
         *            local file (file:///...),
         *            network url
         */
        public Builder video(String url) {
            segments.add(new MsgSegment.Video(url));
            return this;
        }

        /**
         * Appends a voice segment.
         * <p>追加语音段。</p>
         *
         * @param url the voice source:
         *            local file (file:///...),
         *            base64 (base64://...),
         *            network url
         */
        public Builder voice(String url) {
            segments.add(new MsgSegment.Voice(url));
            return this;
        }

        /**
         * Appends a mention segment.
         * <p>追加提及段。</p>
         *
         * @param userId mentioned user's id
         */
        public Builder at(long userId) {
            segments.add(new MsgSegment.At(userId));
            return this;
        }

        /**
         * Appends a reply segment.
         * <p>追加回复段。</p>
         *
         * @param messageId the quoted message id
         */
        public Builder reply(long messageId) {
            segments.add(new MsgSegment.Reply(messageId));
            return this;
        }

        /**
         * Appends a face segment.
         * <p>追加表情段。</p>
         *
         * @param id the face id
         */
        public Builder face(int id) {
            segments.add(new MsgSegment.Face(id));
            return this;
        }

        /**
         * Builds the message content.
         * <p>构建消息内容。</p>
         */
        public MsgContent build() {
            return new MsgContent(segments);
        }
    }
}
