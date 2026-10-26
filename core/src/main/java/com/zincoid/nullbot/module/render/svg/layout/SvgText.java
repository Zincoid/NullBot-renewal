package com.zincoid.nullbot.module.render.svg.layout;

import com.zincoid.nullbot.module.render.FontRegistry;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.List;

/**
 * SVG 文本排版工具：字体宽度测量、按宽度截断、多行垂直居中与文本转义。
 * <p>SVG text layout utility: font measurements, width-based truncation, vertical line centering, and escaping.</p>
 */
public final class SvgText {

    private static final double LINE_ASCENT_RATIO = 0.9;
    private static final double LINE_DESCENT_RATIO = 0.25;
    private static final FontRenderContext FONT_CONTEXT = new FontRenderContext(null, true, true);

    private SvgText() {
    }

    /**
     * 测量文本宽度，使用默认渲染字体。
     * <p>Measures text width with the default render font.</p>
     *
     * @param text the text content
     * @param size the font size
     * @return the width in SVG units
     */
    public static double width(String text, float size) {
        return width(text, FontRegistry.JETBRAINS_MAPLE_MONO, size);
    }

    /**
     * 测量指定字体下的文本宽度。
     * <p>Measures text width with the given font family.</p>
     *
     * @param text   the text content
     * @param family the font family
     * @param size   the font size
     * @return the width in SVG units
     */
    public static double width(String text, String family, float size) {
        if (text == null || text.isEmpty()) return 0;
        Font font = new Font(family, Font.PLAIN, 1).deriveFont(size);
        return font.getStringBounds(text, FONT_CONTEXT).getWidth();
    }

    /**
     * 按宽度截断文本，使用默认渲染字体。
     * <p>Truncates text to a width with the default render font.</p>
     *
     * @param text     the text content
     * @param maxWidth the maximum width
     * @param size     the font size
     * @return the text with an ellipsis when truncated
     */
    public static String fit(String text, double maxWidth, float size) {
        return fit(text, maxWidth, FontRegistry.JETBRAINS_MAPLE_MONO, size);
    }

    /**
     * 按宽度截断文本。
     * <p>Truncates text to a width.</p>
     *
     * @param text     the text content
     * @param maxWidth the maximum width
     * @param family   the font family
     * @param size     the font size
     * @return the text with an ellipsis when truncated
     */
    public static String fit(String text, double maxWidth, String family, float size) {
        if (text == null || text.isEmpty()) return "";
        if (width(text, family, size) <= maxWidth) return text;
        int end = text.length();
        while (end > 0) {
            end = text.offsetByCodePoints(end, -1);
            String candidate = text.substring(0, end) + "…";
            if (width(candidate, family, size) <= maxWidth) return candidate;
        }
        return "…";
    }

    /**
     * 按行前间距排列文本，并使整块文本围绕指定 y 坐标垂直居中。
     * <p>Lays out lines by their gaps and vertically centers the block around the given y coordinate.</p>
     *
     * @param source  the lines
     * @param centerY the center y coordinate
     * @return the positioned lines
     */
    public static List<SvgPositionedLine> center(List<SvgLine> source, double centerY) {
        if (source == null || source.isEmpty()) return List.of();
        int[] offsets = new int[source.size()];
        for (int i = 1; i < source.size(); i++)
            offsets[i] = offsets[i - 1] + source.get(i).gapBefore();
        double ascent = source.getFirst().size() * LINE_ASCENT_RATIO;
        double descent = source.getLast().size() * LINE_DESCENT_RATIO;
        double top = centerY - (offsets[offsets.length - 1] + descent - ascent) / 2;
        List<SvgPositionedLine> result = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) {
            SvgLine line = source.get(i);
            result.add(new SvgPositionedLine(
                    line.text(), (int) Math.round(top + offsets[i]), line.size(), line.color()));
        }
        return result;
    }

    /**
     * 按普通字符和 emoji 拆分文本，并使用 core 的 Noto Emoji 字体族。
     * <p>Splits text into normal and emoji runs using the core Noto Emoji family.</p>
     *
     * @param text       the text content
     * @param textFamily the font family for non-emoji runs
     * @return the escaped font runs
     */
    public static List<Segment> fontSegments(String text, String textFamily) {
        return fontSegments(text, textFamily, FontRegistry.NOTO_EMOJI);
    }

    /**
     * 按普通字符和 emoji 拆分文本，并为每段指定字体族。
     * <p>Splits text into normal and emoji runs with explicit font families.</p>
     *
     * @param text        the text content
     * @param textFamily  the font family for non-emoji runs
     * @param emojiFamily the font family for emoji runs
     * @return the escaped font runs
     */
    public static List<Segment> fontSegments(String text, String textFamily, String emojiFamily) {
        if (text == null || text.isEmpty()) return List.of();
        List<Segment> segments = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int start = i;
            boolean emojiRun = isEmoji(text.codePointAt(i));
            i += Character.charCount(text.codePointAt(i));
            while (i < text.length() && isEmoji(text.codePointAt(i)) == emojiRun)
                i += Character.charCount(text.codePointAt(i));
            segments.add(new Segment(escape(text.substring(start, i)),
                    emojiRun ? emojiFamily : textFamily, emojiRun));
        }
        return segments;
    }

    /**
     * 转义文本中的 XML 特殊字符。
     * <p>Escapes XML special characters in text.</p>
     *
     * @param value the text
     * @return the escaped text, or an empty string for null
     */
    public static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static boolean isEmoji(int codePoint) {
        return codePoint == 0x200D
                || codePoint == 0xFE0F
                || (codePoint >= 0x1F000 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || (codePoint >= 0x2B00 && codePoint <= 0x2BFF)
                || codePoint == 0x20E3
                || (codePoint >= 0xE0020 && codePoint <= 0xE007F);
    }

    /**
     * 一段带字体的文本。
     * <p>A text segment with its font family.</p>
     *
     * @param text   the escaped text
     * @param family the font family
     * @param emoji  whether this segment contains emoji
     */
    public record Segment(String text, String family, boolean emoji) {
    }
}
