package com.zincoid.nullbot.module.setting;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.mapper.UserSettingMapper;
import com.zincoid.nullbot.platform.persistence.po.UserSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link UserSettingService} 的默认实现。
 * <p>Default implementation of {@link UserSettingService}.</p>
 */
@Slf4j
@Service
public class UserSettingServiceImpl extends ServiceImpl<UserSettingMapper, UserSetting> implements UserSettingService {

    private final Map<String, UserSetting> cache = new ConcurrentHashMap<>();

    // ════ Get ═════════════════════

    @Override
    public UserSetting get(String userId) {
        if (userId == null || userId.isBlank()) throw new SettingException("Missing key: userId");
        return cache.computeIfAbsent(userId, this::load).toBuilder().build();
    }

    // ════ Update ══════════════════

    @Override
    public void update(UserSetting setting) {
        if (setting == null) throw new SettingException("Missing key: setting");
        String userId = setting.getUserId();
        if (userId == null || userId.isBlank()) throw new SettingException("Missing key: userId");
        UserSetting existing = lambdaQuery().eq(UserSetting::getUserId, userId).one();
        if (existing == null) {
            save(setting);
            log.info("User setting created: {}", userId);
        } else {
            setting.setId(existing.getId());
            updateById(setting);
            log.info("User setting updated: {}", userId);
        }
        UserSetting stored = getById(setting.getId());
        if (stored == null) throw new SettingException("User setting not found: " + userId);
        cache.put(userId, stored);
    }

    // ════ Helpers ═════════════════

    private UserSetting load(String userId) {
        UserSetting setting = lambdaQuery().eq(UserSetting::getUserId, userId).one();
        if (setting != null) return setting;
        return UserSetting.builder()
                .userId(userId)
                .privateAiEffort(Effort.HIGH)
                .privateAiInjectionGuard(true)
                .privateAiEpisodicMemoryEnabled(false)
                .privateAiSemanticMemoryEnabled(false)
                .build();
    }
}
