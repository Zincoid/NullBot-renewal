package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.po.Group;

/**
 * 控制台群视图。
 * <p>Console view of a group.</p>
 *
 * @param id     the platform group id
 * @param name   the group name
 * @param access the access level
 */
public record GroupView(String id, String name, Access access) {

    /**
     * 由群记录构建视图。
     * <p>Builds the view from a group record.</p>
     *
     * @param group the group record
     * @return the view
     */
    public static GroupView of(Group group) {
        return new GroupView(group.getId(), group.getName(), group.getAccess());
    }
}
