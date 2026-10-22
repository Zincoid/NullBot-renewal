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
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Language;
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

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, event.content().replyId(), args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, event.content().replyId(), args);
    }

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        handle(ctx, null, args);
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        handle(ctx, null, args);
    }

    private void handle(CmdContext ctx, String replyId, CmdArgs args) {
        boolean replied = args.hasOpt("id", "i");
        String messageId = replied
                ? args.getOpt("id", "i")
                : replyId;
        if (messageId == null)
            throw new CmdWarnException(ctx.render(Text.MISSING_REPLY));
        if ("true".equals(messageId))
            throw new CmdWarnException(ctx.render(Text.MALFORMED_OPTION_ID));
        MsgDetail detail = ctx.client().getMsg(messageId);
        if (detail.content().hasRaw())
            throw new CmdWarnException(ctx.render(Text.UNSUPPORTED_CONTENT));
        String text = text(detail.content(), ctx.client());
        if (text.isBlank())
            throw new CmdWarnException(ctx.render(Text.NO_TEXT_CONTENT));
        if (QUOTE_PATTERN.matcher(text).matches())
            throw new CmdWarnException(ctx.render(Text.QUOTE_ALREADY_SAVED));
        Quote quote = quoteService.add(
                detail.userId(), detail.nickname(), text);
        MsgContent.Builder builder = MsgContent.builder()
                .text(ctx.render(Text.QUOTE_SAVED, quote.getId()));
        if (replied) builder.reply(messageId);
        ctx.send(builder.build());
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                QuoteSave
                Saves a quote.
                Usage: [reply] QuoteSave
                Aliases: 保存语录
                Access: >=0(normal)""" : """
                QuoteSave
                保存引用消息的文本为语录。
                用法：[回复] QuoteSave
                别名：保存语录
                权限：>=0(normal)""";
    }

    @Override
    public String getAiHelp() {
        return """
                QuoteSave
                Saves the text of one message as a quote.
                Usage: QuoteSave -i=<message id>
                Note: the id is the [id=...] marker shown before messages in the conversation.""";
    }

    // ════ Helpers ═════════════════

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
