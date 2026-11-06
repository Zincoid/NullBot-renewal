package com.zincoid.nullbot.bot.aichat.tool;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.message.AttachmentService;
import com.zincoid.nullbot.module.message.MessageService;
import com.zincoid.nullbot.platform.persistence.po.Attachment;
import com.zincoid.nullbot.platform.persistence.po.Message;
import com.zincoid.nullbot.platform.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息查看工具：按消息 ID 返回正文与附件 ID。
 * <p>Message tool: returns a message's content and attachment ids.</p>
 */
@Component
@RequiredArgsConstructor
public class Read implements Tool {

    private final MessageService messageService;
    private final AttachmentService attachmentService;

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("read", """
                        Returns a message's content and attachment ids.""")
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
        String content = msg.getContent() == null
                ? "(No content)" : msg.getContent();
        StringBuilder text = new StringBuilder()
                .append("[id=").append(msg.getId()).append("] ")
                .append(name).append("(").append(msg.getUserId()).append("): ")
                .append(content);
        List<Long> attachments = attachmentService.listByMessageId(msg.getId()).stream()
                .map(Attachment::getId)
                .toList();
        if (attachments.isEmpty()) text.append("\nAttachments: none");
        else text.append("\nAttachments: ").append(String.join(
                ", ", attachments.stream().map(String::valueOf).toList()));
        return ToolRes.of(text.toString());
    }

    private record Args(String id) {
    }
}
