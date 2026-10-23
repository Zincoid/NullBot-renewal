package com.zincoid.nullbot.bot.cmd.chain;

import com.zincoid.nullbot.bot.cmd.CmdContext;

/**
 * 指令处理器链的一个环节。
 * <p>One stage of the command handler chain.</p>
 */
public interface CmdHandler {

    /**
     * 处理指令，可按需把请求交给链条后续环节。
     * <p>Handles the command, optionally delegating to the remaining chain.</p>
     *
     * @param ctx   the command context
     * @param chain the remaining chain
     */
    void handle(CmdContext ctx, CmdChain chain);
}
