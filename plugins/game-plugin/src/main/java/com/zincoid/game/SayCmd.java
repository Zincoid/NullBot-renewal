package com.zincoid.game;

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

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 语录图片指令：随机抽一条语录，左侧头像渐隐，右侧渲染语录。
 * <p>Say command: draws a random quote with the author avatar fading on the left and the quote on the right.</p>
 */
@Slf4j
@CmdMapping({"Say", "说"})
@RequiredArgsConstructor
public class SayCmd implements Cmd {

    private static final int MAX_AVATAR_BYTES = 5 * 1024 * 1024;
    private static final int MAX_UNITS = 46;
    private static final int MAX_LINES = 7;

    private final GamePlugin plugin;

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
        String userId = content.atIds().stream().findFirst().orElse(null);
        if (userId == null && args.hasNext()) userId = args.next();
        Quote quote = userId == null
                ? plugin.context().quotes().random()
                : plugin.context().quotes().random(userId);
        if (quote == null) throw new CmdInfoException(ctx.render(Text.NO_QUOTES));

        String avatar = avatar(ctx, quote.getUserId());
        List<String> lines = wrap(quote.getText());
        if (lines.size() > MAX_LINES) {
            lines = new ArrayList<>(lines.subList(0, MAX_LINES));
            int last = lines.size() - 1;
            lines.set(last, lines.get(last) + "…");
        }
        lines = new ArrayList<>(lines.stream().map(SayCmd::escape).toList());
        if (!lines.getFirst().startsWith("“")) lines.set(0, "“" + lines.getFirst());
        if (!lines.getLast().endsWith("”")) lines.set(lines.size() - 1, lines.getLast() + "”");

        String author = escape(quote.getNickname() == null ? "" : quote.getNickname());
        if (quote.getId() != null) author += " (" + quote.getUserId() + ")";
        byte[] png;
        try {
            png = plugin.context().renderer().template("say.jte", Map.of(
                    "avatar", avatar,
                    "lines", lines,
                    "author", author
            ));
        } catch (RuntimeException e) {
            log.warn("Say image render failed: quote={}", quote.getId(), e);
            throw new CmdWarnException(ctx.language() == Language.EN
                    ? "Failed to render the quote image"
                    : "语录图片生成失败", e);
        }
        ctx.send(MsgContent.builder().imageBase64(png).build());
        log.info("Say sent: quote={}", quote.getId());
    }

    // ════ Helpers ═════════════════

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

    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.strip().split("\\R")) {
            String value = paragraph.strip();
            if (value.isEmpty()) {
                if (!lines.isEmpty()) lines.add("");
                continue;
            }
            BreakIterator iterator = BreakIterator.getLineInstance(Locale.ROOT);
            iterator.setText(value);
            StringBuilder line = new StringBuilder();
            int units = 0;
            int start = iterator.first();
            for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                String chunk = value.substring(start, end);
                int width = units(chunk);
                if (units + width > MAX_UNITS && !line.isEmpty()) {
                    lines.add(line.toString().stripTrailing());
                    line.setLength(0);
                    units = 0;
                }
                line.append(chunk);
                units += width;
            }
            if (!line.isEmpty()) lines.add(line.toString().stripTrailing());
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private static int units(String value) {
        int units = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            units += Character.isIdeographic(codePoint) || codePoint >= 0x2E80 ? 2 : 1;
            i += Character.charCount(codePoint);
        }
        return units;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Say
                Renders a random quote with the author avatar.
                Usage: Say [user id | @user]
                Aliases: 说
                Access: >=0(normal)""" : """
                Say
                随机渲染一条语录卡片，左侧为作者头像渐隐。
                用法：Say [用户ID | @用户]
                别名：说
                权限：>=0(normal)""";
    }
}
