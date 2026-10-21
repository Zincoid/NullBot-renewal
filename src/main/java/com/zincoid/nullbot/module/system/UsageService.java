package com.zincoid.nullbot.module.system;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.Usage;

/**
 * 指令用量服务：按天累积使用次数。
 * <p>Command usage service: accumulates daily usage counts.</p>
 */
public interface UsageService extends IService<Usage> {

    // ════ Increase ════════════════

    /**
     * 增加一次指令使用计数。
     * <p>Increases the command usage count by one.</p>
     *
     * @param groupId group platform id, {@code 0} for private chat
     * @param userId  operator platform user id
     * @param cmd     command name
     */
    void increase(String groupId, String userId, String cmd);
}
