package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.event.RecallEvent;
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

    public static final String CHAT_CMD_NAME = "Chat";
    public static final String POKED_CMD_NAME = "Poked";

    private final ConfigService configService;

    // ════ Parse ═══════════════════

    /**
     * 把事件解析为指令请求。
     * <p>Parses an event into a command request.</p>
     *
     * @param event the inbound event
     * @return the command request, or null when the event is not a command
     */
    public CmdRequest parse(BotEvent event) {
        return switch (event) {
            case GroupMsgEvent e -> {
                boolean at = e.at();
                yield at
                        ? new CmdRequest(CHAT_CMD_NAME, List.of())
                        : msg(e.content());
            }
            case PrivateMsgEvent e -> {
                CmdRequest req = msg(e.content());
                yield req == null
                        ? new CmdRequest(CHAT_CMD_NAME, List.of())
                        : req;
            }
            case GroupPokeEvent e -> poke(e.selfId(), e.targetId());
            case PrivatePokeEvent e -> poke(e.selfId(), e.targetId());
            case RecallEvent _, JoinEvent _, LeaveEvent _ -> null;
        };
    }

    // ════ Helpers ═════════════════

    /**
     * 判断消息是否以配置的指令前缀开头，回复消息只检查引用后的文本段。
     * <p>Whether the message starts with the configured command prefix; for reply messages only the text after the quote is checked.</p>
     *
     * @param content the message content
     * @return true when the message starts with the command prefix
     */
    public boolean isCmd(MsgContent content) {
        String text = cmdText(content);
        return text != null && cmdPrefix(text) != null;
    }

    private CmdRequest msg(MsgContent content) {
        String text = cmdText(content);
        if (text == null) return null;
        String prefix = cmdPrefix(text);
        if (prefix == null) return null;
        String body = text.substring(prefix.length()).trim();
        if (body.isEmpty()) return null;
        List<String> words = List.of(body.split("\\s+"));
        return new CmdRequest(words.getFirst(), words.subList(1, words.size()));
    }

    private String cmdPrefix(String text) {
        String prefix = configService.getString(Conf.CMD_PREFIX);
        return prefix != null && !prefix.isEmpty() && text.startsWith(prefix) ? prefix : null;
    }

    private static String cmdText(MsgContent content) {
        List<MsgSegment> segments = content.segments();
        int i = !segments.isEmpty() && segments.getFirst() instanceof MsgSegment.Reply ? 1 : 0;
        if (i >= segments.size() || !(segments.get(i) instanceof MsgSegment.Text(String text))) return null;
        return text;
    }

    private static CmdRequest poke(String selfId, String targetId) {
        return selfId.equals(targetId) ? new CmdRequest(POKED_CMD_NAME, List.of()) : null;
    }
}
