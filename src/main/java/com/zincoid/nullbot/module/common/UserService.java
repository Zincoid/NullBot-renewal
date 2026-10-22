package com.zincoid.nullbot.module.common;

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

    // ════ Delete ══════════════════

    /**
     * 删除用户记录。
     * <p>Deletes the user record.</p>
     *
     * @param id platform user id
     */
    void delete(String id);
}
