package com.zincoid.query.cmd;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.query.QueryException;
import com.zincoid.query.QueryPlugin;
import com.zincoid.query.QueryText;
import com.zincoid.query.service.TraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 图片溯源指令：引用图片后调用 SauceNAO 查询出处。
 * <p>Image trace command: queries SauceNAO for the source of a replied image.</p>
 */
@Slf4j
@CmdMapping({"Trace", "图溯源", "溯源"})
@RequiredArgsConstructor
public class TraceCmd implements Cmd {

    private final QueryPlugin plugin;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, event.content().replyId(), args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, event.content().replyId(), args);
    }

    private void handle(CmdContext ctx, String replyId, CmdArgs args) {
        if (replyId == null)
            throw new CmdWarnException(QueryText.TRACE_MISSING_IMAGE.of(ctx.language()));
        if (plugin.config().sauceNaoApiKey().isBlank())
            throw new CmdWarnException(QueryText.TRACE_NO_API_KEY.of(ctx.language()));
        int count = count(ctx, args);
        MsgDetail detail;
        try {
            detail = ctx.client().getMsg(replyId);
        } catch (RuntimeException e) {
            log.warn("Cannot read replied message: {}", replyId, e);
            throw new CmdWarnException(QueryText.TRACE_READ_FAILED.of(ctx.language()));
        }
        MsgSegment.Image image = detail.content().segments().stream()
                .filter(MsgSegment.Image.class::isInstance)
                .map(MsgSegment.Image.class::cast)
                .findFirst()
                .orElse(null);
        if (image == null)
            throw new CmdWarnException(QueryText.TRACE_NO_IMAGE.of(ctx.language()));

        List<TraceService.TraceResult> results;
        try {
            results = plugin.trace().search(image.url(), count);
        } catch (QueryException e) {
            log.warn("Trace query failed", e);
            throw new CmdWarnException(QueryText.TRACE_FAILED.of(ctx.language()));
        }
        if (results.isEmpty())
            throw new CmdInfoException(QueryText.TRACE_NO_MATCHES.of(ctx.language()));

        for (TraceService.TraceResult result : results) {
            MsgContent.Builder builder = MsgContent.builder().text(format(ctx, result));
            if (result.thumbnail() != null && !result.thumbnail().isBlank())
                builder.image(result.thumbnail());
            ctx.send(builder.build());
        }
        log.info("Trace sent: results={}", results.size());
    }

    // ════ Helpers ═════════════════

    private static int count(CmdContext ctx, CmdArgs args) {
        if (!args.hasNext()) return 1;
        String raw = args.next();
        try {
            return Math.clamp(Integer.parseInt(raw), 1, 5);
        } catch (NumberFormatException e) {
            throw new CmdWarnException(
                    QueryText.TRACE_MALFORMED_COUNT.of(ctx.language(), raw));
        }
    }

    private static String format(CmdContext ctx, TraceService.TraceResult result) {
        Language language = ctx.language();
        StringBuilder text = new StringBuilder()
                .append(QueryText.TRACE_SIMILARITY.of(language)).append(": ")
                .append(result.similarity()).append('%')
                .append('\n').append(QueryText.TRACE_SOURCE.of(language)).append(": ")
                .append(result.source())
                .append('\n').append(QueryText.TRACE_TITLE.of(language)).append(": ")
                .append(result.title())
                .append('\n').append(QueryText.TRACE_AUTHOR.of(language)).append(": ")
                .append(result.author());
        if (result.url() != null && !result.url().isBlank())
            text.append('\n').append(QueryText.TRACE_LINK.of(language)).append(": ")
                    .append(result.url());
        return text.toString();
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return QueryText.TRACE_HELP.of(language);
    }
}
