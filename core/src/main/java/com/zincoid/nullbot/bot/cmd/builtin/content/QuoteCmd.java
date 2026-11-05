package com.zincoid.nullbot.bot.cmd.builtin.content;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.Quote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 随机语录指令：随机发送一条语录。
 * <p>Random quote command: sends a random quote.</p>
 */
@Slf4j
@CmdMapping({"Quote", "quot", "语录"})
@Component
@RequiredArgsConstructor
public class QuoteCmd implements Cmd {

    private final QuoteService quoteService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, event.content(), args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, event.content(), args);
    }

    private void handle(CmdContext ctx, MsgContent content, CmdArgs args) {
        String userId = content == null ? null : content.atIds()
                .stream().findFirst().orElse(null);
        if (userId == null && args.hasNext())
            userId = args.next();
        Quote quote = userId == null
                ? quoteService.random()
                : quoteService.random(userId);
        if (quote == null)
            throw ctx.info(Text.NO_QUOTES);
        ctx.send(quoteService.format(quote));
        log.info("Quote sent: {}", quote.getId());
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.RESTRICTED;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Quote
                Sends a random quote.
                Usage: Quote [user id | @user]
                Aliases: quot/语录
                Access: >=-1(restricted)""" : """
                Quote
                随机发送一条语录。
                用法：Quote [user id | @user]
                别名：quot/语录
                权限：>=-1(restricted)""";
    }
}
