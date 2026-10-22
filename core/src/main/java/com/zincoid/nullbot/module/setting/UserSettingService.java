package com.zincoid.nullbot.module.setting;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.UserSetting;

/**
 * 用户设置服务：按用户读取与更新设置，缺失时返回默认设置。
 * <p>User setting service: reads and updates per-user settings, returning defaults when absent.</p>
 */
public interface UserSettingService extends IService<UserSetting> {

    // ════ Get ═════════════════════

    /**
     * 读取用户设置，缺失时返回未持久化的默认设置。
     * <p>Returns the user settings, or a transient default when absent.</p>
     *
     * @param userId platform user id
     * @return the user settings
     */
    UserSetting get(String userId);

    // ════ Update ══════════════════

    /**
     * 更新用户设置，缺失时新建。
     * <p>Updates the user settings, inserting them when absent.</p>
     *
     * @param setting the user settings
     */
    void update(UserSetting setting);
}
