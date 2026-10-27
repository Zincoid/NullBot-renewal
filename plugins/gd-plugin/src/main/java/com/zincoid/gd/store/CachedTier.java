package com.zincoid.gd.store;

/**
 * 缓存的 GDDL 关卡评级。
 * <p>A cached GDDL level rating.</p>
 *
 * @param levelId   the GD level id
 * @param rating    the raw GDDL rating, nullable when unrated
 * @param tier      the rounded tier, nullable when unrated
 * @param fetchedAt the fetch time in epoch milliseconds
 */
public record CachedTier(long levelId, Double rating, Integer tier, long fetchedAt) {
}
