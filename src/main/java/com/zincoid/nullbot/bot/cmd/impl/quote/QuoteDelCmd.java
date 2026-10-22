package com.zincoid.nullbot.bot.cmd.impl.quote;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.module.common.CommonException;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
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

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        long id = args.nextLong();
        try {
            quoteService.delete(id);
        } catch (CommonException e) {
            throw new CmdWarnException(e.getMessage());
        }
        ctx.send(Text.QUOTE_DELETED, id);
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                QuoteDel
                Deletes a quote.
                Usage: QuoteDel [quote id]
                Aliases: 删除语录
                Access: >=1(manager)""" : """
                QuoteDel
                删除一条语录。
                用法：QuoteDel [quote id]
                别名：删除语录
                权限：>=1(manager)""";
    }
}
