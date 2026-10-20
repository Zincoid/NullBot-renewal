package com.zincoid.nullbot.bot.bus.cmd;

import com.zincoid.nullbot.bot.cmd.CmdContext;

/**
 * One stage of the command handler chain.
 * <p>指令处理器链的一个环节。</p>
 */
public interface CmdHandler {

    /**
     * Handles the command, optionally delegating to the remaining chain.
     * <p>处理指令，可按需把请求交给链条后续环节。</p>
     *
     * @param ctx   the command context
     * @param chain the remaining chain
     */
    void handle(CmdContext ctx, CmdChain chain);
}
