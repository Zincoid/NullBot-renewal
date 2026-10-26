package com.zincoid.gd;

import java.util.List;

/**
 * 搜索结果视图：标题、页码、关卡摘要与选择等待时间。
 * <p>Search result view: title, page, level summaries, and the selection wait time.</p>
 *
 * @param query       the search keyword
 * @param page        the one-based page number
 * @param levels      the level summaries
 * @param waitSeconds the selection wait time in seconds
 */
record SearchView(String query, int page, List<LevelSummary> levels, long waitSeconds) {

    /**
     * 列表中的单条关卡摘要。
     * <p>A single level summary in the list.</p>
     *
     * @param id         the level id
     * @param name       the level name
     * @param difficulty the displayed difficulty
     * @param author     the level author
     * @param stars      the star reward
     */
    record LevelSummary(long id, String name, String difficulty, String author, int stars) {
    }
}
