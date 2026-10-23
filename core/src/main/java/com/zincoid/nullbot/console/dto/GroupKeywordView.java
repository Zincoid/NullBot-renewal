package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.po.GroupKeyword;

import java.util.List;

/**
 * 控制台群关键词规则视图。
 * <p>Console view of a group keyword rule.</p>
 *
 * @param id      the rule id
 * @param keyword the keyword to match
 * @param cmd     the command name to dispatch
 * @param args    the command arguments
 * @param enabled whether the rule is enabled
 */
public record GroupKeywordView(Long id, String keyword, String cmd, List<String> args, boolean enabled) {

    /**
     * 由关键词规则构建视图。
     * <p>Builds the view from a keyword rule.</p>
     *
     * @param rule the keyword rule
     * @return the view
     */
    public static GroupKeywordView of(GroupKeyword rule) {
        return new GroupKeywordView(
                rule.getId(),
                rule.getKeyword(),
                rule.getCmd(),
                rule.getArgs() == null ? List.of() : rule.getArgs(),
                rule.getEnabled());
    }
}
