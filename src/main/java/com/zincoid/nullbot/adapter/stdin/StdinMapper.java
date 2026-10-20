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
                    case MsgSegment.Text s -> required(s.text(), "text");
                    case MsgSegment.Image s -> "[image " + required(s.url(), "url") + "]";
                    case MsgSegment.Video s -> "[video " + required(s.url(), "url") + "]";
                    case MsgSegment.Voice s -> "[voice " + required(s.url(), "url") + "]";
                    case MsgSegment.At s -> "@" + required(s.userId(), "userId");
                    case MsgSegment.Reply s -> "[reply " + required(s.messageId(), "messageId") + "]";
                    case MsgSegment.Face s -> "[face " + required(s.id(), "id") + "]";
                    case MsgSegment.Raw s -> "[" + required(s.type(), "type") + "]";
                })
                .collect(Collectors.joining());
    }

    // ════ stdin -> SPI ════════════

    public static MsgContent fromStdin(String text) {
        return MsgContent.of(text);
    }

    // ════ Helpers ═════════════════

    private static String required(String value, String key) {
        if (value == null) throw new StdinException("Null value: " + key);
        return value;
    }
}
