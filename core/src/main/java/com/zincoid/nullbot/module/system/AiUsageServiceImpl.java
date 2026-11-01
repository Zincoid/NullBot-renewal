package com.zincoid.nullbot.module.system;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.enums.AiTrigger;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.mapper.AiUsageMapper;
import com.zincoid.nullbot.platform.persistence.po.AiUsage;
import com.zincoid.nullbot.platform.persistence.row.AiUsageSummaryRow;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * {@link AiUsageService} 的默认实现。
 * <p>Default implementation of {@link AiUsageService}.</p>
 */
@Service
public class AiUsageServiceImpl extends ServiceImpl<AiUsageMapper, AiUsage> implements AiUsageService {

    @Override
    public void record(ChatType chatType, String chatId, String userId, AiTrigger trigger,
                       Integer inputTokens, Integer outputTokens) {
        baseMapper.increase(LocalDate.now(), chatType.getValue(), chatId, userId,
                trigger.getValue(), inputTokens, outputTokens);
    }

    // ════ Stats ═══════════════════

    @Override
    public Stats stats(LocalDate start, LocalDate end, int limit) {
        if (start == null || end == null) throw new SystemException("Missing key: range");
        if (start.isAfter(end)) throw new SystemException("Invalid range: " + start + " > " + end);
        int top = Math.clamp(limit, 1, 50);
        AiUsageSummaryRow summary = baseMapper.summary(start, end);
        List<Daily> daily = baseMapper.daily(start, end).stream()
                .map(row -> new Daily(row.getDay(), count(row.getCount()),
                        count(row.getInputTokens()), count(row.getOutputTokens())))
                .toList();
        List<Rank> triggers = baseMapper.triggers(start, end).stream()
                .map(row -> rank(triggerKey(row.getTriggerType()), row.getCount(),
                        row.getInputTokens(), row.getOutputTokens()))
                .toList();
        List<Rank> topGroups = baseMapper.groups(start, end, top).stream()
                .map(row -> rank(row.getKey(), row.getCount(),
                        row.getInputTokens(), row.getOutputTokens()))
                .toList();
        List<Rank> topUsers = baseMapper.users(start, end, top).stream()
                .map(row -> rank(row.getKey(), row.getCount(),
                        row.getInputTokens(), row.getOutputTokens()))
                .toList();
        long total = summary == null ? 0 : count(summary.getTotal());
        long inputTokens = summary == null ? 0 : count(summary.getInputTokens());
        long outputTokens = summary == null ? 0 : count(summary.getOutputTokens());
        long users = summary == null ? 0 : count(summary.getUsers());
        long groups = summary == null ? 0 : count(summary.getGroups());
        return new Stats(total, inputTokens, outputTokens, users, groups,
                daily, triggers, topGroups, topUsers);
    }

    // ════ Helpers ═════════════════

    private static Rank rank(String key, Long count, Long inputTokens, Long outputTokens) {
        return new Rank(key, count(count), count(inputTokens), count(outputTokens));
    }

    private static String triggerKey(Integer value) {
        AiTrigger trigger = AiTrigger.fromValue(value);
        return trigger == null ? String.valueOf(value) : trigger.getLabel();
    }

    private static long count(Long value) {
        return value == null ? 0 : value;
    }
}
