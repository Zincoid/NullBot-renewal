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
     * <p>只有显式指令前缀的消息会返回请求；@、私聊和戳一戳由 AI 中间件处理。</p>
     * <p>Only messages with an explicit command prefix return a request; mentions, private messages, and pokes are handled by the AI middleware.</p>
     *
     * @param event the inbound message event
     * @return the command request, or null when the message is not a command
     */
    public CmdRequest parse(MsgEvent event) {
        String text = text(event.content());
        if (text == null) return null;
        String prefix = prefix(text);
        if (prefix == null) return null;
        String body = text.substring(prefix.length()).trim();
        if (body.isEmpty()) return null;
        List<String> words = List.of(body.split("\\s+"));
        return new CmdRequest(words.getFirst(), words.subList(1, words.size()));
    }

    /**
     * 判断消息是否以配置的指令前缀开头。
     * <p>Whether the message starts with the configured command prefix.</p>
     * <p>引用段和开头的提及段会被跳过。</p>
     * <p>Leading quote and mention segments are skipped.</p>
     *
     * @param content the message content
     * @return true when the message starts with the command prefix
     */
    public boolean isCmd(MsgContent content) {
        String text = text(content);
        return text != null && prefix(text) != null;
    }

    // ════ Helpers ═════════════════

    private String prefix(String text) {
        String prefix = configService.getString(Conf.CMD_PREFIX);
        return prefix != null && !prefix.isEmpty() && text.startsWith(prefix) ? prefix : null;
    }

    private static String text(MsgContent content) {
        List<MsgSegment> segments = content.segments();
        int i = !segments.isEmpty() && segments.getFirst() instanceof MsgSegment.Reply ? 1 : 0;
        while (i < segments.size() && segments.get(i) instanceof MsgSegment.At) i++;
        if (i >= segments.size() || !(segments.get(i) instanceof MsgSegment.Text(String text))) return null;
        return text;
    }
}
