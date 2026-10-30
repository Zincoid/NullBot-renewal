package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdRegistry;
import com.zincoid.nullbot.console.dto.CmdDisableView;
import com.zincoid.nullbot.console.dto.GroupCmdDisableCreate;
import com.zincoid.nullbot.console.dto.PrivateCmdDisableCreate;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import com.zincoid.nullbot.module.setting.CmdDisableService;
import com.zincoid.nullbot.platform.persistence.po.CmdDisable;
import com.zincoid.nullbot.platform.util.DurationUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * 控制台指令禁用接口：分别管理群聊与私聊规则。
 * <p>Console command-disable API: manages group-chat and private-chat rules separately.</p>
 */
@RestController
@RequestMapping("/console")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class CmdDisableController {

    private final CmdDisableService cmdDisableService;
    private final CmdRegistry cmdRegistry;

    // ════ Group ═══════════════════

    /**
     * 列出群内生效的禁用规则。
     * <p>Lists the active disable rules of a group.</p>
     *
     * @param groupId platform group id
     * @return the active rules
     */
    @GetMapping("/groups/{groupId}/cmd-disables")
    public List<CmdDisableView> listGroup(@PathVariable String groupId) {
        String gid = ConsoleValidation.required(groupId, 64, "groupId");
        return cmdDisableService.listGroup(gid).stream().map(CmdDisableView::of).toList();
    }

    /**
     * 添加或更新群内禁用规则。
     * <p>Adds or updates a group disable rule.</p>
     *
     * @param groupId platform group id
     * @param body    the new rule; {@code userId = *} means the whole group
     * @return the stored rule
     */
    @PostMapping("/groups/{groupId}/cmd-disables")
    public CmdDisableView addGroup(@PathVariable String groupId,
                                   @Valid @RequestBody GroupCmdDisableCreate body) {
        String gid = ConsoleValidation.required(groupId, 64, "groupId");
        String uid = ConsoleValidation.required(body.userId(), 64, "userId");
        CmdDisable rule = cmdDisableService.addGroup(
                gid, uid, command(body.cmd()), parseDuration(body.duration()));
        return CmdDisableView.of(rule);
    }

    /**
     * 解除一条群内禁用规则。
     * <p>Removes a group disable rule.</p>
     *
     * @param groupId platform group id
     * @param cmd     the canonical command name
     * @param userId  the target user id, {@code *} for the whole group
     * @return an empty response
     */
    @DeleteMapping("/groups/{groupId}/cmd-disables/{cmd}")
    public ResponseEntity<Void> removeGroup(@PathVariable String groupId,
                                            @PathVariable String cmd,
                                            @RequestParam(required = false) String userId) {
        String gid = ConsoleValidation.required(groupId, 64, "groupId");
        String uid = ConsoleValidation.required(userId, 64, "userId");
        String name = ConsoleValidation.required(cmd, 100, "cmd");
        if (!cmdDisableService.removeGroup(gid, uid, name))
            throw new ConsoleException(HttpStatus.NOT_FOUND, "Disable rule not found: " + name);
        return ResponseEntity.noContent().build();
    }

    // ════ Private ═════════════════

    /**
     * 列出用户私聊中生效的禁用规则。
     * <p>Lists the active disable rules of a private chat.</p>
     *
     * @param userId platform user id
     * @return the active rules
     */
    @GetMapping("/users/{userId}/cmd-disables")
    public List<CmdDisableView> listPrivate(@PathVariable String userId) {
        String uid = ConsoleValidation.required(userId, 64, "userId");
        return cmdDisableService.listPrivate(uid).stream().map(CmdDisableView::of).toList();
    }

    /**
     * 添加或更新私聊禁用规则。
     * <p>Adds or updates a private disable rule.</p>
     *
     * @param userId platform user id
     * @param body   the new rule
     * @return the stored rule
     */
    @PostMapping("/users/{userId}/cmd-disables")
    public CmdDisableView addPrivate(@PathVariable String userId,
                                     @Valid @RequestBody PrivateCmdDisableCreate body) {
        String uid = ConsoleValidation.required(userId, 64, "userId");
        CmdDisable rule = cmdDisableService.addPrivate(
                uid, command(body.cmd()), parseDuration(body.duration()));
        return CmdDisableView.of(rule);
    }

    /**
     * 解除一条私聊禁用规则。
     * <p>Removes a private disable rule.</p>
     *
     * @param userId platform user id
     * @param cmd    the canonical command name
     * @return an empty response
     */
    @DeleteMapping("/users/{userId}/cmd-disables/{cmd}")
    public ResponseEntity<Void> removePrivate(@PathVariable String userId,
                                              @PathVariable String cmd) {
        String uid = ConsoleValidation.required(userId, 64, "userId");
        String name = ConsoleValidation.required(cmd, 100, "cmd");
        if (!cmdDisableService.removePrivate(uid, name))
            throw new ConsoleException(HttpStatus.NOT_FOUND, "Disable rule not found: " + name);
        return ResponseEntity.noContent().build();
    }

    // ════ Helpers ═════════════════

    private String command(String raw) {
        String name = ConsoleValidation.required(raw, 100, "cmd");
        Cmd cmd = cmdRegistry.get(name);
        if (cmd == null) throw new ConsoleException(HttpStatus.BAD_REQUEST, "Unknown command: " + name);
        return cmd.name();
    }

    private static Duration parseDuration(String raw) {
        String value = ConsoleValidation.text(raw, 20, "duration");
        if (value == null) return null;
        Duration duration = DurationUtils.parse(value);
        if (duration == null)
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Malformed duration: " + value);
        return duration;
    }
}
