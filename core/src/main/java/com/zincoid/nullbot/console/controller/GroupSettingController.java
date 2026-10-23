package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.GroupSettingUpdate;
import com.zincoid.nullbot.console.dto.GroupSettingView;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import com.zincoid.nullbot.module.setting.GroupCmdRateLimiter;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制台群设置接口：读取与更新单个群的设置。
 * <p>Console group setting API: reads and updates the settings of one group.</p>
 */
@RestController
@RequestMapping("/console/groups/{groupId}/setting")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class GroupSettingController {

    private final GroupSettingService groupSettingService;
    private final GroupCmdRateLimiter groupCmdRateLimiter;

    // ════ Query ══════════════════

    /**
     * 读取群设置，缺失时创建默认设置。
     * <p>Returns the group settings, creating the default row when absent.</p>
     *
     * @param groupId platform group id
     * @return the group settings
     */
    @GetMapping
    public GroupSettingView get(@PathVariable String groupId) {
        return GroupSettingView.of(groupSettingService.get(
                ConsoleValidation.required(groupId, 64, "groupId")));
    }

    // ════ Update ═════════════════

    /**
     * 全量更新群设置。
     * <p>Fully updates the group settings.</p>
     *
     * @param groupId platform group id
     * @param body    the new settings
     * @return the updated settings
     */
    @PutMapping
    public GroupSettingView update(@PathVariable String groupId,
                                   @Valid @RequestBody GroupSettingUpdate body) {
        String id = ConsoleValidation.required(groupId, 64, "groupId");
        String prompt = body.aiPrompt() == null ? "" : body.aiPrompt().trim();
        GroupSetting setting = GroupSetting.builder()
                .groupId(id)
                .language(body.language())
                .joinNoticeEnabled(body.joinNoticeEnabled())
                .leaveNoticeEnabled(body.leaveNoticeEnabled())
                .limitEnabled(body.limitEnabled())
                .limitScope(body.limitScope())
                .limitCapacity(body.limitCapacity())
                .limitRefill(body.limitRefill())
                .limitInterval(body.limitInterval())
                .aiEffort(body.aiEffort())
                .aiPrompt(prompt)
                .aiInjectionGuard(body.aiInjectionGuard())
                .aiAutoChatEnabled(body.aiAutoChatEnabled())
                .aiChatFrequency(body.aiChatFrequency())
                .build();
        groupSettingService.update(setting);
        groupCmdRateLimiter.reset(id);
        return GroupSettingView.of(groupSettingService.get(id));
    }
}
