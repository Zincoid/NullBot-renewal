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

    /**
     * 判断用户是否已被封禁，用户记录不存在时返回 false。
     * <p>Whether the user is banned, returning false when the user record is absent.</p>
     *
     * @param id platform user id
     * @return true when the user is banned
     */
    boolean isBanned(String id);

    // ════ Permissions ═════════════

    /**
     * 返回用户是否允许私聊指令，未设置时视为不允许。
     * <p>Returns whether private commands are enabled for the user, treating unset as disabled.</p>
     *
     * @param id platform user id
     * @return true when private commands are enabled
     */
    boolean isPrivateCmdEnabled(String id);

    /**
     * 返回用户是否允许私聊 AI 对话，未设置时视为不允许。
     * <p>Returns whether private AI chat is enabled for the user, treating unset as disabled.</p>
     *
     * @param id platform user id
     * @return true when private AI chat is enabled
     */
    boolean isPrivateAiChatEnabled(String id);

    /**
     * 设置用户是否允许私聊指令。
     * <p>Sets whether private commands are enabled for the user.</p>
     *
     * @param id      platform user id
     * @param enabled whether private commands are enabled
     */
    void setPrivateCmdEnabled(String id, boolean enabled);

    /**
     * 设置用户是否允许私聊 AI 对话。
     * <p>Sets whether private AI chat is enabled for the user.</p>
     *
     * @param id      platform user id
     * @param enabled whether private AI chat is enabled
     */
    void setPrivateAiChatEnabled(String id, boolean enabled);

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
