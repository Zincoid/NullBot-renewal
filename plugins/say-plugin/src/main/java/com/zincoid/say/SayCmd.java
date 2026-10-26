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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
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
    private static final int MAX_UNITS = 40;
    private static final int MAX_LINES = 7;
    private static final int QUOTE_X = 500;
    private static final int AUTHOR_X = 1240;
    private static final int QUOTE_SIZE = 34;
    private static final int AUTHOR_SIZE = 30;
    private static final String QUOTE_FONT = "Noto Serif CJK SC";
    private static final String EMOJI_FONT = "Noto Emoji";
    private static final FontRenderContext FONT_CONTEXT = fontContext();

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
        String userId = content.atIds().stream().findFirst().orElse(null);
        if (userId == null && args.hasNext()) userId = args.next();
        Quote quote = userId == null
                ? plugin.context().quotes().random()
                : plugin.context().quotes().random(userId);
        if (quote == null) throw new CmdInfoException(ctx.render(Text.NO_QUOTES));

        String avatar = avatar(ctx, quote.getUserId());
        String text = quote.getText().strip();
        if (!text.startsWith("“")) text = "“" + text;
        if (!text.endsWith("”")) text += "”";
        List<String> lines = wrap(text);
        if (lines.size() > MAX_LINES) {
            lines = new ArrayList<>(lines.subList(0, MAX_LINES));
            int last = lines.size() - 1;
            lines.set(last, truncated(lines.get(last)));
        }
        Font quoteFont = new Font(QUOTE_FONT, Font.ITALIC, QUOTE_SIZE);
        Font quoteEmojiFont = new Font(EMOJI_FONT, Font.PLAIN, QUOTE_SIZE);
        List<List<Segment>> rendered = lines.stream()
                .map(line -> segments(line, QUOTE_X, quoteFont, quoteEmojiFont, false))
                .toList();

        String author = quote.getNickname() == null ? "" : quote.getNickname();
        if (quote.getId() != null) author += " (" + quote.getUserId() + ")";
        Font authorFont = new Font(QUOTE_FONT, Font.BOLD | Font.ITALIC, AUTHOR_SIZE);
        Font authorEmojiFont = new Font(EMOJI_FONT, Font.PLAIN, AUTHOR_SIZE);
        List<Segment> authorLine = segments("⸺ " + author, AUTHOR_X, authorFont, authorEmojiFont, true);
        byte[] png;
        try {
            png = plugin.context().renderer().template("say.jte", Map.of(
                    "avatar", avatar,
                    "lines", rendered,
                    "author", authorLine
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

    private static String truncated(String line) {
        String suffix = "…”";
        if (line.endsWith("”")) line = line.substring(0, line.length() - 1);
        while (!line.isEmpty() && units(line) + units(suffix) > MAX_UNITS) {
            line = line.substring(0, line.offsetByCodePoints(line.length(), -1));
        }
        return line + suffix;
    }

    private static int units(String value) {
        int units = 0;
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            units += wide(codePoint) ? 2 : 1;
            i += Character.charCount(codePoint);
        }
        return units;
    }

    private static boolean wide(int codePoint) {
        return Character.isIdeographic(codePoint)
                || codePoint >= 0x2E80
                || (codePoint >= 0x2010 && codePoint <= 0x205E)
                || (codePoint >= 0x2E00 && codePoint <= 0x2E7F);
    }

    private static List<Segment> segments(String text, float x, Font font, Font emojiFont, boolean right) {
        List<Segment> pieces = new ArrayList<>();
        float width = 0;
        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            boolean isEmoji = emoji(codePoint);
            int end = i + Character.charCount(codePoint);
            while (end < text.length() && emoji(text.codePointAt(end)) == isEmoji) {
                end += Character.charCount(text.codePointAt(end));
            }
            String piece = text.substring(i, end);
            pieces.add(new Segment(escape(piece), width, isEmoji));
            width += (float) (isEmoji ? emojiFont : font).getStringBounds(piece, FONT_CONTEXT).getWidth();
            i = end;
        }
        float start = right ? x - width : x;
        List<Segment> segments = new ArrayList<>(pieces.size());
        for (Segment piece : pieces) {
            segments.add(new Segment(piece.text(), start + piece.x(), piece.emoji()));
        }
        return segments;
    }

    private static boolean emoji(int codePoint) {
        return codePoint == 0x200D
                || codePoint == 0xFE0F
                || (codePoint >= 0x1F000 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || (codePoint >= 0x2B00 && codePoint <= 0x2BFF)
                || codePoint == 0x20E3
                || (codePoint >= 0xE0020 && codePoint <= 0xE007F);
    }

    private static FontRenderContext fontContext() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        FontRenderContext context = graphics.getFontRenderContext();
        graphics.dispose();
        return context;
    }

    /**
     * 语录卡片中的一段文本；emoji 段使用表情字体单独定位。
     * <p>A run of quote text; emoji runs use the emoji font and an explicit position.</p>
     *
     * @param text the escaped text
     * @param x the x coordinate
     * @param emoji whether the run is rendered with the emoji font
     */
    public record Segment(String text, float x, boolean emoji) {

        public String family() {
            return emoji ? EMOJI_FONT : QUOTE_FONT;
        }

        public String style() {
            return emoji ? "normal" : "italic";
        }

        public String weight() {
            return emoji ? "normal" : "bold";
        }

        public String stroke() {
            return emoji ? "none" : "#8FA0B8";
        }
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
