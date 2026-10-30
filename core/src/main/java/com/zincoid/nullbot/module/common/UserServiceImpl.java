package com.zincoid.nullbot.module.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.mapper.UserMapper;
import com.zincoid.nullbot.platform.persistence.po.User;
import com.zincoid.nullbot.platform.util.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * {@link UserService} 的默认实现。
 * <p>Default implementation of {@link UserService}.</p>
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // ════ Register ════════════════

    @Override
    public void register(String id, String name) {
        if (id == null || id.isBlank()) throw new CommonException("Missing key: id");
        if (name == null || name.isBlank()) throw new CommonException("Missing key: name");
        User user = getById(id);
        if (user == null) {
            user = new User(id, name);
            save(user);
            log.info("User registered: {}", id);
        } else if (!name.equals(user.getName())) {
            lambdaUpdate()
                    .eq(User::getId, id)
                    .set(User::getName, name)
                    .update();
        }
    }

    // ════ Update ══════════════════

    @Override
    public void update(String id, String name) {
        if (name == null)
            throw new CommonException("Missing name: " + id);
        boolean updated = lambdaUpdate()
                .eq(User::getId, id)
                .set(User::getName, name)
                .update();
        if (!updated) throw new CommonException("User not found: " + id);
    }

    // ════ Access ══════════════════

    @Override
    public Access getAccess(String id) {
        User user = getById(id);
        if (user == null) throw new CommonException("User not found: " + id);
        return user.getAccess();
    }

    @Override
    public void setAccess(String id, Access access) {
        if (access == null) throw new CommonException("Missing key: access");
        boolean updated = lambdaUpdate()
                .eq(User::getId, id)
                .set(User::getAccess, access)
                .update();
        if (!updated) throw new CommonException("User not found: " + id);
    }

    // ════ Private message ═════════

    @Override
    public boolean isPrivateMessageAllowed(String id) {
        User user = getById(id);
        if (user == null) throw new CommonException("User not found: " + id);
        return user.getPrivateMessageAllowed();
    }

    @Override
    public void setPrivateMessageAllowed(String id, boolean allowed) {
        boolean updated = lambdaUpdate()
                .eq(User::getId, id)
                .set(User::getPrivateMessageAllowed, allowed)
                .update();
        if (!updated) throw new CommonException("User not found: " + id);
    }

    // ════ Delete ══════════════════

    @Override
    public void delete(String id) {
        if (!removeById(id)) throw new CommonException("User not found: " + id);
        log.info("User deleted: {}", id);
    }

    // ════ Page ════════════════════

    @Override
    public Page<User> page(String keyword, long page, long size) {
        String key = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return lambdaQuery()
                .and(key != null, q -> q.like(User::getId, key).or().like(User::getName, key))
                .orderByAsc(User::getId)
                .page(PageUtils.of(page, size));
    }
}
