package com.zincoid.nullbot.bot.bus.middleware;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import lombok.Getter;

import java.util.Iterator;
import java.util.List;

/**
 * 基于迭代器的 {@link Middleware} 责任链。
 * <p>Iterator-based chain of responsibility over {@link Middleware} stages.</p>
 */
public class MiddlewareChain {

    private final Iterator<Middleware> iterator;

    /**
     * 事件是否已被消费。
     * <p>Whether the event was consumed.</p>
     */
    @Getter
    private boolean consumed;

    public MiddlewareChain(List<Middleware> middlewares) {
        this.iterator = middlewares.iterator();
    }

    /**
     * 标记事件已消费并中断链条。
     * <p>Marks the event as consumed and stops the chain.</p>
     */
    public void consume() {
        this.consumed = true;
    }

    /**
     * 运行中间件链并返回事件是否被消费。
     * <p>Runs the middleware chain and returns whether the event was consumed.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @return true when the event was consumed
     */
    public boolean process(BotClient client, BotEvent event) {
        doHandle(client, event);
        return consumed;
    }

    /**
     * 执行链条中的下一个中间件。
     * <p>Runs the next middleware of the chain.</p>
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
}
