package com.zincoid.nullbot.adapter.qq;

import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * QQ 消息索引：短 ID、平台 ID、场景索引、目标会话、消息详情与昵称的有限缓存。
 * <p>QQ message index: a bounded cache of short ids, platform ids, scene indexes, targets, message details, and usernames.</p>
 * <p>同时用于重复消息去重，以及为缺少会话参数的 recall/getMsg 补齐目标。</p>
 * <p>Also used to deduplicate messages and to supply conversation context to recall/getMsg.</p>
 */
@Component
public class QqMessageIndex {

    private static final int MAX_ENTRIES = 2048;
    private static final int MAX_USERNAMES = 2048;

    private final Map<String, MessageEntry> entries = lru(MAX_ENTRIES);
    private final Map<String, String> sceneIndexes = lru(MAX_ENTRIES);
    private final Map<String, String> sceneRefs = lru(MAX_ENTRIES);
    private final Map<String, String> usernames = lru(MAX_USERNAMES);

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
     * 记录一条被引用消息的详情，覆盖可能已有的同索引条目。
     * <p>Records the detail of a quoted message, replacing any existing entry with the same index.</p>
     *
     * @param actualId the quote index or platform message id
     * @param target   the target conversation
     * @param detail   the quoted message detail
     */
    public void rememberQuoted(String actualId, QqTarget target, MsgDetail detail) {
        String shortId = QqIds.shortId(actualId);
        entries.put(shortId, new MessageEntry(shortId, actualId, target, detail));
        if (actualId.startsWith("REFIDX_") || actualId.startsWith("TMP_"))
            sceneRefs.put(shortId, actualId);
    }

    /**
     * 记录场景索引到消息短 ID 的映射，供后续引用消息回查。
     * <p>Maps a scene message index to an internal message short id for later quote lookups.</p>
     *
     * @param index          the scene message index
     * @param messageShortId the internal short id of the message
     */
    public void rememberIndex(String index, String messageShortId) {
        if (index == null || index.isBlank()
                || messageShortId == null || messageShortId.isBlank()) return;
        sceneIndexes.put(QqIds.shortId(index), messageShortId);
        sceneRefs.put(messageShortId, index);
    }

    /**
     * 按消息场景索引与会话查询详情。
     * <p>Looks up a message detail by its scene index and conversation.</p>
     *
     * @param index  the scene message index
     * @param target the target conversation
     * @return the message detail, or null when absent or from another conversation
     */
    public MsgDetail detailByIndex(String index, QqTarget target) {
        if (index == null || index.isBlank() || target == null) return null;
        MessageEntry entry = entryByScene(index);
        if (entry == null || !target.equals(entry.target())) return null;
        return entry.detail();
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
        return rememberSent(actualId, target, null);
    }

    /**
     * 记录一条出站消息及其引用索引，并返回短 ID。
     * <p>Records an outbound message with its reference index and returns the short id.</p>
     *
     * @param actualId the platform message id
     * @param target   the target conversation
     * @param sceneRef the reference index for later quotes, nullable
     * @return the short message id
     */
    public String rememberSent(String actualId, QqTarget target, String sceneRef) {
        String shortId = QqIds.shortId(actualId);
        entries.put(shortId, new MessageEntry(shortId, actualId, target, null));
        if (sceneRef != null && !sceneRef.isBlank()) sceneRefs.put(shortId, sceneRef);
        return shortId;
    }

    /**
     * 按短 ID、平台 ID 或场景索引查询索引。
     * <p>Looks up an entry by short id, platform id, or scene index.</p>
     *
     * @param id the short, platform, or scene message id
     * @return the entry, or null when absent
     */
    public MessageEntry resolve(String id) {
        if (id == null || id.isBlank()) return null;
        String shortId = QqIds.shortId(id);
        String mapped = sceneIndexes.get(id);
        if (mapped == null) mapped = sceneIndexes.get(shortId);
        if (mapped != null) {
            MessageEntry aliased = entries.get(mapped);
            if (aliased != null) return aliased;
        }
        MessageEntry entry = entries.get(id);
        return entry != null ? entry : entries.get(shortId);
    }

    /**
     * 按消息 ID 查询可用于引用回复的场景索引。
     * <p>Looks up the scene index used for quoting by a message id.</p>
     *
     * @param id the short, platform, or scene message id
     * @return the scene index, or null when absent
     */
    public String sceneIndex(String id) {
        MessageEntry entry = resolve(id);
        return entry == null ? null : sceneRefs.get(entry.shortId());
    }

    private MessageEntry entryByScene(String index) {
        String key = QqIds.shortId(index);
        String mapped = sceneIndexes.get(key);
        if (mapped != null) {
            MessageEntry entry = entries.get(mapped);
            if (entry != null) return entry;
        }
        return entries.get(key);
    }

    private static <V> Map<String, V> lru(int maxEntries) {
        return Collections.synchronizedMap(new LinkedHashMap<String, V>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                return size() > maxEntries;
            }
        });
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
