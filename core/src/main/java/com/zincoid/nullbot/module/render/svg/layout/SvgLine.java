package com.zincoid.nullbot.module.render.svg.layout;

/**
 * SVG 文本行：文本、行前间距、字号与颜色。
 * <p>SVG text line: text, gap before the line, font size, and color.</p>
 *
 * @param text       the text content
 * @param gapBefore  the gap before this line, in SVG units
 * @param size       the font size
 * @param color      the fill color
 */
public record SvgLine(String text, int gapBefore, int size, String color) {
}
