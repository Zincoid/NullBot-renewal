package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdContext;

import java.util.Iterator;
import java.util.List;

/**
 * 基于迭代器的 {@link CmdHandler} 责任链。
 * <p>Iterator-based chain of responsibility over {@link CmdHandler} stages.</p>
 */
public class CmdChain {

    private final Iterator<CmdHandler> iterator;

    public CmdChain(List<CmdHandler> handlers) {
        this.iterator = handlers.iterator();
    }

    /**
     * 执行链条中的下一个处理器。
     * <p>Runs the next handler of the chain.</p>
     *
     * @param ctx the command context
     */
    public void doHandle(CmdContext ctx) {
        if (iterator.hasNext()) {
            iterator.next().handle(ctx, this);
        }
    }
}
