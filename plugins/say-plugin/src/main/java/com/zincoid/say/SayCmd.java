package com.zincoid.say;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.Quote;
import com.zincoid.nullbot.platform.util.HttpUtils;
import com.zincoid.nullbot.platform.util.MimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

/**
 * 语录图片指令：随机抽一条语录，左侧头像渐隐，右侧渲染语录。
 * <p>Say command: draws a random quote with the author avatar fading on the left and the quote on the right.</p>
 */
@Slf4j
@CmdMapping({"Say", "说"})
@RequiredArgsConstructor
public class SayCmd implements Cmd {

    private static final int MAX_AVATAR_BYTES = 5 * 1024 * 1024;

    private final SayPlugin plugin;

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
        Quote quote = quote(ctx, content, args);
        String avatar = avatar(ctx, quote.getUserId());
        byte[] png;
        try {
            png = QuoteCard.render(plugin.context().renderer(), quote, avatar);
        } catch (RuntimeException e) {
            log.warn("Say image render failed: quote={}", quote.getId(), e);
            throw new CmdWarnException(SayText.RENDER_FAILED.of(ctx.language()), e);
        }
        ctx.send(MsgContent.builder().imageBase64(png).build());
        log.info("Say sent: quote={}", quote.getId());
    }

    // ════ Helpers ═════════════════

    private Quote quote(CmdContext ctx, MsgContent content, CmdArgs args) {
        if (args.hasOpt("id", "i")) {
            long id = quoteId(ctx, args);
            Quote quote = plugin.context().quotes().getById(id);
            if (quote == null) throw new CmdInfoException(ctx.render(Text.QUOTE_NOT_FOUND, id));
            return quote;
        }
        String userId = content.atIds().stream().findFirst().orElse(null);
        if (userId == null && args.hasNext()) userId = args.next();
        Quote quote = userId == null
                ? plugin.context().quotes().random()
                : plugin.context().quotes().random(userId);
        if (quote == null) throw new CmdInfoException(ctx.render(Text.NO_QUOTES));
        return quote;
    }

    private static long quoteId(CmdContext ctx, CmdArgs args) {
        String value = args.getOpt("id", "i", "");
        if ("true".equals(value)) value = args.next("");
        if (value.isBlank()) throw new CmdWarnException(ctx.render(Text.ARG_MISSING));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CmdWarnException(ctx.render(Text.ARG_MALFORMED), e);
        }
    }

    private static String avatar(CmdContext ctx, String userId) {
        if (userId == null || userId.isBlank()) return "";
        try {
            UserInfo info = ctx.client().getUserInfo(userId);
            if (info == null || info.avatar() == null || info.avatar().isBlank()) return "";
            byte[] bytes = HttpUtils.getBytes(info.avatar(), MAX_AVATAR_BYTES);
            String mime = MimeUtils.mime(bytes);
            if (mime == null || !mime.startsWith("image/")) return "";
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (RuntimeException e) {
            log.warn("Say avatar failed: author={}", userId, e);
            return "";
        }
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return SayText.HELP.of(language);
    }
}
