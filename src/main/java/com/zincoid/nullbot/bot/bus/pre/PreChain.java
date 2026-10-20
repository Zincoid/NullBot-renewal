package com.zincoid.nullbot.bot.bus.pre;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;

import java.util.Iterator;
import java.util.List;

/**
 * Iterator-based chain of responsibility over {@link PreHandler} stages.
 * <p>基于迭代器的 {@link PreHandler} 责任链。</p>
 */
public class PreChain {

    private final Iterator<PreHandler> iterator;
    private boolean consumed;

    public PreChain(List<PreHandler> handlers) {
        this.iterator = handlers.iterator();
    }

    /**
     * Runs the next handler of the chain.
     * <p>执行链条中的下一个处理器。</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     */
    public void doHandle(BotClient client, BotEvent event) {
        if (consumed) return;
        if (iterator.hasNext()) {
            iterator.next().handle(client, event, this);
        }
    }

    /**
     * Marks the event as consumed and stops the chain.
     * <p>标记事件已消费并中断链条。</p>
     */
    public void consume() {
        this.consumed = true;
    }

    /**
     * Whether the event was consumed.
     * <p>事件是否已被消费。</p>
     *
     * @return true when a handler consumed the event
     */
    public boolean isConsumed() {
        return consumed;
    }
}
