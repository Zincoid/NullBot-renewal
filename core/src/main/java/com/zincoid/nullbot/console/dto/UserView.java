package com.zincoid.nullbot.console.dto;

import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.po.User;

/**
 * 控制台用户视图。
 * <p>Console view of a user.</p>
 *
 * @param id                    the platform user id
 * @param name                  the nickname
 * @param access                the access level
 * @param privateMessageAllowed whether private messages are allowed
 */
public record UserView(String id, String name, Access access, boolean privateMessageAllowed) {

    /**
     * 由用户记录构建视图。
     * <p>Builds the view from a user record.</p>
     *
     * @param user the user record
     * @return the view
     */
    public static UserView of(User user) {
        return new UserView(user.getId(), user.getName(), user.getAccess(),
                user.getPrivateMessageAllowed());
    }
}
