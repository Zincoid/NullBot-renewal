package com.zincoid.nullbot.module.setting;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.mapper.GroupKeywordMapper;
import com.zincoid.nullbot.platform.persistence.po.GroupKeyword;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link GroupKeywordService} 的默认实现。
 * <p>Default implementation of {@link GroupKeywordService}.</p>
 */
@Slf4j
@Service
public class GroupKeywordServiceImpl extends ServiceImpl<GroupKeywordMapper, GroupKeyword> implements GroupKeywordService {

    // ════ List ════════════════════

    @Override
    public List<GroupKeyword> listEnabled(String groupId) {
        if (groupId == null || groupId.isBlank()) throw new SettingException("Missing key: groupId");
        return lambdaQuery()
                .eq(GroupKeyword::getGroupId, groupId)
                .eq(GroupKeyword::getEnabled, true)
                .orderByAsc(GroupKeyword::getId)
                .list();
    }

    @Override
    public List<GroupKeyword> listAll(String groupId) {
        if (groupId == null || groupId.isBlank()) throw new SettingException("Missing key: groupId");
        return lambdaQuery()
                .eq(GroupKeyword::getGroupId, groupId)
                .orderByAsc(GroupKeyword::getId)
                .list();
    }

    // ════ Add ═════════════════════

    @Override
    public GroupKeyword add(String groupId, String keyword, String cmd, List<String> args) {
        if (groupId == null || groupId.isBlank()) throw new SettingException("Missing key: groupId");
        if (keyword == null || keyword.isBlank()) throw new SettingException("Missing key: keyword");
        if (cmd == null || cmd.isBlank()) throw new SettingException("Missing key: cmd");
        boolean exists = lambdaQuery()
                .eq(GroupKeyword::getGroupId, groupId)
                .eq(GroupKeyword::getKeyword, keyword)
                .exists();
        if (exists) throw new SettingException("Keyword exists: " + keyword);
        GroupKeyword rule = GroupKeyword.builder()
                .groupId(groupId)
                .keyword(keyword)
                .cmd(cmd)
                .args(args == null || args.isEmpty() ? null : args)
                .enabled(true)
                .build();
        save(rule);
        log.info("Keyword added: group={}, keyword={}", groupId, keyword);
        return rule;
    }

    // ════ Update ══════════════════

    @Override
    public GroupKeyword update(Long id, String keyword, String cmd, List<String> args) {
        if (id == null) throw new SettingException("Missing key: id");
        if (keyword == null || keyword.isBlank()) throw new SettingException("Missing key: keyword");
        if (cmd == null || cmd.isBlank()) throw new SettingException("Missing key: cmd");
        GroupKeyword rule = getById(id);
        if (rule == null) throw new SettingException("Keyword not found: " + id);
        boolean exists = lambdaQuery()
                .eq(GroupKeyword::getGroupId, rule.getGroupId())
                .eq(GroupKeyword::getKeyword, keyword)
                .ne(GroupKeyword::getId, id)
                .exists();
        if (exists) throw new SettingException("Keyword exists: " + keyword);
        GroupKeyword update = GroupKeyword.builder()
                .id(id)
                .keyword(keyword)
                .cmd(cmd)
                .args(args == null || args.isEmpty() ? List.of() : args)
                .build();
        if (!updateById(update)) throw new SettingException("Keyword not found: " + id);
        log.info("Keyword updated: {}", id);
        return getById(id);
    }

    // ════ Delete ══════════════════

    @Override
    public void delete(Long id) {
        if (id == null) throw new SettingException("Missing key: id");
        if (!removeById(id)) throw new SettingException("Keyword not found: " + id);
        log.info("Keyword deleted: {}", id);
    }

    // ════ Enabled ═════════════════

    @Override
    public void enable(Long id) {
        setEnabled(id, true);
    }

    @Override
    public void disable(Long id) {
        setEnabled(id, false);
    }

    private void setEnabled(Long id, boolean enabled) {
        if (id == null) throw new SettingException("Missing key: id");
        boolean updated = lambdaUpdate()
                .eq(GroupKeyword::getId, id)
                .set(GroupKeyword::getEnabled, enabled)
                .update();
        if (!updated) throw new SettingException("Keyword not found: " + id);
        log.info("Keyword {}: {}", enabled ? "enabled" : "disabled", id);
    }
}
