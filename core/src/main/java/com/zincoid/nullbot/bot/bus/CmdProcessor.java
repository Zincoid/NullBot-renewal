package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.bus.cmd.CmdChain;
import com.zincoid.nullbot.bot.bus.cmd.CmdHandler;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdRegistry;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.GroupRecallEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateRecallEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.i18n.LanguageService;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 解析指令请求并运行处理器链。
 * <p>Resolves parsed command requests and runs them through the handler chain.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CmdProcessor {

    private final CmdRegistry registry;
    private final CmdParser parser;
    private final List<CmdHandler> handlers;
    private final LanguageService languageService;

    /**
     * 入站事件解析为指令时进行处理。
     * <p>Processes an inbound event when it parses as a command.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     */
    public void process(BotClient client, BotEvent event) {
        CmdRequest req = parser.parse(event);
        if (req != null) process(client, event, req);
    }

    /**
     * 按显式请求执行指令处理链。
     * <p>Runs the command handler chain for an explicit request.</p>
     *
     * @param client  the outbound port
     * @param event   the inbound event
     * @param req the explicit command request
     */
    public void process(BotClient client, BotEvent event, CmdRequest req) {
        Cmd cmd = registry.get(req.name());
        if (cmd == null) {
            log.info("Unknown command: {}", req.name());
            return;
        }
        CmdContext ctx = new CmdContext(client, event, cmd, req.params(), language(event));
        log.info("Command start: {}", req.name());
        new CmdChain(handlers).doHandle(ctx);
        log.info("Command done: {}", req.name());
    }

    // ════ Helpers ═════════════════

    private Language language(BotEvent event) {
        String groupId = switch (event) {
            case GroupMsgEvent e -> e.groupId();
            case GroupPokeEvent e -> e.groupId();
            case GroupRecallEvent e -> e.groupId();
            case JoinEvent e -> e.groupId();
            case LeaveEvent e -> e.groupId();
            case PrivateMsgEvent _, PrivatePokeEvent _, PrivateRecallEvent _ -> null;
        };
        return groupId == null
                ? languageService.defaultLanguage()
                : languageService.group(groupId);
    }
}
