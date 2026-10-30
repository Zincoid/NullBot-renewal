package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.po.CmdDisable;

import java.time.LocalDateTime;

/**
 * 控制台指令禁用规则视图。
 * <p>Console view of a command-disable rule.</p>
 *
 * @param id        the rule id
 * @param userId    the target user id; {@code *} for a whole group, the chat user for private rules
 * @param cmd       the canonical command name
 * @param expiresAt the expiry time, or null for a permanent rule
 */
public record CmdDisableView(Long id, String userId, String cmd, LocalDateTime expiresAt) {

    /**
     * 由规则构建视图。
     * <p>Builds the view from a rule.</p>
     *
     * @param rule the command-disable rule
     * @return the view
     */
    public static CmdDisableView of(CmdDisable rule) {
        return new CmdDisableView(rule.getId(), rule.getUserId(), rule.getCmd(), rule.getExpiresAt());
    }
}
