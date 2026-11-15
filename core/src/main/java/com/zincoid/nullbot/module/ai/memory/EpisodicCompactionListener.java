package com.zincoid.nullbot.module.ai.memory;

import com.zincoid.nullbot.module.message.MessageTrimBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 情景压缩触发适配器：把窗口修剪事件转交给异步压缩。
 * <p>Episodic compaction trigger adapter: hands window trim events to asynchronous compaction.</p>
 */
@Component
@RequiredArgsConstructor
public class EpisodicCompactionListener {

    private final EpisodicMemory episodicMemory;

    /**
     * 异步压缩触发会话的已读消息。
     * <p>Asynchronously compacts the pending read messages of the triggering chat.</p>
     *
     * @param event the trim blocked event
     */
    @Async("AsyncRunner")
    @EventListener
    public void onTrimBlocked(MessageTrimBlockedEvent event) {
        episodicMemory.compact(event.chatId());
    }
}
