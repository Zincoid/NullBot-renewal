package com.zincoid.nullbot.module.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.po.User;

/**
 * 用户记录服务：注册、改名与限权。
 * <p>User record service: registration, rename, and access levels.</p>
 */
public interface UserService extends IService<User> {

    // ════ Register ════════════════

    /**
     * 注册新用户，或刷新已有用户的名称。
     * <p>Registers a new user or refreshes the name of an existing one.</p>
     *
     * @param id   platform user id
     * @param name current nickname
     */
    void register(String id, String name);

    // ════ Update ══════════════════

    /**
     * 修改用户名称。
     * <p>Renames the user.</p>
     *
     * @param id   platform user id
     * @param name new nickname
     */
    void update(String id, String name);

    // ════ Access ══════════════════

    /**
     * 返回用户的限权等级。
     * <p>Returns the access level of the user.</p>
     *
     * @param id platform user id
     * @return the access level
     */
    Access getAccess(String id);

    /**
     * 设置用户的限权等级。
     * <p>Sets the access level of the user.</p>
     *
     * @param id     platform user id
     * @param access new access level
     */
    void setAccess(String id, Access access);

    // ════ Private chat ════════════

    /**
     * 返回用户是否允许私聊，未设置时视为不允许。
     * <p>Returns whether the user may chat privately, treating unset as disallowed.</p>
     *
     * @param id platform user id
     * @return true when private chat is allowed
     */
    boolean isPrivateChatAllowed(String id);

    /**
     * 设置用户是否允许私聊。
     * <p>Sets whether the user may chat privately.</p>
     *
     * @param id      platform user id
     * @param allowed whether private chat is allowed
     */
    void setPrivateChatAllowed(String id, boolean allowed);

    // ════ Delete ══════════════════

    /**
     * 删除用户记录。
     * <p>Deletes the user record.</p>
     *
     * @param id platform user id
     */
    void delete(String id);

    // ════ Page ════════════════════

    /**
     * 按关键词分页查询用户。
     * <p>Pages users by keyword.</p>
     *
     * @param keyword keyword matched against id or name, nullable
     * @param page    one-based page number
     * @param size    page size
     * @return the user page
     */
    Page<User> page(String keyword, long page, long size);
}
