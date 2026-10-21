package com.zincoid.nullbot.bot.cmd.impl.quote;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.platform.persistence.po.Quote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 保存语录指令。
 * <p>Save quote command.</p>
 */
@CmdMapping({"QuoteSave", "保存语录"})
@Component
@RequiredArgsConstructor
public class QuoteSaveCmd implements Cmd {

    private static final Pattern QUOTE_PATTERN = Pattern.compile(
            "^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}]\\[No\\.\\d+][\\s\\S]*");

    private final QuoteService quoteService;

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        boolean replied = args.hasOpt("id", "i");
        String messageId = replied
                ? args.getOpt("id", "i")
                : event.content().replyId();
        if (messageId == null)
            throw new CmdWarnException("Missing reply");
        if ("true".equals(messageId))
            throw new CmdWarnException("Malformed option: id");
        MsgDetail detail = ctx.client().getMsg(messageId);
        if (detail.content().hasRaw())
            throw new CmdWarnException("Unsupported content");
        String text = text(detail.content(), ctx.client());
        if (text.isBlank())
            throw new CmdWarnException("No text content");
        if (QUOTE_PATTERN.matcher(text).matches())
            throw new CmdWarnException("Quote already saved");
        Quote quote = quoteService.add(
                detail.userId(), detail.nickname(), text);
        MsgContent.Builder builder = MsgContent.builder()
                .text("💾 Quote saved: no." + quote.getId());
        if (replied) builder.reply(messageId);
        ctx.send(builder.build());
    }

    @Override
    public String getHelp() {
        return """
                QuoteSave
                Saves a quote.
                Usage: [reply] QuoteSave
                Options: -i,--id=[message ID]
                Aliases: 语录保存
                Access: >=NORMAL""";
    }

    @Override
    public String getAiHelp() {
        return """
                QuoteSave
                Saves a quote.
                Usage: QuoteSave [options]
                Options: -i,--id=[message ID]
                Note: saves funny or remarkable user messages.""";
    }

    private static String text(MsgContent content, BotClient client) {
        StringBuilder text = new StringBuilder();
        for (MsgSegment segment : content.segments()) {
            if (segment instanceof MsgSegment.Text(String s))
                text.append(s);
            else if (segment instanceof MsgSegment.At(String userId))
                text.append(at(userId, client));
        }
        return text.toString();
    }

    private static String at(String userId, BotClient client) {
        try {
            return "@" + client.getUserInfo(userId).nickname() + "(" + userId + ")";
        } catch (RuntimeException e) {
            return "@" + userId;
        }
    }
}
