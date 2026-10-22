package com.zincoid.nullbot.module.setting;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.enums.LimitScope;
import com.zincoid.nullbot.platform.persistence.mapper.GroupSettingMapper;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link GroupSettingService} 的默认实现。
 * <p>Default implementation of {@link GroupSettingService}.</p>
 */
@Slf4j
@Service
public class GroupSettingServiceImpl extends ServiceImpl<GroupSettingMapper, GroupSetting> implements GroupSettingService {

    private final Map<String, GroupSetting> cache = new ConcurrentHashMap<>();

    // ════ Get ═════════════════════

    @Override
    public GroupSetting get(String groupId) {
        if (groupId == null || groupId.isBlank()) throw new SettingException("Missing key: groupId");
        return cache.computeIfAbsent(groupId, this::load);
    }

    // ════ Update ══════════════════

    @Override
    public void update(GroupSetting setting) {
        if (setting == null) throw new SettingException("Missing key: setting");
        String groupId = setting.getGroupId();
        if (groupId == null || groupId.isBlank()) throw new SettingException("Missing key: groupId");
        GroupSetting existing = lambdaQuery().eq(GroupSetting::getGroupId, groupId).one();
        if (existing == null) {
            save(setting);
            log.info("Group setting created: {}", groupId);
        } else {
            setting.setId(existing.getId());
            updateById(setting);
            log.info("Group setting updated: {}", groupId);
        }
        GroupSetting stored = getById(setting.getId());
        if (stored == null) throw new SettingException("Group setting not found: " + groupId);
        cache.put(groupId, stored);
    }

    // ════ Helpers ═════════════════

    private GroupSetting load(String groupId) {
        GroupSetting setting = lambdaQuery().eq(GroupSetting::getGroupId, groupId).one();
        if (setting != null) return setting;
        setting = GroupSetting.builder()
                .groupId(groupId)
                .language(Language.AUTO)
                .limitEnabled(true)
                .limitScope(LimitScope.USER)
                .limitCapacity(5)
                .limitRefill(2)
                .limitInterval(1)
                .aiEffort(Effort.HIGH)
                .aiInjectionGuard(true)
                .build();
        save(setting);
        log.info("Group setting created: {}", groupId);
        return setting;
    }
}
