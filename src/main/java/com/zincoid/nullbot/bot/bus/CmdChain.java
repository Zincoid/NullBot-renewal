package com.zincoid.nullbot.bot.bus;

import com.zincoid.nullbot.bot.cmd.CmdContext;

import java.util.Iterator;
import java.util.List;

/**
 * Iterator-based chain of responsibility over {@link CmdHandler} stages.
 * <p>基于迭代器的 {@link CmdHandler} 责任链。</p>
 */
public class CmdChain {

    private final Iterator<CmdHandler> iterator;

    public CmdChain(List<CmdHandler> handlers) {
        this.iterator = handlers.iterator();
    }

    /**
     * Runs the next handler of the chain.
     * <p>执行链条中的下一个处理器。</p>
     *
     * @param ctx the command context
     */
    public void doHandle(CmdContext ctx) {
        if (iterator.hasNext()) {
            iterator.next().handle(ctx, this);
        }
    }
}
