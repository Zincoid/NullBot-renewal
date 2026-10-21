package com.zincoid.nullbot.bot.spi.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息内容：有序消息段列表。
 * <p>Message content: an ordered list of message segments.</p>
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

    // ════ Get ═════════════════════

    /**
     * 是否包含未建模的原始段。
     * <p>Whether the content contains an unmodeled raw segment.</p>
     *
     * @return true when any raw segment is present
     */
    public boolean hasRaw() {
        return segments.stream().anyMatch(MsgSegment.Raw.class::isInstance);
    }

    /**
     * 提取消息内容中所有提及的用户 ID。
     * <p>Extracts the user IDs of all mention segments.</p>
     *
     * @return the mentioned user IDs, in segment order
     */
    public List<String> atIds() {
        return segments.stream()
                .filter(MsgSegment.At.class::isInstance)
                .map(MsgSegment.At.class::cast)
                .map(MsgSegment.At::userId)
                .toList();
    }

    /**
     * 提取回复引用的消息 ID。
     * <p>Extracts the quoted message ID of the reply segment.</p>
     *
     * @return the replied message ID, or null when absent
     */
    public String replyId() {
        return segments.stream()
                .filter(MsgSegment.Reply.class::isInstance)
                .map(MsgSegment.Reply.class::cast)
                .map(MsgSegment.Reply::messageId)
                .findFirst()
                .orElse(null);
    }

    // ════ Builder ═════════════════

    /**
     * 启动流式构建器。
     * <p>Starts a fluent builder.</p>
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 流式消息构建器。
     * <p>Fluent message builder.</p>
     */
    public static final class Builder {

        private final List<MsgSegment> segments = new ArrayList<>();

        private Builder() {
        }

        /**
         * 追加文本段。
         * <p>Appends a text segment.</p>
         *
         * @param text the text content
         */
        public Builder text(String text) {
            segments.add(new MsgSegment.Text(text));
            return this;
        }

        /**
         * 追加图片段。
         * <p>Appends an image segment.</p>
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
         * 追加视频段。
         * <p>Appends a video segment.</p>
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
         * 追加语音段。
         * <p>Appends a voice segment.</p>
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
         * 追加提及段。
         * <p>Appends a mention segment.</p>
         *
         * @param userId mentioned user's id
         */
        public Builder at(String userId) {
            segments.add(new MsgSegment.At(userId));
            return this;
        }

        /**
         * 追加回复段。
         * <p>Appends a reply segment.</p>
         *
         * @param messageId the quoted message id
         */
        public Builder reply(String messageId) {
            segments.add(new MsgSegment.Reply(messageId));
            return this;
        }

        /**
         * 追加表情段。
         * <p>Appends a face segment.</p>
         *
         * @param id the face id
         */
        public Builder face(String id) {
            segments.add(new MsgSegment.Face(id));
            return this;
        }

        /**
         * 构建消息内容。
         * <p>Builds the message content.</p>
         */
        public MsgContent build() {
            return new MsgContent(segments);
        }
    }
}
