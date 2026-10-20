package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.bus.cmd.CmdChain;
import com.zincoid.nullbot.bot.bus.cmd.CmdHandler;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdRegistry;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves parsed command requests and runs them through the handler chain.
 * <p>解析指令请求并运行处理器链。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CmdProcessor {

    private final CmdRegistry registry;
    private final CmdParser parser;
    private final List<CmdHandler> handlers;

    /**
     * Processes an inbound event when it parses as a command.
     * <p>入站事件解析为指令时进行处理。</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     */
    public void process(BotClient client, BotEvent event) {
        CmdRequest request = parser.parse(event);
        if (request == null) return;
        Cmd cmd = registry.get(request.name());
        if (cmd == null) {
            log.info("Unknown command: {}", request.name());
            return;
        }
        CmdContext ctx = new CmdContext(client, event, cmd, request.params());
        log.info("Command start: {}", request.name());
        new CmdChain(handlers).doHandle(ctx);
        log.info("Command done: {}", request.name());
    }
}
