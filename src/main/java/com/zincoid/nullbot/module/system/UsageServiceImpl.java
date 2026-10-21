package com.zincoid.nullbot.module.system;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.mapper.UsageMapper;
import com.zincoid.nullbot.platform.persistence.po.Usage;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * {@link UsageService} 的默认实现。
 * <p>Default implementation of {@link UsageService}.</p>
 */
@Service
public class UsageServiceImpl extends ServiceImpl<UsageMapper, Usage> implements UsageService {

    // ════ Increase ════════════════

    @Override
    public void increase(String groupId, String userId, String cmd) {
        if (groupId == null || groupId.isBlank()) throw new SystemException("Missing key: groupId");
        if (userId == null || userId.isBlank()) throw new SystemException("Missing key: userId");
        if (cmd == null || cmd.isBlank()) throw new SystemException("Missing key: cmd");
        baseMapper.increase(LocalDate.now(), groupId, userId, cmd);
    }
}
