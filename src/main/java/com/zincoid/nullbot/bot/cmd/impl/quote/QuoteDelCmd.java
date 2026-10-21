package com.zincoid.nullbot.bot.cmd.impl.quote;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.module.common.CommonException;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 删除语录指令。
 * <p>Delete quote command.</p>
 */
@CmdMapping({"QuoteDel", "删除语录"})
@Component
@RequiredArgsConstructor
public class QuoteDelCmd implements Cmd {

    private final QuoteService quoteService;

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        long id = args.nextLong();
        try {
            quoteService.delete(id);
        } catch (CommonException e) {
            throw new CmdWarnException(e.getMessage());
        }
        ctx.send("⚠️ Quote No.%d deleted".formatted(id));
    }

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    @Override
    public String getHelp() {
        return """
                QuoteDel
                Deletes a quote.
                Usage: QuoteDel [quote id]
                Aliases: 删除语录
                Access: >=MANAGER""";
    }
}
