package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.bot.BotRegistrar;
import com.zincoid.nullbot.bot.cmd.handler.CmdChain;
import com.zincoid.nullbot.bot.cmd.handler.CmdHandler;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.i18n.LanguageService;
import com.zincoid.nullbot.module.i18n.Text;
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
    private final BotRegistrar botRegistrar;
    private final GroupService groupService;
    private final UserService userService;

    // ════ Dispatch ════════════════

    /**
     * 入站消息解析为指令时进行分发。
     * <p>Dispatches an inbound message when it parses as a command.</p>
     *
     * @param client the outbound port
     * @param event  the inbound message event
     */
    public void dispatch(BotClient client, MsgEvent event) {
        CmdRequest req = parser.parse(event);
        if (req != null) dispatch(client, event, req, true);
    }

    /**
     * 按显式请求分发指令处理链，可选择是否执行权限校验。
     * <p>Dispatches an explicit command request through the handler chain with optional access checks.</p>
     *
     * @param client      the outbound port
     * @param event       the inbound message event
     * @param req         the explicit command request
     * @param checkAccess whether to run access checks
     */
    public void dispatch(BotClient client, MsgEvent event, CmdRequest req, boolean checkAccess) {
        Cmd cmd = registry.get(req.name());
        if (cmd == null) {
            log.info("Unknown command: {}", req.name());
            return;
        }
        botRegistrar.ensure(client, event);
        if (!enabled(event)) {
            log.info("Command disabled: {}", req.name());
            return;
        }
        CmdContext ctx = new CmdContext(client, event, cmd, req.params(), language(event), checkAccess);
        log.info("Command start: {}", req.name());
        try {
            new CmdChain(handlers).doHandle(ctx);
        } catch (CmdInfoException e) {
            log.info("Cmd info: {}", e.getMessage());
            ctx.send(e.getMessage());
        } catch (CmdWarnException e) {
            log.warn("Cmd warning: {}", e.getMessage());
            ctx.send(Text.CMD_WARN, e.getMessage());
        } catch (Exception e) {
            log.error("Cmd fatal: {}", e.getMessage(), e);
            ctx.send(Text.CMD_ERROR, e.getMessage());
        }
        log.info("Command done: {}", req.name());
    }

    // ════ Helpers ═════════════════

    private boolean enabled(MsgEvent event) {
        return event instanceof GroupEvent e
                ? groupService.isCmdEnabled(e.groupId())
                : userService.isPrivateCmdEnabled(event.userId());
    }

    private Language language(MsgEvent event) {
        return event instanceof GroupEvent e
                ? languageService.group(e.groupId())
                : languageService.defaultLanguage();
    }
}
