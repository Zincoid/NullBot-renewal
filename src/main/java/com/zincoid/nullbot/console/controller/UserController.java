package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.PageView;
import com.zincoid.nullbot.console.dto.UserUpdate;
import com.zincoid.nullbot.console.dto.UserView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.platform.persistence.po.User;
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
 * 控制台用户接口：分页查询、权限修改与删除。
 * <p>Console user API: paged queries, access updates, and deletion.</p>
 */
@RestController
@RequestMapping("/console/users")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
     * 修改用户权限。
     * <p>Updates the user access level.</p>
     *
     * @param id   platform user id
     * @param body the new access level
     * @return the updated user
     */
    @PutMapping("/{id}")
    public UserView update(@PathVariable String id, @Valid @RequestBody UserUpdate body) {
        User user = userService.getById(id);
        if (user == null) throw new ConsoleException(HttpStatus.NOT_FOUND, "User not found: " + id);
        userService.setAccess(id, body.access());
        return UserView.of(userService.getById(id));
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
