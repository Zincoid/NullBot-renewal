package com.zincoid.gd.store;

/**
 * 一条本地进度记录。
 * <p>A local progress record.</p>
 *
 * @param levelId   the GD level id
 * @param progress  the completion percentage
 * @param levelName the level name snapshot
 * @param author    the level author snapshot, nullable
 * @param createdAt the creation time in epoch milliseconds
 * @param updatedAt the last update time in epoch milliseconds
 */
public record ProgressRecord(
        long levelId,
        int progress,
        String levelName,
        String author,
        long createdAt,
        long updatedAt
) {
}
