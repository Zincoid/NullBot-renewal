package com.zincoid.nullbot.module.setting;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zincoid.nullbot.platform.persistence.mapper.GroupCmdDisableMapper;
import com.zincoid.nullbot.platform.persistence.mapper.UserCmdDisableMapper;
import com.zincoid.nullbot.platform.persistence.po.GroupCmdDisable;
import com.zincoid.nullbot.platform.persistence.po.UserCmdDisable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * {@link CmdDisableService} 的默认实现。
 * <p>Default implementation of {@link CmdDisableService}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CmdDisableServiceImpl implements CmdDisableService {

    private final GroupCmdDisableMapper groupMapper;
    private final UserCmdDisableMapper userMapper;

    // ════ Group ═══════════════════

    @Override
    public List<GroupCmdDisable> listGroup(String groupId) {
        String gid = normalizeGroupId(groupId);
        return groupMapper.selectList(Wrappers.lambdaQuery(GroupCmdDisable.class)
                        .eq(GroupCmdDisable::getGroupId, gid))
                .stream()
                .filter(rule -> !expired(rule.getExpiresAt()))
                .sorted(Comparator
                        .comparing(GroupCmdDisable::getCmd, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(rule -> !CmdDisableService.WHOLE_GROUP.equals(rule.getUserId()))
                        .thenComparing(GroupCmdDisable::getUserId))
                .toList();
    }

    @Override
    public GroupCmdDisable findGroup(String groupId, String userId, String cmd) {
        String gid = normalizeGroupId(groupId);
        String uid = normalizeGroupUserId(userId);
        String name = normalizeCommand(cmd);
        GroupCmdDisable rule = groupMapper.selectOne(Wrappers.lambdaQuery(GroupCmdDisable.class)
                .eq(GroupCmdDisable::getGroupId, gid)
                .eq(GroupCmdDisable::getUserId, uid)
                .eq(GroupCmdDisable::getCmd, name));
        return rule == null || expired(rule.getExpiresAt()) ? null : rule;
    }

    @Override
    public GroupCmdDisable addGroup(String groupId, String userId, String cmd, LocalDateTime expiresAt) {
        String gid = normalizeGroupId(groupId);
        String uid = normalizeGroupUserId(userId);
        String name = normalizeCommand(cmd);
        GroupCmdDisable existing = groupMapper.selectOne(Wrappers.lambdaQuery(GroupCmdDisable.class)
                .eq(GroupCmdDisable::getGroupId, gid)
                .eq(GroupCmdDisable::getUserId, uid)
                .eq(GroupCmdDisable::getCmd, name));
        if (existing != null) {
            if (existing.getExpiresAt() == null) return existing;
            groupMapper.update(null, Wrappers.lambdaUpdate(GroupCmdDisable.class)
                    .eq(GroupCmdDisable::getId, existing.getId())
                    .set(GroupCmdDisable::getExpiresAt, expiresAt));
            GroupCmdDisable stored = groupMapper.selectById(existing.getId());
            if (stored == null) throw new SettingException("Group command disable not found: " + name);
            log.info("Group command disable updated: group={}, user={}, cmd={}, expires={}",
                    gid, uid, name, expiresAt);
            return stored;
        }
        GroupCmdDisable rule = GroupCmdDisable.builder()
                .groupId(gid)
                .userId(uid)
                .cmd(name)
                .expiresAt(expiresAt)
                .build();
        if (groupMapper.insert(rule) != 1)
            throw new SettingException("Group command disable insert failed: " + name);
        log.info("Group command disable added: group={}, user={}, cmd={}, expires={}",
                gid, uid, name, expiresAt);
        return rule;
    }

    @Override
    public boolean removeGroup(String groupId, String userId, String cmd) {
        String gid = normalizeGroupId(groupId);
        String uid = normalizeGroupUserId(userId);
        String name = normalizeCommand(cmd);
        boolean removed = groupMapper.delete(Wrappers.lambdaQuery(GroupCmdDisable.class)
                .eq(GroupCmdDisable::getGroupId, gid)
                .eq(GroupCmdDisable::getUserId, uid)
                .eq(GroupCmdDisable::getCmd, name)) > 0;
        if (removed) {
            log.info("Group command disable removed: group={}, user={}, cmd={}", gid, uid, name);
        }
        return removed;
    }

    // ════ User ════════════════════

    @Override
    public List<UserCmdDisable> listUser(String userId) {
        String uid = normalizeUserId(userId);
        return userMapper.selectList(Wrappers.lambdaQuery(UserCmdDisable.class)
                        .eq(UserCmdDisable::getUserId, uid))
                .stream()
                .filter(rule -> !expired(rule.getExpiresAt()))
                .sorted(Comparator.comparing(UserCmdDisable::getCmd, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public UserCmdDisable findUser(String userId, String cmd) {
        String uid = normalizeUserId(userId);
        String name = normalizeCommand(cmd);
        UserCmdDisable rule = userMapper.selectOne(Wrappers.lambdaQuery(UserCmdDisable.class)
                .eq(UserCmdDisable::getUserId, uid)
                .eq(UserCmdDisable::getCmd, name));
        return rule == null || expired(rule.getExpiresAt()) ? null : rule;
    }

    @Override
    public UserCmdDisable addUser(String userId, String cmd, LocalDateTime expiresAt) {
        String uid = normalizeUserId(userId);
        String name = normalizeCommand(cmd);
        UserCmdDisable existing = userMapper.selectOne(Wrappers.lambdaQuery(UserCmdDisable.class)
                .eq(UserCmdDisable::getUserId, uid)
                .eq(UserCmdDisable::getCmd, name));
        if (existing != null) {
            if (existing.getExpiresAt() == null) return existing;
            userMapper.update(null, Wrappers.lambdaUpdate(UserCmdDisable.class)
                    .eq(UserCmdDisable::getId, existing.getId())
                    .set(UserCmdDisable::getExpiresAt, expiresAt));
            UserCmdDisable stored = userMapper.selectById(existing.getId());
            if (stored == null) throw new SettingException("User command disable not found: " + name);
            log.info("User command disable updated: user={}, cmd={}, expires={}",
                    uid, name, expiresAt);
            return stored;
        }
        UserCmdDisable rule = UserCmdDisable.builder()
                .userId(uid)
                .cmd(name)
                .expiresAt(expiresAt)
                .build();
        if (userMapper.insert(rule) != 1)
            throw new SettingException("User command disable insert failed: " + name);
        log.info("User command disable added: user={}, cmd={}, expires={}", uid, name, expiresAt);
        return rule;
    }

    @Override
    public boolean removeUser(String userId, String cmd) {
        String uid = normalizeUserId(userId);
        String name = normalizeCommand(cmd);
        boolean removed = userMapper.delete(Wrappers.lambdaQuery(UserCmdDisable.class)
                .eq(UserCmdDisable::getUserId, uid)
                .eq(UserCmdDisable::getCmd, name)) > 0;
        if (removed) {
            log.info("User command disable removed: user={}, cmd={}", uid, name);
        }
        return removed;
    }

    // ════ Helpers ═════════════════

    private static boolean expired(LocalDateTime expiresAt) {
        return expiresAt != null && !expiresAt.isAfter(LocalDateTime.now());
    }

    private static String normalizeGroupId(String groupId) {
        if (groupId == null || groupId.isBlank())
            throw new SettingException("Missing key: groupId");
        return groupId;
    }

    private static String normalizeGroupUserId(String userId) {
        return userId == null || userId.isBlank() ? CmdDisableService.WHOLE_GROUP : userId.trim();
    }

    private static String normalizeUserId(String userId) {
        if (userId == null || userId.isBlank())
            throw new SettingException("Missing key: userId");
        return userId.trim();
    }

    private static String normalizeCommand(String cmd) {
        if (cmd == null || cmd.isBlank()) throw new SettingException("Missing key: cmd");
        return cmd.trim();
    }
}
