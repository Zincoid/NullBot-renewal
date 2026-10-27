package com.zincoid.nullbot.adapter.qq;

import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * QQ 消息索引：短 ID、平台 ID、目标会话、消息详情与昵称的有限缓存。
 * <p>QQ message index: a bounded cache of short ids, platform ids, targets, message details, and usernames.</p>
 * <p>同时用于重复消息去重，以及为缺少会话参数的 recall/getMsg 补齐目标。</p>
 * <p>Also used to deduplicate messages and to supply conversation context to recall/getMsg.</p>
 */
@Component
public class QqMessageIndex {

    private static final int MAX_ENTRIES = 2048;
    private static final int MAX_USERNAMES = 2048;

    private final Map<String, MessageEntry> entries = Collections.synchronizedMap(
            new LinkedHashMap<String, MessageEntry>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, MessageEntry> eldest) {
                    return size() > MAX_ENTRIES;
                }
            });
    private final Map<String, String> usernames = Collections.synchronizedMap(
            new LinkedHashMap<String, String>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, String> eldest) {
                    return size() > MAX_USERNAMES;
                }
            });

    /**
     * 一条消息索引。
     * <p>One message index entry.</p>
     *
     * @param shortId  the internal short id
     * @param actualId the platform message id
     * @param target   the target conversation
     * @param detail   the cached message detail, nullable
     */
    public record MessageEntry(String shortId, String actualId, QqTarget target, MsgDetail detail) {
    }

    /**
     * 记录一条入站消息，并判断是否为重复消息。
     * <p>Records an inbound message and reports whether it is a duplicate.</p>
     *
     * @param actualId the platform message id
     * @param target   the target conversation
     * @param detail   the message detail
     * @return true when recorded, false when already present
     */
    public boolean remember(String actualId, QqTarget target, MsgDetail detail) {
        String shortId = QqIds.shortId(actualId);
        synchronized (entries) {
            if (entries.containsKey(shortId)) return false;
            entries.put(shortId, new MessageEntry(shortId, actualId, target, detail));
            return true;
        }
    }

    /**
     * 记录一条出站消息并返回短 ID。
     * <p>Records an outbound message and returns its short id.</p>
     *
     * @param actualId the platform message id
     * @param target   the target conversation
     * @return the short message id
     */
    public String rememberSent(String actualId, QqTarget target) {
        String shortId = QqIds.shortId(actualId);
        entries.put(shortId, new MessageEntry(shortId, actualId, target, null));
        return shortId;
    }

    /**
     * 按短 ID 或平台 ID 查询索引。
     * <p>Looks up an entry by short id or platform id.</p>
     *
     * @param id the short or platform message id
     * @return the entry, or null when absent
     */
    public MessageEntry resolve(String id) {
        if (id == null || id.isBlank()) return null;
        MessageEntry entry = entries.get(id);
        if (entry != null) return entry;
        return entries.get(QqIds.shortId(id));
    }

    /**
     * 记录用户昵称。
     * <p>Records a username.</p>
     *
     * @param userId the platform user id
     * @param name   the username
     */
    public void rememberUser(String userId, String name) {
        if (userId == null || userId.isBlank() || name == null || name.isBlank()) return;
        usernames.put(userId, name);
    }

    /**
     * 查询用户昵称。
     * <p>Looks up a username.</p>
     *
     * @param userId the platform user id
     * @return the username, or null when absent
     */
    public String username(String userId) {
        return userId == null ? null : usernames.get(userId);
    }
}
