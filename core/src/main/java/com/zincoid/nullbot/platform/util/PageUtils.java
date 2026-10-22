package com.zincoid.nullbot.platform.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页参数归一化工具：页码从 1 开始，页大小限制在 1..100。
 * <p>Pagination normalization: page numbers start at one and page sizes are clamped to 1..100.</p>
 */
public final class PageUtils {

    /**
     * 单页最大条数。
     * <p>Maximum page size.</p>
     */
    public static final long MAX_SIZE = 100;

    private PageUtils() {
    }

    /**
     * 把页码收敛到从 1 开始。
     * <p>Clamps the page number to start from one.</p>
     *
     * @param page the raw page number
     * @return the clamped page number
     */
    public static long page(long page) {
        return Math.max(1, page);
    }

    /**
     * 把页大小收敛到 1..{@link #MAX_SIZE}。
     * <p>Clamps the page size to 1..{@link #MAX_SIZE}.</p>
     *
     * @param size the raw page size
     * @return the clamped page size
     */
    public static long size(long size) {
        return Math.clamp(size, 1, MAX_SIZE);
    }

    /**
     * 创建归一化后的分页对象。
     * <p>Creates a normalized page object.</p>
     *
     * @param page the raw page number
     * @param size the raw page size
     * @param <T>  the row type
     * @return the normalized page
     */
    public static <T> Page<T> of(long page, long size) {
        return new Page<>(page(page), size(size));
    }
}
