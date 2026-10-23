package com.zincoid.query.trace;

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
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 图片溯源指令：引用图片后调用 SauceNAO 查询出处。
 * <p>Image trace command: queries SauceNAO for the source of a replied image.</p>
 */
@Slf4j
@CmdMapping({"Trace", "图溯源", "溯源"})
public class TraceCmd implements Cmd {

    private final QueryPlugin plugin;

    public TraceCmd(QueryPlugin plugin) {
        this.plugin = plugin;
    }

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
            throw new CmdWarnException(text(ctx, "缺少图片引用", "Missing image reply"));
        if (plugin.config().sauceNaoApiKey().isBlank())
            throw new CmdWarnException(text(ctx,
                    "未配置 SauceNAO API Key", "SauceNAO API key is not configured"));
        int count = count(ctx, args);
        MsgDetail detail;
        try {
            detail = ctx.client().getMsg(replyId);
        } catch (RuntimeException e) {
            log.warn("Cannot read replied message: {}", replyId, e);
            throw new CmdWarnException(text(ctx, "无法读取引用消息", "Cannot read the replied message"));
        }
        MsgSegment.Image image = detail.content().segments().stream()
                .filter(MsgSegment.Image.class::isInstance)
                .map(MsgSegment.Image.class::cast)
                .findFirst()
                .orElse(null);
        if (image == null)
            throw new CmdWarnException(text(ctx, "引用中没有图片", "No image in the reply"));

        List<TraceService.TraceResult> results;
        try {
            results = plugin.trace().search(image.url(), count);
        } catch (QueryException e) {
            log.warn("Trace query failed", e);
            throw new CmdWarnException(text(ctx,
                    "图片溯源失败，请稍后重试",
                    "Image trace failed; try again later"));
        }
        if (results.isEmpty())
            throw new CmdInfoException(text(ctx, "未找到匹配结果", "No matches found"));

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
            throw new CmdWarnException(text(ctx,
                    "数量格式错误：%s", "Malformed count: %s").formatted(raw));
        }
    }

    private static String format(CmdContext ctx, TraceService.TraceResult result) {
        boolean en = ctx.language() == Language.EN;
        StringBuilder text = new StringBuilder()
                .append(en ? "Similarity: " : "相似度: ").append(result.similarity()).append('%')
                .append('\n').append(en ? "Source: " : "来源: ").append(result.source())
                .append('\n').append(en ? "Title: " : "标题: ").append(result.title())
                .append('\n').append(en ? "Author: " : "作者: ").append(result.author());
        if (result.url() != null && !result.url().isBlank())
            text.append('\n').append(en ? "Link: " : "链接: ").append(result.url());
        return text.toString();
    }

    private static String text(CmdContext ctx, String zh, String en) {
        return ctx.language() == Language.EN ? en : zh;
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Trace
                Finds the source of a replied image through SauceNAO.
                Usage: [reply image] Trace [count]
                Count: 1..5, default 1
                Aliases: 图溯源/溯源
                Access: >=0(normal)
                Note: set SAUCENAO_API_KEY or saucenao.api-key for higher quota.""" : """
                Trace
                通过 SauceNAO 查询引用图片的出处。
                用法：[引用图片] Trace [数量]
                数量：1~5，默认 1
                别名：图溯源/溯源
                权限：>=0(normal)
                说明：配置 SAUCENAO_API_KEY 或 saucenao.api-key 可提高额度。""";
    }
}
