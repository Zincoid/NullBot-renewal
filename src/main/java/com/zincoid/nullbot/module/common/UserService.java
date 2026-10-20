package com.zincoid.nullbot.module.common;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.po.User;

/**
 * User record service: registration, rename, and access levels.
 * <p>用户记录服务：注册、改名与限权。</p>
 */
public interface UserService extends IService<User> {

    // ════ Register ════════════════

    /**
     * Registers a new user or refreshes the name of an existing one.
     * <p>注册新用户，或刷新已有用户的名称。</p>
     *
     * @param id   platform user ID
     * @param name current nickname
     * @return the persisted record
     */
    User register(String id, String name);

    // ════ Update ══════════════════

    /**
     * Renames the user.
     * <p>修改用户名称。</p>
     *
     * @param id   platform user ID
     * @param name new nickname
     */
    void update(String id, String name);

    // ════ Access ══════════════════

    /**
     * Returns the access level of the user.
     * <p>返回用户的限权等级。</p>
     *
     * @param id platform user ID
     * @return the access level
     */
    Access getAccess(String id);

    /**
     * Sets the access level of the user.
     * <p>设置用户的限权等级。</p>
     *
     * @param id     platform user ID
     * @param access new access level
     */
    void setAccess(String id, Access access);

    // ════ Delete ══════════════════

    /**
     * Deletes the user record.
     * <p>删除用户记录。</p>
     *
     * @param id platform user ID
     */
    void delete(String id);
}
