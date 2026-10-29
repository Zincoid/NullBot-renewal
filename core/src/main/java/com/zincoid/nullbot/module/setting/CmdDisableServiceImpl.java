package com.zincoid.nullbot.module.setting;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zincoid.nullbot.platform.persistence.mapper.GroupCmdDisableMapper;
import com.zincoid.nullbot.platform.persistence.mapper.PrivateCmdDisableMapper;
import com.zincoid.nullbot.platform.persistence.po.GroupCmdDisable;
import com.zincoid.nullbot.platform.persistence.po.PrivateCmdDisable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * {@link CmdDisableService} 的默认实现。
 * <p>Default implementation of {@link CmdDisableService}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CmdDisableServiceImpl implements CmdDisableService {

    private final GroupCmdDisableMapper groupMapper;
    private final PrivateCmdDisableMapper privateMapper;

    // ════ Group ═══════════════════

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

    // ════ Private ═════════════════

    @Override
    public PrivateCmdDisable findPrivate(String userId, String cmd) {
        String uid = normalizePrivateUserId(userId);
        String name = normalizeCommand(cmd);
        PrivateCmdDisable rule = privateMapper.selectOne(Wrappers.lambdaQuery(PrivateCmdDisable.class)
                .eq(PrivateCmdDisable::getUserId, uid)
                .eq(PrivateCmdDisable::getCmd, name));
        return rule == null || expired(rule.getExpiresAt()) ? null : rule;
    }

    @Override
    public PrivateCmdDisable addPrivate(String userId, String cmd, LocalDateTime expiresAt) {
        String uid = normalizePrivateUserId(userId);
        String name = normalizeCommand(cmd);
        PrivateCmdDisable existing = privateMapper.selectOne(Wrappers.lambdaQuery(PrivateCmdDisable.class)
                .eq(PrivateCmdDisable::getUserId, uid)
                .eq(PrivateCmdDisable::getCmd, name));
        if (existing != null) {
            if (existing.getExpiresAt() == null) return existing;
            privateMapper.update(null, Wrappers.lambdaUpdate(PrivateCmdDisable.class)
                    .eq(PrivateCmdDisable::getId, existing.getId())
                    .set(PrivateCmdDisable::getExpiresAt, expiresAt));
            PrivateCmdDisable stored = privateMapper.selectById(existing.getId());
            if (stored == null) throw new SettingException("Private command disable not found: " + name);
            log.info("Private command disable updated: user={}, cmd={}, expires={}",
                    uid, name, expiresAt);
            return stored;
        }
        PrivateCmdDisable rule = PrivateCmdDisable.builder()
                .userId(uid)
                .cmd(name)
                .expiresAt(expiresAt)
                .build();
        if (privateMapper.insert(rule) != 1)
            throw new SettingException("Private command disable insert failed: " + name);
        log.info("Private command disable added: user={}, cmd={}, expires={}", uid, name, expiresAt);
        return rule;
    }

    @Override
    public boolean removePrivate(String userId, String cmd) {
        String uid = normalizePrivateUserId(userId);
        String name = normalizeCommand(cmd);
        boolean removed = privateMapper.delete(Wrappers.lambdaQuery(PrivateCmdDisable.class)
                .eq(PrivateCmdDisable::getUserId, uid)
                .eq(PrivateCmdDisable::getCmd, name)) > 0;
        if (removed) {
            log.info("Private command disable removed: user={}, cmd={}", uid, name);
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

    private static String normalizePrivateUserId(String userId) {
        if (userId == null || userId.isBlank())
            throw new SettingException("Missing key: userId");
        return userId.trim();
    }

    private static String normalizeCommand(String cmd) {
        if (cmd == null || cmd.isBlank()) throw new SettingException("Missing key: cmd");
        return cmd.trim();
    }
}
