package com.zincoid.nullbot.module.common;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.po.Group;

/**
 * Group record service: registration, rename, and access levels.
 * <p>群记录服务：注册、改名与限权。</p>
 */
public interface GroupService extends IService<Group> {

    // ════ Register ════════════════

    /**
     * Registers a new group or refreshes the name of an existing one.
     * <p>注册新群，或刷新已有群的名称。</p>
     *
     * @param id   platform group ID
     * @param name current group name
     * @return the persisted record
     */
    Group register(String id, String name);

    // ════ Update ══════════════════

    /**
     * Renames the group.
     * <p>修改群名称。</p>
     *
     * @param id   platform group ID
     * @param name new group name
     */
    void update(String id, String name);

    // ════ Access ══════════════════

    /**
     * Returns the access level of the group.
     * <p>返回群的限权等级。</p>
     *
     * @param id platform group ID
     * @return the access level
     */
    Access getAccess(String id);

    /**
     * Sets the access level of the group.
     * <p>设置群的限权等级。</p>
     *
     * @param id     platform group ID
     * @param access new access level
     */
    void setAccess(String id, Access access);

    // ════ Delete ══════════════════

    /**
     * Deletes the group record.
     * <p>删除群记录。</p>
     *
     * @param id platform group ID
     */
    void delete(String id);
}
