package com.zincoid.nullbot.module.setting;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.mapper.UserSettingMapper;
import com.zincoid.nullbot.platform.persistence.po.UserSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * {@link UserSettingService} 的默认实现。
 * <p>Default implementation of {@link UserSettingService}.</p>
 */
@Slf4j
@Service
public class UserSettingServiceImpl extends ServiceImpl<UserSettingMapper, UserSetting> implements UserSettingService {

    // ════ Get ═════════════════════

    @Override
    public UserSetting get(String userId) {
        if (userId == null || userId.isBlank()) throw new SettingException("Missing key: userId");
        UserSetting setting = lambdaQuery().eq(UserSetting::getUserId, userId).one();
        return setting != null ? setting : UserSetting.builder().userId(userId).build();
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
    }
}
