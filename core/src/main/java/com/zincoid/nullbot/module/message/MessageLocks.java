package com.zincoid.nullbot.module.message;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 消息会话锁：把同一会话的已读标记、窗口修剪与情景压缩串行化。
 * <p>Message locks: serializes read marking, window trimming, and episodic compaction per chat.</p>
 */
@Component
public class MessageLocks {

    private static final int LOCK_STRIPES = 64;

    private final ReentrantLock[] locks = initLocks();

    /**
     * 返回会话对应的锁。
     * <p>Returns the lock for the chat.</p>
     *
     * @param chatId chat identifier
     * @return the striped lock
     */
    public ReentrantLock get(String chatId) {
        int hash = chatId == null ? 0 : chatId.hashCode();
        return locks[Math.floorMod(hash, locks.length)];
    }

    /**
     * 创建固定数量的条带锁。
     * <p>Creates the fixed set of striped locks.</p>
     *
     * @return the striped locks
     */
    private static ReentrantLock[] initLocks() {
        ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];
        for (int i = 0; i < locks.length; i++) locks[i] = new ReentrantLock();
        return locks;
    }
}
