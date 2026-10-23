package com.zincoid.nullbot.module.ai.tool.impl;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.platform.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 中间回复工具：在最终回复前向当前会话发送文本或媒体（图片、视频、音频）。
 * <p>Intermediate reply tool: sends text or media (image, video, audio) to the current chat before the final reply.</p>
 */
@Component
public class Send implements Tool {

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("send", """
                        Sends an intermediate message to the current chat before the final reply.
                        Set file to attach an image, video, or audio by file id.""")
                .addString("content", "message text, optional when file is set")
                .addInteger("file", "file id of an image, video, or audio to send, optional")
                .addString("reply", "message id to quote, optional")
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        String content = _args.content == null ? "" : _args.content;
        if (content.isBlank() && _args.file == null)
            return ToolRes.error("Missing content or file");
        List<Long> files = _args.file == null ? List.of() : List.of(_args.file);
        return ctx.send(content, files, _args.reply);
    }

    private record Args(String content, Long file, String reply) {
    }
}
