package com.zincoid.nullbot.bot.bus;

import java.util.List;

/**
 * Parsed command request before the command is resolved.
 * <p>指令请求解析结果（尚未绑定指令）。</p>
 *
 * @param name   the command name
 * @param params the raw command parameters
 */
public record CmdRequest(
        String name,
        List<String> params
) {

    public CmdRequest {
        params = List.copyOf(params);
    }
}
