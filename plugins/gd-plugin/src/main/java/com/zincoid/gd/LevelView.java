package com.zincoid.gd;

/**
 * 关卡详情视图：游戏数据、GDDL 数据与缩略图。
 * <p>Level detail view: game data, GDDL data, and the thumbnail.</p>
 *
 * @param id            the level id
 * @param name          the level name
 * @param author        the level author
 * @param difficulty    the displayed difficulty
 * @param stars         the star reward
 * @param downloads     the download count
 * @param likes         the like count
 * @param length        the displayed level length
 * @param objects       the object count
 * @param song          the song description, nullable
 * @param description   the level description, nullable
 * @param tier          the GDDL tier, nullable
 * @param rating        the GDDL rating, nullable
 * @param enjoyment     the GDDL enjoyment, nullable
 * @param ratingCount   the GDDL rating count, nullable
 * @param aredlPosition the AREDL position, nullable
 * @param gddlFailed    whether the GDDL query failed
 * @param thumbnail     the thumbnail bytes, nullable
 */
record LevelView(
        long id,
        String name,
        String author,
        String difficulty,
        int stars,
        int downloads,
        int likes,
        String length,
        int objects,
        String song,
        String description,
        Integer tier,
        Double rating,
        Double enjoyment,
        Integer ratingCount,
        Integer aredlPosition,
        boolean gddlFailed,
        byte[] thumbnail
) {
}
