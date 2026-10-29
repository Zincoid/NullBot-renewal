package com.zincoid.nullbot.module.setting;

import com.zincoid.nullbot.platform.persistence.po.GroupCmdDisable;
import com.zincoid.nullbot.platform.persistence.po.PrivateCmdDisable;

import java.time.LocalDateTime;

/**
 * 指令禁用服务：分别维护群聊与私聊禁用规则。
 * <p>Command disable service: maintains group-chat and private-chat rules separately.</p>
 */
public interface CmdDisableService {

    /**
     * 群规则中表示整群的用户 ID 哨兵。
     * <p>User-id sentinel for a whole group in group rules.</p>
     */
    String WHOLE_GROUP = "*";

    // ════ Group ═══════════════════

    /**
     * 查找群聊中生效的规则，未找到或已过期时返回 null。
     * <p>Finds an active group rule, returning null when absent or expired.</p>
     *
     * @param groupId group id
     * @param userId  user id, {@code *} for a whole group
     * @param cmd     the canonical command name
     * @return the active rule, or null
     */
    GroupCmdDisable findGroup(String groupId, String userId, String cmd);

    /**
     * 添加或更新群聊规则；已有的永久规则不会被临时规则降级。
     * <p>Adds or updates a group rule; an existing permanent rule is not downgraded by a temporary one.</p>
     *
     * @param groupId   group id
     * @param userId    user id, {@code *} for a whole group
     * @param cmd       the canonical command name
     * @param expiresAt expiry time, or null for a permanent rule
     * @return the stored rule
     */
    GroupCmdDisable addGroup(String groupId, String userId, String cmd, LocalDateTime expiresAt);

    /**
     * 删除一条精确匹配的群聊规则。
     * <p>Removes one exactly matching group rule.</p>
     *
     * @param groupId group id
     * @param userId  user id, {@code *} for a whole group
     * @param cmd     the canonical command name
     * @return true when a rule was removed
     */
    boolean removeGroup(String groupId, String userId, String cmd);

    // ════ Private ═════════════════

    /**
     * 查找私聊中生效的规则，未找到或已过期时返回 null。
     * <p>Finds an active private rule, returning null when absent or expired.</p>
     *
     * @param userId user id
     * @param cmd    the canonical command name
     * @return the active rule, or null
     */
    PrivateCmdDisable findPrivate(String userId, String cmd);

    /**
     * 添加或更新私聊规则；已有的永久规则不会被临时规则降级。
     * <p>Adds or updates a private rule; an existing permanent rule is not downgraded by a temporary one.</p>
     *
     * @param userId    user id
     * @param cmd       the canonical command name
     * @param expiresAt expiry time, or null for a permanent rule
     * @return the stored rule
     */
    PrivateCmdDisable addPrivate(String userId, String cmd, LocalDateTime expiresAt);

    /**
     * 删除一条精确匹配的私聊规则。
     * <p>Removes one exactly matching private rule.</p>
     *
     * @param userId user id
     * @param cmd    the canonical command name
     * @return true when a rule was removed
     */
    boolean removePrivate(String userId, String cmd);
}
