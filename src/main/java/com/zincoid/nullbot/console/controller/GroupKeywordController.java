package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.GroupKeywordCreate;
import com.zincoid.nullbot.console.dto.GroupKeywordUpdate;
import com.zincoid.nullbot.console.dto.GroupKeywordView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import com.zincoid.nullbot.module.setting.GroupKeywordService;
import com.zincoid.nullbot.module.setting.SettingException;
import com.zincoid.nullbot.platform.persistence.po.GroupKeyword;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 控制台群关键词规则接口：查询、新增、修改、启停与删除。
 * <p>Console group keyword API: queries, creation, updates, enabling, and deletion.</p>
 */
@RestController
@RequestMapping("/console/groups/{groupId}/keywords")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class GroupKeywordController {

    private final GroupKeywordService groupKeywordService;

    // ════ Query ══════════════════

    /**
     * 列出群内全部关键词规则。
     * <p>Lists all keyword rules of the group.</p>
     *
     * @param groupId platform group id
     * @return the keyword rules
     */
    @GetMapping
    public List<GroupKeywordView> list(@PathVariable String groupId) {
        return groupKeywordService.listAll(groupId).stream().map(GroupKeywordView::of).toList();
    }

    // ════ Create ═════════════════

    /**
     * 新增关键词规则。
     * <p>Creates a keyword rule.</p>
     *
     * @param groupId platform group id
     * @param body    the new rule
     * @return the created rule
     */
    @PostMapping
    public ResponseEntity<GroupKeywordView> add(@PathVariable String groupId,
                                                @Valid @RequestBody GroupKeywordCreate body) {
        String id = ConsoleValidation.required(groupId, 64, "groupId");
        String keyword = body.keyword().trim();
        String cmd = body.cmd().trim();
        List<String> args = args(body.args());
        try {
            GroupKeyword rule = groupKeywordService.add(id, keyword, cmd, args);
            return ResponseEntity.status(HttpStatus.CREATED).body(GroupKeywordView.of(rule));
        } catch (SettingException e) {
            throw new ConsoleException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    // ════ Update ═════════════════

    /**
     * 修改关键词规则。
     * <p>Updates a keyword rule.</p>
     *
     * @param groupId platform group id
     * @param id      rule id
     * @param body    the new rule
     * @return the updated rule
     */
    @PutMapping("/{id}")
    public GroupKeywordView update(@PathVariable String groupId,
                                   @PathVariable Long id,
                                   @Valid @RequestBody GroupKeywordUpdate body) {
        rule(groupId, id);
        String keyword = body.keyword().trim();
        String cmd = body.cmd().trim();
        List<String> args = args(body.args());
        try {
            return GroupKeywordView.of(groupKeywordService.update(id, keyword, cmd, args));
        } catch (SettingException e) {
            throw new ConsoleException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    // ════ Enable ═════════════════

    /**
     * 启用关键词规则。
     * <p>Enables a keyword rule.</p>
     *
     * @param groupId platform group id
     * @param id      rule id
     * @return an empty response
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable String groupId, @PathVariable Long id) {
        rule(groupId, id);
        groupKeywordService.enable(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 禁用关键词规则。
     * <p>Disables a keyword rule.</p>
     *
     * @param groupId platform group id
     * @param id      rule id
     * @return an empty response
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable String groupId, @PathVariable Long id) {
        rule(groupId, id);
        groupKeywordService.disable(id);
        return ResponseEntity.noContent().build();
    }

    // ════ Delete ═════════════════

    /**
     * 删除关键词规则。
     * <p>Deletes a keyword rule.</p>
     *
     * @param groupId platform group id
     * @param id      rule id
     * @return an empty response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String groupId, @PathVariable Long id) {
        rule(groupId, id);
        groupKeywordService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ════ Helpers ═════════════════

    private GroupKeyword rule(String groupId, Long id) {
        GroupKeyword rule = groupKeywordService.getById(id);
        if (rule == null || !groupId.equals(rule.getGroupId()))
            throw new ConsoleException(HttpStatus.NOT_FOUND, "Keyword not found: " + id);
        return rule;
    }

    private static List<String> args(List<String> raw) {
        if (raw == null) return null;
        List<String> args = raw.stream()
                .filter(arg -> arg != null && !arg.isBlank())
                .map(String::trim)
                .toList();
        return args.isEmpty() ? null : args;
    }
}
