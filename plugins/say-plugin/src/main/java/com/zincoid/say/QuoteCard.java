package com.zincoid.say;

import com.zincoid.nullbot.module.render.PluginRenderer;
import com.zincoid.nullbot.platform.persistence.po.Quote;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 语录卡片排版：换行、截断、emoji 分段与转义。
 * <p>Quote card layout: wrapping, truncation, emoji runs, and escaping.</p>
 */
public final class QuoteCard {

    private static final int MAX_UNITS = 40;
    private static final int MAX_LINES = 7;

    private QuoteCard() {
    }

    /**
     * 渲染语录卡片。
     * <p>Renders the quote card.</p>
     *
     * @param renderer the plugin renderer
     * @param quote    the quote to draw
     * @param avatar   the avatar data URI, or blank
     * @return the PNG bytes
     */
    static byte[] render(PluginRenderer renderer, Quote quote, String avatar) {
        String text = quote.getText().strip();
        if (!text.startsWith("“")) text = "“" + text;
        if (!text.endsWith("”")) text += "”";
        List<String> lines = wrap(text);
        if (lines.size() > MAX_LINES) {
            lines = new ArrayList<>(lines.subList(0, MAX_LINES));
            int last = lines.size() - 1;
            lines.set(last, truncated(lines.get(last)));
        }
        String author = quote.getNickname() == null ? "" : quote.getNickname();
        if (quote.getId() != null) author += " (" + quote.getUserId() + ")";
        return renderer.template("say.jte", Map.of(
                "avatar", avatar,
                "lines", lines.stream().map(QuoteCard::segments).toList(),
                "author", escape(author)
        ));
    }

    // ════ Text ════════════════════

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
        return value.codePoints().map(codePoint -> wide(codePoint) ? 2 : 1).sum();
    }

    private static boolean wide(int codePoint) {
        return Character.isIdeographic(codePoint)
                || (codePoint >= 0x2010 && codePoint <= 0x205E)
                || codePoint >= 0x2E00;
    }

    private static List<Segment> segments(String text) {
        List<Segment> segments = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            boolean emoji = emoji(text.codePointAt(i));
            int end = i + Character.charCount(text.codePointAt(i));
            while (end < text.length() && emoji(text.codePointAt(end)) == emoji) {
                end += Character.charCount(text.codePointAt(end));
            }
            segments.add(new Segment(escape(text.substring(i, end)), emoji));
            i = end;
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

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;");
    }

    /**
     * 语录卡片中的一段文本；emoji 段使用表情字体。
     * <p>A run of quote text; emoji runs use the emoji font.</p>
     *
     * @param text the escaped text
     * @param emoji whether the run is rendered with the emoji font
     */
    public record Segment(String text, boolean emoji) {
    }
}
