package com.zincoid.nullbot.module.setting;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.GroupKeyword;

import java.util.List;

/**
 * 群关键词规则服务：按群读取启用的关键词规则。
 * <p>Group keyword rule service: reads the enabled keyword rules of a group.</p>
 */
public interface GroupKeywordService extends IService<GroupKeyword> {

    // ════ List ════════════════════

    /**
     * 读取群内启用的关键词规则。
     * <p>Returns the enabled keyword rules of the group.</p>
     *
     * @param groupId platform group id
     * @return the enabled rules, oldest first
     */
    List<GroupKeyword> listEnabled(String groupId);

    // ════ Add ═════════════════════

    /**
     * 添加一条启用的关键词规则。
     * <p>Adds an enabled keyword rule.</p>
     *
     * @param groupId platform group id
     * @param keyword keyword to match, must not be blank
     * @param cmd     command name to dispatch, must not be blank
     * @param args    command arguments, may be empty
     * @return the persisted rule
     */
    GroupKeyword add(String groupId, String keyword, String cmd, List<String> args);

    // ════ Delete ══════════════════

    /**
     * 删除关键词规则。
     * <p>Deletes a keyword rule.</p>
     *
     * @param id rule id
     */
    void delete(Long id);

    // ════ Enabled ═════════════════

    /**
     * 启用关键词规则。
     * <p>Enables a keyword rule.</p>
     *
     * @param id rule id
     */
    void enable(Long id);

    /**
     * 禁用关键词规则。
     * <p>Disables a keyword rule.</p>
     *
     * @param id rule id
     */
    void disable(Long id);
}
