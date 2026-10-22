package com.zincoid.nullbot.platform.persistence.row;

import lombok.Data;

/**
 * 用量汇总查询行。
 * <p>Usage summary query row.</p>
 */
@Data
public class UsageSummaryRow {

    private Long total;
    private Long users;
    private Long groups;
}
