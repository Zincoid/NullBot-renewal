package com.zincoid.gd.service;

import java.time.Duration;
import java.time.Instant;

/**
 * GD 缓存条目，带创建时间。
 * <p>GD cache entry with its creation time.</p>
 *
 * @param value     the cached value
 * @param createdAt the creation time
 */
record GdCacheEntry<T>(T value, Instant createdAt) {

    GdCacheEntry(T value) {
        this(value, Instant.now());
    }

    boolean expired(Duration ttl) {
        return Instant.now().isAfter(createdAt.plus(ttl));
    }
}
