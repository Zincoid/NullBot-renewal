package com.zincoid.nullbot.console.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

/**
 * 控制台分页视图。
 * <p>Console page view.</p>
 *
 * @param items the page items
 * @param total the total item count
 * @param page  the current page number
 * @param size  the page size
 * @param <T>   the item type
 */
public record PageView<T>(List<T> items, long total, long page, long size) {

    /**
     * 由 MyBatis-Plus 分页结果构建视图。
     * <p>Builds the view from a MyBatis-Plus page.</p>
     *
     * @param page   the source page
     * @param mapper the item mapper
     * @param <E>    the source item type
     * @param <T>    the view item type
     * @return the page view
     */
    public static <E, T> PageView<T> of(IPage<E> page, Function<? super E, T> mapper) {
        List<T> items = page.getRecords().stream().map(mapper).toList();
        return new PageView<>(items, page.getTotal(), page.getCurrent(), page.getSize());
    }
}
