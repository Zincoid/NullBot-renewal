package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.PageView;
import com.zincoid.nullbot.console.dto.UserSettingUpdate;
import com.zincoid.nullbot.console.dto.UserSettingView;
import com.zincoid.nullbot.console.dto.UserUpdate;
import com.zincoid.nullbot.console.dto.UserView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.setting.UserSettingService;
import com.zincoid.nullbot.platform.persistence.po.User;
import com.zincoid.nullbot.platform.persistence.po.UserSetting;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制台用户接口：分页查询、权限与用户设置修改、删除。
 * <p>Console user API: paged queries, access and user-setting updates, and deletion.</p>
 */
@RestController
@RequestMapping("/console/users")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSettingService userSettingService;

    // ════ Query ══════════════════

    /**
     * 分页查询用户。
     * <p>Pages users.</p>
     *
     * @param keyword keyword matched against id or name
     * @param page    one-based page number
     * @param size    page size
     * @return the user page
     */
    @GetMapping
    public PageView<UserView> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return PageView.of(userService.page(keyword,
                ConsoleValidation.page(page), ConsoleValidation.size(size)), UserView::of);
    }

    // ════ Update ═════════════════

    /**
     * 修改用户权限与私聊开关。
     * <p>Updates the user access level and private-chat flag.</p>
     *
     * @param id   platform user id
     * @param body the new access level and private-chat flag
     * @return the updated user
     */
    @PutMapping("/{id}")
    public UserView update(@PathVariable String id, @Valid @RequestBody UserUpdate body) {
        User user = userService.getById(id);
        if (user == null) throw new ConsoleException(HttpStatus.NOT_FOUND, "User not found: " + id);
        userService.setAccess(id, body.access());
        user.setAccess(body.access());
        if (body.allowPrivateChat() != null) {
            userService.setPrivateChatAllowed(id, body.allowPrivateChat());
            user.setAllowPrivateChat(body.allowPrivateChat());
        }
        return UserView.of(user);
    }

    // ════ Setting ════════════════

    /**
     * 查询用户设置。
     * <p>Returns the settings of a user.</p>
     *
     * @param id platform user id
     * @return the user settings
     */
    @GetMapping("/{id}/setting")
    public UserSettingView getSetting(@PathVariable String id) {
        User user = userService.getById(id);
        if (user == null) throw new ConsoleException(HttpStatus.NOT_FOUND, "User not found: " + id);
        return UserSettingView.of(id, userSettingService.get(id));
    }

    /**
     * 更新用户私聊提示词。
     * <p>Updates the user's private-chat prompt.</p>
     *
     * @param id   platform user id
     * @param body the new settings
     * @return the updated user settings
     */
    @PutMapping("/{id}/setting")
    public UserSettingView updateSetting(@PathVariable String id, @Valid @RequestBody UserSettingUpdate body) {
        User user = userService.getById(id);
        if (user == null) throw new ConsoleException(HttpStatus.NOT_FOUND, "User not found: " + id);
        String prompt = body.aiPrompt() == null ? "" : body.aiPrompt().trim();
        userSettingService.update(UserSetting.builder().userId(id).aiPrompt(prompt).build());
        return UserSettingView.of(id, userSettingService.get(id));
    }

    // ════ Delete ═════════════════

    /**
     * 删除用户记录。
     * <p>Deletes a user record.</p>
     *
     * @param id platform user id
     * @return an empty response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        User user = userService.getById(id);
        if (user == null) throw new ConsoleException(HttpStatus.NOT_FOUND, "User not found: " + id);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
