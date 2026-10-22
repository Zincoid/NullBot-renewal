package com.zincoid.nullbot.module.ai.tool.impl;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.message.MessageService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.persistence.po.Message;
import com.zincoid.nullbot.platform.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息查看工具：按消息 ID 返回正文与附件文件名。
 * <p>Message tool: returns a message's content and attachment filenames.</p>
 */
@Component
@RequiredArgsConstructor
public class Read implements Tool {

    private final MessageService messageService;
    private final FileService fileService;

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("read", """
                        Returns a message's content and attachment filenames.""")
                .addString("id", "message id", true)
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        Message msg = messageService.getById(_args.id);
        if (msg == null)
            return ToolRes.error("Message not found: " + _args.id);
        String name = msg.getNickname() == null
                ? msg.getUserId() : msg.getNickname();
        StringBuilder text = new StringBuilder()
                .append("[id=").append(msg.getId()).append("] ")
                .append(name).append("(").append(msg.getUserId()).append("): ")
                .append(msg.getContent());
        List<String> files = msg.getFiles();
        if (files == null || files.isEmpty()) {
            text.append("\nAttachments: none");
        } else {
            text.append("\nAttachments:");
            for (String fileId : files)
                text.append("\n").append(attachment(fileId));
        }
        return ToolRes.of(text.toString());
    }

    // ════ Helpers ═════════════════

    private String attachment(String id) {
        File file = fileService.getById(Long.parseLong(id));
        return file == null
                ? "- file id=" + id + ": not found"
                : "- file id=" + id + ": " + file.getOriginal();
    }

    private record Args(String id) {
    }
}
