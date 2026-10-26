package com.zincoid.nullbot.module.render.svg.layout;

/**
 * SVG 表格列：表头、宽度与对齐方式。
 * <p>SVG table column: header, width, and alignment.</p>
 *
 * @param header the header text
 * @param width  the column width, including spacing
 * @param align  the horizontal alignment
 */
public record SvgColumn(String header, double width, SvgAlign align) {
}
