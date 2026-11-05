package com.zincoid.nullbot.bot.aichat.tool;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.platform.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息发送工具：在最终回复前向当前会话发送文本或媒体（图片、视频、音频），也可标记为本轮最终消息。
 * <p>Message tool: sends text or media (image, video, audio) to the current chat before the final reply, or marks the message as the final one.</p>
 */
@Component
public class Send implements Tool {

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("send", """
                        Sends an additional message to the current chat before the final reply; you may call it multiple times.
                        Set last=true when this message completes your answer; no separate final reply will be sent.
                        Set file to attach an image, video, or audio by file id.""")
                .addString("content", "message text, optional when file is set")
                .addInteger("file", "file id of an image, video, or audio to send, optional")
                .addString("reply", "message id to quote, optional")
                .addBoolean("last", "set true when this message completes your answer", false)
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        String content = _args.content == null ? "" : _args.content;
        if (content.isBlank() && _args.file == null)
            return ToolRes.error("Missing content or file");
        List<Long> files = _args.file == null ? List.of() : List.of(_args.file);
        boolean last = Boolean.TRUE.equals(_args.last);
        return ctx.send(content, files, _args.reply, last);
    }

    private record Args(String content, Long file, String reply, Boolean last) {
    }
}
