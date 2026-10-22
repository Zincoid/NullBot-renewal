package com.zincoid.nullbot.platform.persistence.row;

import lombok.Data;

/**
 * 用量排行查询行。
 * <p>Usage ranking query row.</p>
 */
@Data
public class UsageRankRow {

    private String name;
    private Long count;
}
