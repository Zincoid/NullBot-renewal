package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.AiUsageTotalView;
import com.zincoid.nullbot.console.dto.AiUsageView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.system.AiUsageService;
import com.zincoid.nullbot.platform.persistence.po.Group;
import com.zincoid.nullbot.platform.persistence.po.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 控制台 AI 用量接口：汇总 AI 对话、token 消耗与排行。
 * <p>Console AI usage API: summarizes AI turns, token consumption, and rankings.</p>
 */
@RestController
@RequestMapping("/console/ai-usage")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class AiUsageController {

    private static final int TOP = 20;

    private final AiUsageService aiUsageService;
    private final GroupService groupService;
    private final UserService userService;

    // ════ Query ═══════════════════

    /**
     * 查询全量 AI 用量。
     * <p>Returns the all-time AI usage.</p>
     *
     * @return the all-time AI usage total
     */
    @GetMapping("/total")
    public AiUsageTotalView total() {
        AiUsageService.Total total = aiUsageService.total();
        return new AiUsageTotalView(total.count(), total.inputTokens(), total.outputTokens());
    }

    /**
     * 查询最近若干天的 AI 用量汇总。
     * <p>Returns the AI usage summary of the last several days.</p>
     *
     * @param days number of days, 1..90
     * @return the AI usage summary
     */
    @GetMapping
    public AiUsageView usage(@RequestParam(defaultValue = "14") int days) {
        if (days < 1 || days > 90)
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Invalid days: " + days);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        AiUsageService.Stats stats = aiUsageService.stats(start, end, TOP);
        Map<LocalDate, AiUsageService.Daily> counts = stats.daily().stream()
                .collect(Collectors.toMap(AiUsageService.Daily::day, Function.identity(),
                        (left, right) -> left));
        List<AiUsageView.Day> daily = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate date = start.plusDays(i);
            AiUsageService.Daily row = counts.get(date);
            daily.add(row == null
                    ? new AiUsageView.Day(date, 0, 0, 0)
                    : new AiUsageView.Day(date, row.count(), row.inputTokens(), row.outputTokens()));
        }
        List<AiUsageView.Rank> triggers = stats.triggers().stream()
                .map(rank -> new AiUsageView.Rank(rank.key(), rank.key(),
                        rank.count(), rank.inputTokens(), rank.outputTokens()))
                .toList();
        List<String> groupIds = stats.topGroups().stream()
                .map(AiUsageService.Rank::key)
                .toList();
        Map<String, String> groupNames = groupIds.isEmpty()
                ? Map.of()
                : groupService.listByIds(groupIds).stream()
                .collect(Collectors.toMap(Group::getId, Group::getName));
        List<AiUsageView.Rank> groups = stats.topGroups().stream()
                .map(rank -> new AiUsageView.Rank(rank.key(),
                        groupNames.getOrDefault(rank.key(), rank.key()),
                        rank.count(), rank.inputTokens(), rank.outputTokens()))
                .toList();
        List<String> userIds = stats.topUsers().stream()
                .map(AiUsageService.Rank::key)
                .toList();
        Map<String, String> userNames = userIds.isEmpty()
                ? Map.of()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId,
                        user -> user.getName() == null ? user.getId() : user.getName()));
        List<AiUsageView.Rank> topUsers = stats.topUsers().stream()
                .map(rank -> new AiUsageView.Rank(rank.key(),
                        userNames.getOrDefault(rank.key(), rank.key()),
                        rank.count(), rank.inputTokens(), rank.outputTokens()))
                .toList();
        return new AiUsageView(days, stats.total(), stats.inputTokens(), stats.outputTokens(),
                stats.users(), stats.groups(), daily, triggers, groups, topUsers);
    }
}
