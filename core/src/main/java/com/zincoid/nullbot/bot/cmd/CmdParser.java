package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 把入站事件解析为指令请求。
 * <p>Parses inbound events into command requests.</p>
 */
@Component
@RequiredArgsConstructor
public class CmdParser {

    private final ConfigService configService;

    // ════ Parse ═══════════════════

    /**
     * 把事件解析为指令请求。
     * <p>Parses an event into a command request.</p>
     *
     * @param event the inbound message event
     * @return the command request, or null when the message is not a command
     */
    public CmdRequest parse(MsgEvent event) {
        String text = text(event.content());
        if (text == null) return null;
        int index = match(text);
        if (index < 0) return null;
        String body = text.substring(index).trim();
        if (body.isEmpty()) return null;
        List<String> words = List.of(body.split("\\s+"));
        return new CmdRequest(words.getFirst(), words.subList(1, words.size()));
    }

    /**
     * 判断消息是否以配置的指令前缀开头。
     * <p>Whether the message starts with the configured command prefix.</p>
     *
     * @param content the message content
     * @return true when the message starts with the command prefix
     */
    public boolean isCmd(MsgContent content) {
        String text = text(content);
        return text != null && match(text) >= 0;
    }

    // ════ Helpers ═════════════════

    private int match(String text) {
        String prefix = configService.getString(Conf.CMD_PREFIX);
        if (prefix == null || prefix.isEmpty() || !text.startsWith(prefix)) return -1;
        return prefix.length();
    }

    private static String text(MsgContent content) {
        List<MsgSegment> segments = content.segments();
        int i = 0;
        if (i < segments.size() && segments.get(i) instanceof MsgSegment.Reply) i++;
        while (i < segments.size() && segments.get(i) instanceof MsgSegment.At) i++;
        return i < segments.size() && segments.get(i) instanceof MsgSegment.Text(String text)
                ? text
                : null;
    }
}
