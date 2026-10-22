package com.zincoid.nullbot.bot.bus;

import java.util.List;

/**
 * 指令请求解析结果（尚未绑定指令）。
 * <p>Parsed command request before the command is resolved.</p>
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
