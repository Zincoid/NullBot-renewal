package com.zincoid.nullbot.bot.cmd.impl.quote;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.platform.persistence.po.Quote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * 获取语录指令：按语录 ID 发送指定语录。
 * <p>Get quote command: sends the quote with the given id.</p>
 */
@Slf4j
@CmdMapping({"QuoteGet", "获取语录"})
@Component
@RequiredArgsConstructor
public class QuoteGetCmd implements Cmd {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final QuoteService quoteService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    private void handle(CmdContext ctx, CmdArgs args) {
        long id = args.nextLong();
        Quote quote = quoteService.getById(id);
        if (quote == null)
            throw new CmdWarnException("Quote not found: " + id);
        ctx.send(format(quote));
        log.info("Quote sent: {}", id);
    }

    // ════ Helpers ═════════════════

    private static String format(Quote quote) {
        String time = quote.getTime() == null ? "" : quote.getTime().format(FORMATTER);
        return """
                [%s][No.%d]
                %s
                \t—— %s(%s)""".formatted(
                time, quote.getId(), quote.getText(), quote.getNickname(), quote.getUserId());
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp() {
        return """
                QuoteGet
                Sends a quote by id.
                Usage: QuoteGet [quote id]
                Aliases: 获取语录
                Access: >=0(normal)""";
    }

    @Override
    public String getAiHelp() {
        return """
                QuoteGet
                Sends a quote by id.
                Usage: QuoteGet [quote id]""";
    }
}
