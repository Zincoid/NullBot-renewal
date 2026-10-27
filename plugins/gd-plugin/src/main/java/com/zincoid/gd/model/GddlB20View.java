package com.zincoid.gd.model;

import java.util.List;

/**
 * GDDL B20 排行视图：GDDL 用户信息、分数与最佳记录。
 * <p>GDDL B20 rank view: GDDL user information, score, and best records.</p>
 *
 * @param userId       the GDDL user id
 * @param userName     the GDDL user name
 * @param beatenTotal  the total beaten records
 * @param beatingTotal the total in-progress records
 * @param score        the B20 score
 * @param highestTier  the highest tier in the B20
 * @param sample       the number of records averaged
 * @param top          the records in the B20
 * @param limit        the number of records to display
 * @param avatar       the GDDL avatar image bytes, nullable
 */
public record GddlB20View(
        long userId,
        String userName,
        int beatenTotal,
        int beatingTotal,
        double score,
        int highestTier,
        int sample,
        List<Play> top,
        int limit,
        byte[] avatar
) {

    /**
     * GDDL B20 中的单条记录。
     * <p>A single record in the GDDL B20.</p>
     *
     * @param levelId  the level id
     * @param name     the level name
     * @param tier     the GDDL tier
     * @param progress the completion percentage
     * @param rt       the computed rt
     */
    public record Play(long levelId, String name, int tier, int progress, double rt) {
    }
}
