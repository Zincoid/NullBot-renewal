package com.zincoid.nullbot.bot.cmd.impl.quote;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.Quote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 获取语录指令：按语录 ID 发送指定语录。
 * <p>Get quote command: sends the quote with the given id.</p>
 */
@Slf4j
@CmdMapping({"QuoteGet", "SayGet", "获取语录"})
@Component
@RequiredArgsConstructor
public class QuoteGetCmd implements Cmd {

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

    private void handle(CmdContext ctx, CmdArgs args) {
        long id = args.nextLong();
        Quote quote = quoteService.getById(id);
        if (quote == null)
            throw new CmdWarnException(ctx.render(Text.QUOTE_NOT_FOUND, id));
        ctx.send(QuoteText.format(quote));
        log.info("Quote sent: {}", id);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                QuoteGet
                Sends a quote by id.
                Usage: QuoteGet [quote id]
                Aliases: SayGet/获取语录
                Access: >=0(normal)""" : """
                QuoteGet
                按语录 id 发送语录。
                用法：QuoteGet [quote id]
                别名：SayGet/获取语录
                权限：>=0(normal)""";
    }
}
