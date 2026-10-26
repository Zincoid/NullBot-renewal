package com.zincoid.gd;

import com.zincoid.nullbot.module.render.FontRegistry;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GD SVG 文本排版：英文使用 JetBrains Mono，中文使用 Noto Sans Mono CJK SC。
 * <p>GD SVG text layout: English uses JetBrains Mono and Chinese uses Noto Sans Mono CJK SC.</p>
 */
public final class GdSvgText {

    private static final String LATIN_FAMILY = "JetBrains Mono";
    private static final String CJK_FAMILY = FontRegistry.NOTO_SANS_MONO_CJK_SC;
    private static final Font LATIN = new Font(LATIN_FAMILY, Font.PLAIN, 1);
    private static final Font CJK = new Font(CJK_FAMILY, Font.PLAIN, 1);
    private static final FontRenderContext FONT_CONTEXT = new FontRenderContext(null, true, false);

    private GdSvgText() {
    }

    /**
     * 一行文本及其样式。
     * <p>A line of text with its style.</p>
     *
     * @param text  the text content
     * @param y     the baseline y coordinate
     * @param size  the font size
     * @param color the fill color
     */
    public record Line(String text, int y, int size, String color) {
    }

    /**
     * 渲染一行带样式的文本。
     * <p>Renders one styled line of text.</p>
     *
     * @param line the styled line
     * @param x    the left x coordinate
     * @return the SVG text elements
     */
    public static String render(Line line, int x) {
        return render(line.text(), x, line.y(), line.size(), line.color(), null);
    }

    /**
     * 按字符所属字体拆分并渲染文本。
     * <p>Splits the text by font coverage and renders SVG text elements.</p>
     *
     * @param text   the text content
     * @param x      the anchor x coordinate
     * @param y      the baseline y coordinate
     * @param size   the font size
     * @param color  the fill color
     * @param anchor the text anchor, or null for start
     * @return the SVG text elements
     */
    public static String render(String text, int x, int y, int size, String color, String anchor) {
        if (text == null || text.isEmpty()) return "";
        List<Run> runs = split(text);
        double total = 0;
        for (Run run : runs) total += width(run, size);
        double cursor = x;
        if ("middle".equals(anchor)) cursor -= total / 2;
        else if ("end".equals(anchor)) cursor -= total;
        StringBuilder svg = new StringBuilder();
        for (Run run : runs) {
            svg.append("<text x=\"").append(format(cursor))
                    .append("\" y=\"").append(y)
                    .append("\" font-family=\"").append(run.latin() ? LATIN_FAMILY : CJK_FAMILY)
                    .append("\" font-size=\"").append(size)
                    .append("\" fill=\"").append(color).append("\">")
                    .append(escape(run.text()))
                    .append("</text>");
            cursor += width(run, size);
        }
        return svg.toString();
    }

    // ════ Helpers ═════════════════

    private static List<Run> split(String text) {
        List<Run> runs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Boolean latin = null;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            boolean nextLatin = LATIN.canDisplay(codePoint);
            if (latin == null) {
                latin = nextLatin;
            } else if (latin != nextLatin) {
                runs.add(new Run(current.toString(), latin));
                current.setLength(0);
                latin = nextLatin;
            }
            current.appendCodePoint(codePoint);
            i += Character.charCount(codePoint);
        }
        if (!current.isEmpty()) runs.add(new Run(current.toString(), latin));
        return runs;
    }

    private static double width(Run run, int size) {
        return (run.latin() ? LATIN : CJK).deriveFont((float) size)
                .getStringBounds(run.text(), FONT_CONTEXT).getWidth();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record Run(String text, boolean latin) {
    }
}
