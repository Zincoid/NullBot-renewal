package com.zincoid.nullbot.module.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.mapper.GroupMapper;
import com.zincoid.nullbot.platform.persistence.po.Group;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * {@link GroupService} 的默认实现。
 * <p>Default implementation of {@link GroupService}.</p>
 */
@Slf4j
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    // ════ Register ════════════════

    @Override
    public void register(String id, String name) {
        if (id == null || id.isBlank()) throw new CommonException("Missing key: id");
        if (name == null || name.isBlank()) throw new CommonException("Missing key: name");
        Group group = getById(id);
        if (group == null) {
            group = new Group(id, name);
            save(group);
            log.info("Group registered: {}", id);
        } else if (!name.equals(group.getName())) {
            lambdaUpdate()
                    .eq(Group::getId, id)
                    .set(Group::getName, name)
                    .update();
            group.setName(name);
        }
    }

    // ════ Update ══════════════════

    @Override
    public void update(String id, String name) {
        if (name == null || name.isBlank())
            throw new CommonException("Missing name: " + id);
        boolean updated = lambdaUpdate()
                .eq(Group::getId, id)
                .set(Group::getName, name)
                .update();
        if (!updated) throw new CommonException("Group not found: " + id);
    }

    // ════ Access ══════════════════

    @Override
    public Access getAccess(String id) {
        Group group = getById(id);
        if (group == null) throw new CommonException("Group not found: " + id);
        return group.getAccess();
    }

    @Override
    public void setAccess(String id, Access access) {
        if (access == null) throw new CommonException("Missing key: access");
        boolean updated = lambdaUpdate()
                .eq(Group::getId, id)
                .set(Group::getAccess, access)
                .update();
        if (!updated) throw new CommonException("Group not found: " + id);
    }

    // ════ Delete ══════════════════

    @Override
    public void delete(String id) {
        if (!removeById(id)) throw new CommonException("Group not found: " + id);
        log.info("Group deleted: {}", id);
    }

    // ════ Page ════════════════════

    @Override
    public Page<Group> page(String keyword, long page, long size) {
        long current = Math.max(1, page);
        long limit = Math.clamp(size, 1, 100);
        String key = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return lambdaQuery()
                .and(key != null, q -> q.like(Group::getId, key).or().like(Group::getName, key))
                .orderByAsc(Group::getId)
                .page(new Page<>(current, limit));
    }
}
