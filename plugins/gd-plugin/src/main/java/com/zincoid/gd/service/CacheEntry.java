package com.zincoid.gd.service;

import java.time.Duration;
import java.time.Instant;

/**
 * 带创建时间的缓存条目。
 * <p>A cache entry with its creation time.</p>
 *
 * @param value     the cached value
 * @param createdAt the creation time
 */
record CacheEntry<T>(T value, Instant createdAt) {

    CacheEntry(T value) {
        this(value, Instant.now());
    }

    boolean expired(Duration ttl) {
        return Instant.now().isAfter(createdAt.plus(ttl));
    }
}
