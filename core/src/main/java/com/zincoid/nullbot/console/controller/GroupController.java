package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.GroupUpdate;
import com.zincoid.nullbot.console.dto.GroupView;
import com.zincoid.nullbot.console.dto.PageView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.platform.persistence.po.Group;
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
 * 控制台群接口：分页查询、权限修改与删除。
 * <p>Console group API: paged queries, access updates, and deletion.</p>
 */
@RestController
@RequestMapping("/console/groups")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // ════ Query ══════════════════

    /**
     * 分页查询群。
     * <p>Pages groups.</p>
     *
     * @param keyword keyword matched against id or name
     * @param page    one-based page number
     * @param size    page size
     * @return the group page
     */
    @GetMapping
    public PageView<GroupView> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return PageView.of(groupService.page(keyword,
                ConsoleValidation.page(page), ConsoleValidation.size(size)), GroupView::of);
    }

    // ════ Update ═════════════════

    /**
     * 修改群权限。
     * <p>Updates the group access level.</p>
     *
     * @param id   platform group id
     * @param body the new access level
     * @return the updated group
     */
    @PutMapping("/{id}")
    public GroupView update(@PathVariable String id, @Valid @RequestBody GroupUpdate body) {
        Group group = groupService.getById(id);
        if (group == null) throw new ConsoleException(HttpStatus.NOT_FOUND, "Group not found: " + id);
        groupService.setAccess(id, body.access());
        group.setAccess(body.access());
        return GroupView.of(group);
    }

    // ════ Delete ═════════════════

    /**
     * 删除群记录。
     * <p>Deletes a group record.</p>
     *
     * @param id platform group id
     * @return an empty response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Group group = groupService.getById(id);
        if (group == null) throw new ConsoleException(HttpStatus.NOT_FOUND, "Group not found: " + id);
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
