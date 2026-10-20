package com.zincoid.nullbot.adapter.shiro;

import com.mikuac.shiro.enums.MsgTypeEnum;
import com.mikuac.shiro.model.ArrayMsg;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Converts between Shiro {@link ArrayMsg} and SPI message segments.
 * <p>在 Shiro {@link ArrayMsg} 与 SPI 消息段之间互转。</p>
 */
@Slf4j
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
                    .setData(data("text", s.text()));
            case MsgSegment.Image s -> new ArrayMsg().setType(MsgTypeEnum.image)
                    .setData(data("file", s.url()));
            case MsgSegment.Video s -> new ArrayMsg().setType(MsgTypeEnum.video)
                    .setData(data("file", s.url()));
            case MsgSegment.Voice s -> new ArrayMsg().setType(MsgTypeEnum.record)
                    .setData(data("file", s.url()));
            case MsgSegment.At s -> new ArrayMsg().setType(MsgTypeEnum.at)
                    .setData(data("qq", s.userId()));
            case MsgSegment.Reply s -> new ArrayMsg().setType(MsgTypeEnum.reply)
                    .setData(data("id", s.messageId()));
            case MsgSegment.Face s -> new ArrayMsg().setType(MsgTypeEnum.face)
                    .setData(data("id", s.id()));
            case MsgSegment.Raw s -> throw new ShiroException(
                    "Raw message segment is not sendable: " + s.type());
        };
    }

    // ════ Shiro -> SPI ════════════

    public static MsgContent fromShiro(List<ArrayMsg> msgs) {
        return MsgContent.of(msgs.stream().map(ShiroMapper::fromShiro).toList());
    }

    public static MsgSegment fromShiro(ArrayMsg msg) {
        try {
            return switch (msg.getType()) {
                case text -> new MsgSegment.Text(requiredString(msg, "text"));
                case image -> new MsgSegment.Image(requiredString(msg, "url"));
                case video -> new MsgSegment.Video(requiredString(msg, "url"));
                case record -> new MsgSegment.Voice(requiredString(msg, "url"));
                case at -> new MsgSegment.At(requiredString(msg, "qq"));
                case reply -> new MsgSegment.Reply(requiredString(msg, "id"));
                case face -> new MsgSegment.Face(requiredString(msg, "id"));
                default -> raw(msg);
            };
        } catch (ShiroException e) {
            log.warn("Malformed segment degraded to raw: {}", e.getMessage());
            return raw(msg);
        }
    }

    // ════ Helpers ═════════════════

    private static void required(ArrayMsg msg, String key) {
        if (msg.getData() == null || !msg.getData().has(key))
            throw new ShiroException("Missing key: " + key + " in " + msg.getRawType());
    }

    private static String requiredString(ArrayMsg msg, String key) {
        required(msg, key);
        String value = msg.getStringData(key);
        if (value == null || value.isEmpty())
            throw new ShiroException("Empty value: " + key + " in " + msg.getRawType());
        return value;
    }

    private static Map<String, String> data(String key, String value) {
        if (value == null)
            throw new ShiroException("Null value: " + key);
        return Map.of(key, value);
    }

    private static MsgSegment.Raw raw(ArrayMsg msg) {
        return new MsgSegment.Raw(String.valueOf(msg.getType()), msg.toCQCode());
    }
}
