package com.zincoid.nullbot.module.setting;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.mapper.CmdDisableMapper;
import com.zincoid.nullbot.platform.persistence.po.CmdDisable;
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
public class CmdDisableServiceImpl extends ServiceImpl<CmdDisableMapper, CmdDisable>
        implements CmdDisableService {

    // ════ Group ═══════════════════

    @Override
    public List<CmdDisable> listGroup(String groupId) {
        return list(ChatType.GROUP, normalizeGroupId(groupId));
    }

    @Override
    public CmdDisable findGroup(String groupId, String userId, String cmd) {
        return find(ChatType.GROUP, normalizeGroupId(groupId),
                normalizeGroupUserId(userId), normalizeCommand(cmd));
    }

    @Override
    public CmdDisable addGroup(String groupId, String userId, String cmd, LocalDateTime expiresAt) {
        return add(ChatType.GROUP, normalizeGroupId(groupId),
                normalizeGroupUserId(userId), normalizeCommand(cmd), expiresAt);
    }

    @Override
    public boolean removeGroup(String groupId, String userId, String cmd) {
        return remove(ChatType.GROUP, normalizeGroupId(groupId),
                normalizeGroupUserId(userId), normalizeCommand(cmd));
    }

    // ════ Private ═════════════════

    @Override
    public List<CmdDisable> listPrivate(String userId) {
        return list(ChatType.PRIVATE, normalizeUserId(userId));
    }

    @Override
    public CmdDisable findPrivate(String userId, String cmd) {
        String uid = normalizeUserId(userId);
        return find(ChatType.PRIVATE, uid, uid, normalizeCommand(cmd));
    }

    @Override
    public CmdDisable addPrivate(String userId, String cmd, LocalDateTime expiresAt) {
        String uid = normalizeUserId(userId);
        return add(ChatType.PRIVATE, uid, uid, normalizeCommand(cmd), expiresAt);
    }

    @Override
    public boolean removePrivate(String userId, String cmd) {
        String uid = normalizeUserId(userId);
        return remove(ChatType.PRIVATE, uid, uid, normalizeCommand(cmd));
    }

    // ════ Helpers ═════════════════

    private List<CmdDisable> list(ChatType chatType, String chatId) {
        return lambdaQuery()
                .eq(CmdDisable::getChatType, chatType)
                .eq(CmdDisable::getChatId, chatId)
                .list()
                .stream()
                .filter(rule -> !expired(rule.getExpiresAt()))
                .sorted(Comparator
                        .comparing(CmdDisable::getCmd, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(rule -> chatType == ChatType.GROUP
                                && !CmdDisableService.WHOLE_GROUP.equals(rule.getUserId()))
                        .thenComparing(CmdDisable::getUserId))
                .toList();
    }

    private CmdDisable find(ChatType chatType, String chatId, String userId, String cmd) {
        CmdDisable rule = findRow(chatType, chatId, userId, cmd);
        return rule == null || expired(rule.getExpiresAt()) ? null : rule;
    }

    private CmdDisable add(ChatType chatType, String chatId, String userId,
                           String cmd, LocalDateTime expiresAt) {
        CmdDisable existing = findRow(chatType, chatId, userId, cmd);
        if (existing != null) {
            if (existing.getExpiresAt() == null) return existing;
            lambdaUpdate()
                    .eq(CmdDisable::getId, existing.getId())
                    .set(CmdDisable::getExpiresAt, expiresAt)
                    .update();
            CmdDisable stored = getById(existing.getId());
            if (stored == null) throw new SettingException("Command disable not found: " + cmd);
            log.info("Command disable updated: type={}, chat={}, user={}, cmd={}, expires={}",
                    chatType.getLabel(), chatId, userId, cmd, expiresAt);
            return stored;
        }
        CmdDisable rule = CmdDisable.builder()
                .chatType(chatType)
                .chatId(chatId)
                .userId(userId)
                .cmd(cmd)
                .expiresAt(expiresAt)
                .build();
        if (!save(rule))
            throw new SettingException("Command disable insert failed: " + cmd);
        log.info("Command disable added: type={}, chat={}, user={}, cmd={}, expires={}",
                chatType.getLabel(), chatId, userId, cmd, expiresAt);
        return rule;
    }

    private boolean remove(ChatType chatType, String chatId, String userId, String cmd) {
        boolean removed = remove(Wrappers.lambdaQuery(CmdDisable.class)
                .eq(CmdDisable::getChatType, chatType)
                .eq(CmdDisable::getChatId, chatId)
                .eq(CmdDisable::getUserId, userId)
                .eq(CmdDisable::getCmd, cmd));
        if (removed) {
            log.info("Command disable removed: type={}, chat={}, user={}, cmd={}",
                    chatType.getLabel(), chatId, userId, cmd);
        }
        return removed;
    }

    private CmdDisable findRow(ChatType chatType, String chatId, String userId, String cmd) {
        return lambdaQuery()
                .eq(CmdDisable::getChatType, chatType)
                .eq(CmdDisable::getChatId, chatId)
                .eq(CmdDisable::getUserId, userId)
                .eq(CmdDisable::getCmd, cmd)
                .one();
    }

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
