package com.zincoid.nullbot.bot.aichat;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.ai.context.ContextPacket;
import com.zincoid.nullbot.module.ai.context.ImagePacket;
import com.zincoid.nullbot.module.http.UrlDownloader;
import com.zincoid.nullbot.module.render.image.ImageData;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天图片上下文：把触发消息与被引用消息中的图片编码为上下文信息包。
 * <p>Chat image context: encodes images from the triggering message and its quoted message into context packets.</p>
 */
@Slf4j
final class ChatImages {

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final String CURRENT = "Image(s) attached to the message to respond to";
    private static final String QUOTED = "Image(s) attached to quoted message id=%s";

    private ChatImages() {
    }

    // ════ Images ═════════════════

    /**
     * 收集触发消息与被引用消息的图片上下文信息包。
     * <p>Collects image context packets from the triggering message and its quoted message.</p>
     *
     * @param client the outbound port
     * @param event  the triggering event
     * @return the image context packets
     */
    static List<ContextPacket> context(BotClient client, BotEvent event) {
        if (!(event instanceof MsgEvent e)) return List.of();
        MsgContent content = e.content();
        List<ContextPacket> packets = new ArrayList<>();
        collect(content, CURRENT, packets);
        String replyId = content.replyId();
        if (replyId != null && !replyId.isBlank()) {
            try {
                MsgDetail detail = client.getMsg(replyId);
                if (detail != null && detail.content() != null)
                    collect(detail.content(), QUOTED.formatted(replyId), packets);
            } catch (RuntimeException ex) {
                log.debug("Quoted message unavailable: {}", ex.getMessage());
            }
        }
        return packets;
    }

    // ════ Helpers ═════════════════

    /**
     * 提取消息内容中的图片并追加为上下文信息包。
     * <p>Extracts images from the message content and appends them as context packets.</p>
     *
     * @param content     the message content
     * @param description image label
     * @param packets     the packets to append to
     */
    private static void collect(MsgContent content, String description,
                                List<ContextPacket> packets) {
        for (MsgSegment segment : content.segments()) {
            if (!(segment instanceof MsgSegment.Image image)) continue;
            String data = dataUri(image.url());
            if (!data.isEmpty()) packets.add(new ImagePacket(data, description));
        }
    }

    /**
     * 把网络图片读取为 image data URI。
     * <p>Reads a network image into an image data URI.</p>
     *
     * @param url the image url
     * @return the data URI, or an empty string when unreadable
     */
    private static String dataUri(String url) {
        if (url == null || url.isBlank()) return "";
        try (InputStream in = UrlDownloader.open(url)) {
            byte[] bytes = in.readNBytes(MAX_IMAGE_BYTES + 1);
            return bytes.length > MAX_IMAGE_BYTES ? "" : ImageData.imageDataUri(bytes);
        } catch (Exception e) {
            log.warn("Failed to read image: {}", e.getMessage());
            return "";
        }
    }
}
