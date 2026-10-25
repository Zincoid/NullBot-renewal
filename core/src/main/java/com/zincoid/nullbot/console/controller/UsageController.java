package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.UsageView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.system.UsageService;
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
import java.util.stream.Collectors;

/**
 * 控制台用量接口：汇总指令使用趋势与排行。
 * <p>Console usage API: summarizes command usage trends and rankings.</p>
 */
@RestController
@RequestMapping("/console/usage")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class UsageController {

    private static final int TOP = 20;

    private final UsageService usageService;
    private final GroupService groupService;
    private final UserService userService;

    // ════ Query ══════════════════

    /**
     * 查询最近若干天的用量汇总。
     * <p>Returns the usage summary of the last several days.</p>
     *
     * @param days number of days, 1..90
     * @return the usage summary
     */
    @GetMapping
    public UsageView usage(@RequestParam(defaultValue = "14") int days) {
        if (days < 1 || days > 90)
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Invalid days: " + days);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        UsageService.Stats stats = usageService.stats(start, end, TOP);
        Map<LocalDate, Long> counts = stats.daily().stream()
                .collect(Collectors.toMap(UsageService.Daily::day, UsageService.Daily::count,
                        (left, right) -> left));
        List<UsageView.Day> daily = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate date = start.plusDays(i);
            daily.add(new UsageView.Day(date, counts.getOrDefault(date, 0L)));
        }
        List<UsageView.Rank> cmds = stats.cmds().stream()
                .map(rank -> new UsageView.Rank(rank.key(), rank.key(), rank.count()))
                .toList();
        List<String> groupIds = stats.topGroups().stream()
                .map(UsageService.Rank::key)
                .filter(key -> !"0".equals(key))
                .toList();
        Map<String, String> groupNames = groupIds.isEmpty()
                ? Map.of()
                : groupService.listByIds(groupIds).stream()
                .collect(Collectors.toMap(Group::getId, Group::getName));
        List<UsageView.Rank> groups = stats.topGroups().stream()
                .map(rank -> new UsageView.Rank(
                        rank.key(),
                        "0".equals(rank.key())
                                ? "Private chat"
                                : groupNames.getOrDefault(rank.key(), rank.key()),
                        rank.count()))
                .toList();
        List<String> userIds = stats.topUsers().stream()
                .map(UsageService.Rank::key)
                .toList();
        Map<String, String> userNames = userIds.isEmpty()
                ? Map.of()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId,
                        user -> user.getName() == null ? user.getId() : user.getName()));
        List<UsageView.Rank> topUsers = stats.topUsers().stream()
                .map(rank -> new UsageView.Rank(rank.key(),
                        userNames.getOrDefault(rank.key(), rank.key()), rank.count()))
                .toList();
        return new UsageView(days, stats.total(), stats.users(), stats.groups(), daily, cmds, groups, topUsers);
    }
}
