package com.zincoid.nullbot.module.setting;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.CmdDisable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指令禁用服务：维护群聊与私聊的禁用规则。
 * <p>Command disable service: maintains command-disable rules for group and private chats.</p>
 */
public interface CmdDisableService extends IService<CmdDisable> {

    /**
     * 群规则中表示整群的用户 ID 哨兵。
     * <p>User-id sentinel for a whole group in group rules.</p>
     */
    String WHOLE_GROUP = "*";

    // ════ Group ═══════════════════

    /**
     * 列出群聊中生效的规则，按指令名、整群优先、用户 ID 排序。
     * <p>Lists the active rules of a group, ordered by command name, whole-group first, then user id.</p>
     *
     * @param groupId group id
     * @return the active rules
     */
    List<CmdDisable> listGroup(String groupId);

    /**
     * 查找群聊中生效的规则，未找到或已过期时返回 null。
     * <p>Finds an active group rule, returning null when absent or expired.</p>
     *
     * @param groupId group id
     * @param userId  user id, {@code *} for a whole group
     * @param cmd     the canonical command name
     * @return the active rule, or null
     */
    CmdDisable findGroup(String groupId, String userId, String cmd);

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
    CmdDisable addGroup(String groupId, String userId, String cmd, LocalDateTime expiresAt);

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
     * 列出私聊中生效的规则，按指令名排序。
     * <p>Lists the active rules of a private chat, ordered by command name.</p>
     *
     * @param userId user id
     * @return the active rules
     */
    List<CmdDisable> listPrivate(String userId);

    /**
     * 查找私聊中生效的规则，未找到或已过期时返回 null。
     * <p>Finds an active private rule, returning null when absent or expired.</p>
     *
     * @param userId user id
     * @param cmd    the canonical command name
     * @return the active rule, or null
     */
    CmdDisable findPrivate(String userId, String cmd);

    /**
     * 添加或更新私聊规则；已有的永久规则不会被临时规则降级。
     * <p>Adds or updates a private rule; an existing permanent rule is not downgraded by a temporary one.</p>
     *
     * @param userId    user id
     * @param cmd       the canonical command name
     * @param expiresAt expiry time, or null for a permanent rule
     * @return the stored rule
     */
    CmdDisable addPrivate(String userId, String cmd, LocalDateTime expiresAt);

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
