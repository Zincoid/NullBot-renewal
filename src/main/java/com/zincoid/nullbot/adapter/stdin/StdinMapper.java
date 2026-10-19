package com.zincoid.nullbot.adapter.stdin;

import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;

import java.util.stream.Collectors;

/**
 * Converts between terminal text and SPI message content.
 * <p>在终端文本与 SPI 消息内容之间互转。</p>
 */
public final class StdinMapper {

    private StdinMapper() {
    }

    // ════ SPI -> stdin ════════════

    public static String toStdin(MsgContent content) {
        return content.segments().stream()
                .map(seg -> switch (seg) {
                    case MsgSegment.Text s -> s.text();
                    case MsgSegment.Image s -> "[image " + s.url() + "]";
                    case MsgSegment.Video s -> "[video " + s.url() + "]";
                    case MsgSegment.Voice s -> "[voice " + s.url() + "]";
                    case MsgSegment.At s -> "@" + s.userId();
                    case MsgSegment.Reply s -> "[reply " + s.messageId() + "]";
                    case MsgSegment.Face s -> "[face " + s.id() + "]";
                    case MsgSegment.Raw s -> "[" + s.type() + "]";
                })
                .collect(Collectors.joining());
    }

    // ════ stdin -> SPI ════════════

    public static MsgContent fromStdin(String text) {
        return MsgContent.of(text);
    }
}
