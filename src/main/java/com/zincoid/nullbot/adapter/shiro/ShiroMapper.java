package com.zincoid.nullbot.adapter.shiro;

import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;

import java.util.List;
import java.util.Map;

/**
 * Converts between Shiro {@link ArrayMsg} and SPI message segments.
 * <p>在 Shiro {@link ArrayMsg} 与 SPI 消息段之间互转。</p>
 */
public final class ShiroMapper {

    private ShiroMapper() {
    }

    // ════ SPI -> Shiro ════════════

    public static List<ArrayMsg> toShiro(MsgContent content) {
        return content.segments().stream().map(ShiroMapper::toShiro).toList();
    }

    public static ArrayMsg toShiro(MsgSegment segment) {
        return switch (segment) {
            case MsgSegment.Text s -> new ArrayMsg().setType(MsgTypeEnum.text)
                    .setData(Map.of("text", s.text()));
            case MsgSegment.Image s -> new ArrayMsg().setType(MsgTypeEnum.image)
                    .setData(Map.of("file", s.url()));
            case MsgSegment.Video s -> new ArrayMsg().setType(MsgTypeEnum.video)
                    .setData(Map.of("file", s.url()));
            case MsgSegment.Voice s -> new ArrayMsg().setType(MsgTypeEnum.record)
                    .setData(Map.of("file", s.url()));
            case MsgSegment.At s -> new ArrayMsg().setType(MsgTypeEnum.at)
                    .setData(Map.of("qq", String.valueOf(s.userId())));
            case MsgSegment.Reply s -> new ArrayMsg().setType(MsgTypeEnum.reply)
                    .setData(Map.of("id", String.valueOf(s.messageId())));
            case MsgSegment.Face s -> new ArrayMsg().setType(MsgTypeEnum.face)
                    .setData(Map.of("id", String.valueOf(s.id())));
            case MsgSegment.Raw s -> throw new IllegalArgumentException(
                    "Raw message segment is not sendable: " + s.type());
        };
    }

    // ════ Shiro -> SPI ════════════

    public static MsgContent fromShiro(List<ArrayMsg> msgs) {
        return MsgContent.of(msgs.stream().map(ShiroMapper::fromShiro).toList());
    }

    public static MsgSegment fromShiro(ArrayMsg msg) {
        return switch (msg.getType()) {
            case text -> new MsgSegment.Text(str(msg, "text"));
            case image -> new MsgSegment.Image(str(msg, "url"));
            case video -> new MsgSegment.Video(str(msg, "url"));
            case record -> new MsgSegment.Voice(str(msg, "url"));
            case at -> new MsgSegment.At(num(msg, "qq"));
            case reply -> new MsgSegment.Reply(num(msg, "id"));
            case face -> new MsgSegment.Face((int) num(msg, "id"));
            default -> new MsgSegment.Raw(String.valueOf(msg.getType()), msg.toCQCode());
        };
    }

    // ════ Helpers ═════════════════

    private static void required(ArrayMsg msg, String key) {
        if (msg.getData() == null || !msg.getData().has(key))
            throw new ShiroException("Missing key: " + key + " in " + msg.getRawType());
    }

    private static long num(ArrayMsg msg, String key) {
        required(msg, key);
        return msg.getLongData(key);
    }

    private static String str(ArrayMsg msg, String key) {
        required(msg, key);
        return msg.getStringData(key);
    }
}
