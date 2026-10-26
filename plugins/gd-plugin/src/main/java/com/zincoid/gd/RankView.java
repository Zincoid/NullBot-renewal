package com.zincoid.gd;

import java.util.List;

/**
 * B20 排行视图：用户信息、分数与最佳记录。
 * <p>B20 rank view: user information, score, and best records.</p>
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
 */
record RankView(
        long userId,
        String userName,
        int beatenTotal,
        int beatingTotal,
        double score,
        int highestTier,
        int sample,
        List<Play> top,
        int limit
) {

    /**
     * B20 中的单条记录。
     * <p>A single record in the B20.</p>
     *
     * @param levelId  the level id
     * @param name     the level name
     * @param tier     the GDDL tier
     * @param progress the completion percentage
     * @param rt       the computed rt
     */
    record Play(long levelId, String name, int tier, int progress, double rt) {
    }
}
