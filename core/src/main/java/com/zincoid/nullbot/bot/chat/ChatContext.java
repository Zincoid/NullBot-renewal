package com.zincoid.nullbot.bot.chat;

import com.zincoid.nullbot.bot.bus.CmdProcessor;
import com.zincoid.nullbot.bot.bus.CmdRequest;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.util.MimeUtils;

import java.util.List;

/**
 * 聊天工具上下文：向当前会话发送文本/图片并执行机器人指令。
 * <p>Chat tool context: sends text or images to the current chat and runs bot commands.</p>
 *
 * @param client       the outbound port
 * @param targetId     group or user platform id
 * @param group        whether the target is a group
 * @param event        the triggering event
 * @param cmdProcessor the command processor
 * @param fileService  the file service
 */
public record ChatContext(
        BotClient client,
        String targetId,
        boolean group,
        BotEvent event,
        CmdProcessor cmdProcessor,
        FileService fileService
) implements ToolContext {

    // ════ Send ════════════════════

    @Override
    public ToolRes send(String text, List<Long> fileIds, String replyId) {
        MsgContent.Builder builder = MsgContent.builder();
        if (text != null && !text.isBlank())
            builder.text(text);
        int attached = 0;
        for (Long id : fileIds) {
            File file = fileService.getById(id);
            if (file == null)
                continue;
            String mime = MimeUtils.mime(file.getOriginal());
            if (mime == null || !mime.startsWith("image/"))
                continue;
            builder.image(fileService.uri(file));
            attached++;
        }
        if ((text == null || text.isBlank()) && attached == 0)
            return ToolRes.error("No valid image: " + fileIds);
        if (replyId != null && !replyId.isBlank())
            builder.reply(replyId);
        return ToolRes.of("Message sent: " + deliver(builder.build()));
    }

    private String deliver(MsgContent content) {
        return group
                ? client.sendGroupMsg(targetId, content)
                : client.sendPrivateMsg(targetId, content);
    }

    // ════ Command ═════════════════

    @Override
    public ToolRes cmd(String name, List<String> params) {
        cmdProcessor.process(client, event, new CmdRequest(name, params));
        return ToolRes.of("Command executed: " + name);
    }
}
