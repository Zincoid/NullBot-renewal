package com.zincoid.nullbot.module.render.svg.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * SVG 表格排版：计算列位置、表头与数据行，并按列宽截断文本。
 * <p>SVG table layout: computes column positions and header/data cells, fitting text to column widths.</p>
 */
public final class SvgTable {

    private static final double CELL_GAP = 8;

    private SvgTable() {
    }

    /**
     * 构建表格单元格。
     * <p>Builds table cells.</p>
     *
     * @param x           the left x coordinate
     * @param headerY     the header baseline y coordinate
     * @param rowY        the first data row baseline y coordinate
     * @param rowGap      the baseline gap between data rows
     * @param columns     the columns
     * @param rows        the data rows
     * @param headerSize  the header font size
     * @param rowSize     the data font size
     * @param headerColor the header fill color
     * @param rowColor    the data fill color
     * @return the positioned cells
     */
    public static List<SvgCell> build(double x, int headerY, int rowY, int rowGap,
                                      List<SvgColumn> columns, List<List<String>> rows,
                                      int headerSize, int rowSize,
                                      String headerColor, String rowColor) {
        if (columns == null || columns.isEmpty()) return List.of();
        double[] starts = new double[columns.size()];
        double cursor = x;
        for (int i = 0; i < columns.size(); i++) {
            starts[i] = cursor;
            cursor += columns.get(i).width();
        }

        List<SvgCell> cells = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            SvgColumn column = columns.get(i);
            add(cells, column.header(), starts[i], column, headerY, headerSize, headerColor);
        }
        if (rows == null) return cells;
        for (int row = 0; row < rows.size(); row++) {
            List<String> values = rows.get(row);
            int y = rowY + row * rowGap;
            for (int i = 0; i < columns.size(); i++) {
                String value = values != null && i < values.size() ? values.get(i) : "";
                add(cells, value, starts[i], columns.get(i), y, rowSize, rowColor);
            }
        }
        return cells;
    }

    // ════ Helpers ═════════════════

    private static void add(List<SvgCell> cells, String text, double start, SvgColumn column,
                            int y, int size, String color) {
        if (text == null || text.isEmpty()) return;
        String fitted = SvgText.fit(text, Math.max(0, column.width() - CELL_GAP), size);
        double x = switch (column.align()) {
            case MIDDLE -> start + column.width() / 2;
            case END -> start + column.width();
            default -> start;
        };
        String anchor = switch (column.align()) {
            case MIDDLE -> "middle";
            case END -> "end";
            default -> "start";
        };
        cells.add(new SvgCell(SvgText.escape(fitted), x, y, size, color, anchor));
    }
}
