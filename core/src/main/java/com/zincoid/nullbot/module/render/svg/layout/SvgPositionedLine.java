package com.zincoid.nullbot.module.render.svg.layout;

/**
 * 已计算 y 坐标的 SVG 文本行。
 * <p>An SVG text line with its computed y coordinate.</p>
 *
 * @param text  the text content
 * @param y     the baseline y coordinate
 * @param size  the font size
 * @param color the fill color
 */
public record SvgPositionedLine(String text, int y, int size, String color) {
}
