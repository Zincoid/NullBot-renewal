package com.zincoid.gd.model;

import java.util.List;

/**
 * 本地 B20 视图：本地用户、统计与最佳记录。
 * <p>Local B20 view: the local user, stats, and best records.</p>
 *
 * @param userId       the platform user id
 * @param userName     the platform user name
 * @param avatar       the avatar image bytes, nullable
 * @param beatenTotal  the total beaten records
 * @param beatingTotal the total in-progress records
 * @param ratedTotal   the records with a GDDL tier
 * @param unratedTotal the records without a GDDL tier
 * @param score        the B20 score
 * @param highestTier  the highest tier in the B20
 * @param sample       the number of records averaged
 * @param top          the records in the B20
 * @param limit        the number of records to display
 */
public record LocalB20View(
        String userId,
        String userName,
        byte[] avatar,
        int beatenTotal,
        int beatingTotal,
        int ratedTotal,
        int unratedTotal,
        double score,
        int highestTier,
        int sample,
        List<Play> top,
        int limit
) {

    /**
     * 本地 B20 中的单条记录。
     * <p>A single record in the local B20.</p>
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
