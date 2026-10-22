package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.GroupRecallEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateRecallEvent;
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
            case GroupRecallEvent _, PrivateRecallEvent _ -> recall();
            case JoinEvent _ -> join();
            case LeaveEvent _ -> left();
        };
    }

    // ════ Helpers ═════════════════

    private CmdRequest msg(MsgContent content) {
        List<MsgSegment> segments = content.segments();
        int i = !segments.isEmpty() && segments.getFirst() instanceof MsgSegment.Reply ? 1 : 0;
        if (i >= segments.size() || !(segments.get(i) instanceof MsgSegment.Text(String text))) return null;
        String prefix = configService.getString(Conf.CMD_PREFIX);
        if (!text.startsWith(prefix)) return null;
        List<String> words = List.of(text.substring(prefix.length()).split("\\s+"));
        return new CmdRequest(words.getFirst(), words.subList(1, words.size()));
    }

    private static CmdRequest poke(String selfId, String targetId) {
        return selfId.equals(targetId) ? new CmdRequest("Poked", List.of()) : null;
    }

    private static CmdRequest recall() {
        return new CmdRequest("Recalled", List.of());
    }

    private static CmdRequest join() {
        return new CmdRequest("Joined", List.of());
    }

    private static CmdRequest left() {
        return new CmdRequest("Left", List.of());
    }
}
