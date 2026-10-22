package com.zincoid.nullbot.module.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.po.Group;

/**
 * 群记录服务：注册、改名与限权。
 * <p>Group record service: registration, rename, and access levels.</p>
 */
public interface GroupService extends IService<Group> {

    // ════ Register ════════════════

    /**
     * 注册新群，或刷新已有群的名称。
     * <p>Registers a new group or refreshes the name of an existing one.</p>
     *
     * @param id   platform group id
     * @param name current group name
     */
    void register(String id, String name);

    // ════ Update ══════════════════

    /**
     * 修改群名称。
     * <p>Renames the group.</p>
     *
     * @param id   platform group id
     * @param name new group name
     */
    void update(String id, String name);

    // ════ Access ══════════════════

    /**
     * 返回群的限权等级。
     * <p>Returns the access level of the group.</p>
     *
     * @param id platform group id
     * @return the access level
     */
    Access getAccess(String id);

    /**
     * 设置群的限权等级。
     * <p>Sets the access level of the group.</p>
     *
     * @param id     platform group id
     * @param access new access level
     */
    void setAccess(String id, Access access);

    // ════ Delete ══════════════════

    /**
     * 删除群记录。
     * <p>Deletes the group record.</p>
     *
     * @param id platform group id
     */
    void delete(String id);

    // ════ Page ════════════════════

    /**
     * 按关键词分页查询群。
     * <p>Pages groups by keyword.</p>
     *
     * @param keyword keyword matched against id or name, nullable
     * @param page    one-based page number
     * @param size    page size
     * @return the group page
     */
    Page<Group> page(String keyword, long page, long size);
}
