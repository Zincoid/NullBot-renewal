package com.zincoid.nullbot.module.render.svg.layout;

/**
 * 已定位的 SVG 表格单元格。
 * <p>A positioned SVG table cell.</p>
 *
 * @param text   the escaped cell text
 * @param x      the anchor x coordinate
 * @param y      the baseline y coordinate
 * @param size   the font size
 * @param color  the fill color
 * @param anchor the SVG text anchor
 */
public record SvgCell(String text, double x, int y, int size, String color, String anchor) {
}
