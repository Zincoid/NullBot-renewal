package com.zincoid.nullbot.module.setting;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;

/**
 * 群设置服务：按群读取与更新设置，缺失时按默认值建行。
 * <p>Group setting service: reads and updates per-group settings, creating a default row when absent.</p>
 */
public interface GroupSettingService extends IService<GroupSetting> {

    // ════ Get ═════════════════════

    /**
     * 读取群设置，缺失时创建默认设置。
     * <p>Returns the group settings, creating the default row when absent.</p>
     *
     * @param groupId platform group ID
     * @return the group settings
     */
    GroupSetting get(String groupId);

    // ════ Update ══════════════════

    /**
     * 更新群设置，缺失时新建。
     * <p>Updates the group settings, inserting them when absent.</p>
     *
     * @param setting the group settings
     */
    void update(GroupSetting setting);
}
