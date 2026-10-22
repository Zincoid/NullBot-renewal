package com.zincoid.nullbot.bot.cmd.impl.quote;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdInfoException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.Quote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * 随机语录指令。
 * <p>Random quote command.</p>
 */
@Slf4j
@CmdMapping({"QuoteRand", "quote", "随机语录", "语录"})
@Component
@RequiredArgsConstructor
public class QuoteRandCmd implements Cmd {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        handle(ctx, null, args);
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        handle(ctx, null, args);
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
            throw new CmdInfoException(ctx.render(Text.NO_QUOTES));
        ctx.send(format(quote));
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
                QuoteRand
                Sends a random quote.
                Usage: QuoteRand [user id | @user]
                Aliases: quote/随机语录/语录
                Access: >=-1(restricted)""" : """
                QuoteRand
                随机发送一条语录。
                用法：QuoteRand [user id | @user]
                别名：quote/随机语录/语录
                权限：>=-1(restricted)""";
    }

    @Override
    public String getAiHelp() {
        return """
                QuoteRand
                Sends a random quote.
                Usage: QuoteRand [user id]""";
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
}
