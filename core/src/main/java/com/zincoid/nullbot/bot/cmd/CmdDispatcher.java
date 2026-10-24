package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.bot.cmd.chain.CmdChain;
import com.zincoid.nullbot.bot.cmd.chain.CmdHandler;
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
 * 分发解析后的指令请求并运行处理器链。
 * <p>Dispatches parsed command requests and runs them through the handler chain.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CmdDispatcher {

    private final CmdRegistry registry;
    private final CmdParser parser;
    private final List<CmdHandler> handlers;
    private final LanguageService languageService;

    /**
     * 入站事件解析为指令时进行分发。
     * <p>Dispatches an inbound event when it parses as a command.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     */
    public void dispatch(BotClient client, BotEvent event) {
        CmdRequest req = parser.parse(event);
        if (req != null) dispatch(client, event, req);
    }

    /**
     * 按显式请求分发指令处理链，执行权限校验。
     * <p>Dispatches an explicit command request through the handler chain with access checks.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @param req    the explicit command request
     */
    public void dispatch(BotClient client, BotEvent event, CmdRequest req) {
        dispatch(client, event, req, true);
    }

    /**
     * 按显式请求分发指令处理链，可选择是否执行权限校验。
     * <p>Dispatches an explicit command request through the handler chain with optional access checks.</p>
     *
     * @param client      the outbound port
     * @param event       the inbound event
     * @param req         the explicit command request
     * @param checkAccess whether to run access checks
     */
    public void dispatch(BotClient client, BotEvent event, CmdRequest req, boolean checkAccess) {
        Cmd cmd = registry.get(req.name());
        if (cmd == null) {
            log.info("Unknown command: {}", req.name());
            return;
        }
        CmdContext ctx = new CmdContext(client, event, cmd, req.params(), language(event), checkAccess);
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
