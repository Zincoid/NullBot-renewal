package com.zincoid.nullbot.module.system;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.mapper.UsageMapper;
import com.zincoid.nullbot.platform.persistence.po.Usage;
import com.zincoid.nullbot.platform.persistence.row.UsageRankRow;
import com.zincoid.nullbot.platform.persistence.row.UsageSummaryRow;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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

    // ════ Stats ═══════════════════

    @Override
    public long total(String userId) {
        if (userId == null || userId.isBlank()) throw new SystemException("Missing key: userId");
        return baseMapper.totalByUser(userId);
    }

    @Override
    public Stats stats(LocalDate start, LocalDate end, int limit) {
        if (start == null || end == null) throw new SystemException("Missing key: range");
        if (start.isAfter(end)) throw new SystemException("Invalid range: " + start + " > " + end);
        int top = Math.clamp(limit, 1, 50);
        UsageSummaryRow summary = baseMapper.summary(start, end);
        List<Daily> daily = baseMapper.daily(start, end).stream()
                .map(row -> new Daily(row.getDay(), count(row.getCount())))
                .toList();
        List<Rank> cmds = ranks(baseMapper.cmds(start, end, top));
        List<Rank> topGroups = ranks(baseMapper.groups(start, end, top));
        List<Rank> topUsers = ranks(baseMapper.users(start, end, top));
        long total = summary == null ? 0 : count(summary.getTotal());
        long users = summary == null ? 0 : count(summary.getUsers());
        long groups = summary == null ? 0 : count(summary.getGroups());
        return new Stats(total, users, groups, daily, cmds, topGroups, topUsers);
    }

    // ════ Helpers ═════════════════

    private static List<Rank> ranks(List<UsageRankRow> rows) {
        return rows.stream()
                .map(row -> new Rank(row.getName(), count(row.getCount())))
                .toList();
    }

    private static long count(Long value) {
        return value == null ? 0 : value;
    }
}
