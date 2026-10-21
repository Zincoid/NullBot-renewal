package com.zincoid.nullbot.bot.cmd.impl.quote;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdInfoException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.platform.persistence.enums.Access;
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
@CmdMapping({"RandomQuote", "quote", "随机语录", "语录"})
@Component
@RequiredArgsConstructor
public class QuoteRandCmd implements Cmd {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final QuoteService quoteService;

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        String userId = event.content().atIds()
                .stream().findFirst().orElse(null);
        if (userId == null && args.hasNext())
            userId = args.next();
        Quote quote = userId == null
                ? quoteService.random()
                : quoteService.random(userId);
        if (quote == null)
            throw new CmdInfoException("No quotes");
        ctx.send(format(quote));
        log.info("Quote sent: {}", quote.getId());
    }

    @Override
    public Access getAccess() {
        return Access.RESTRICTED;
    }

    @Override
    public String getHelp() {
        return """
                RandomQuote
                Sends a random quote.
                Usage: RandomQuote [user id | @user]
                Aliases: say/随机语录/语录
                Access: >=RESTRICTED""";
    }

    @Override
    public String getAiHelp() {
        return """
                RandomQuote
                Sends a random quote.
                Usage: RandomQuote [user id | @user]""";
    }

    private static String format(Quote quote) {
        String time = quote.getTime() == null ? "" : quote.getTime().format(FORMATTER);
        return """
                [%s][No.%d]
                %s
                \t—— %s(%s)""".formatted(
                time, quote.getId(), quote.getText(), quote.getNickname(), quote.getUserId());
    }
}
