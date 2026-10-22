package com.zincoid.nullbot.platform.persistence.row;

import lombok.Data;

import java.time.LocalDate;

/**
 * 每日用量查询行。
 * <p>Daily usage query row.</p>
 */
@Data
public class UsageDailyRow {

    private LocalDate day;
    private Long count;
}
