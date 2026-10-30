package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.po.GroupCmdDisable;
import com.zincoid.nullbot.platform.persistence.po.UserCmdDisable;

import java.time.LocalDateTime;

/**
 * 控制台指令禁用规则视图。
 * <p>Console view of a command-disable rule.</p>
 *
 * @param id        the rule id
 * @param userId    the target user id, {@code *} for a whole group
 * @param cmd       the canonical command name
 * @param expiresAt the expiry time, or null for a permanent rule
 */
public record CmdDisableView(Long id, String userId, String cmd, LocalDateTime expiresAt) {

    /**
     * 由群聊规则构建视图。
     * <p>Builds the view from a group rule.</p>
     *
     * @param rule the group rule
     * @return the view
     */
    public static CmdDisableView of(GroupCmdDisable rule) {
        return new CmdDisableView(rule.getId(), rule.getUserId(), rule.getCmd(), rule.getExpiresAt());
    }

    /**
     * 由私聊规则构建视图。
     * <p>Builds the view from a private rule.</p>
     *
     * @param rule the private rule
     * @return the view
     */
    public static CmdDisableView of(UserCmdDisable rule) {
        return new CmdDisableView(rule.getId(), rule.getUserId(), rule.getCmd(), rule.getExpiresAt());
    }
}
