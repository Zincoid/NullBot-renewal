package com.zincoid.nullbot.bot.chat;

import com.zincoid.nullbot.bot.cmd.CmdDispatcher;
import com.zincoid.nullbot.bot.cmd.CmdRequest;
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
 * 聊天工具上下文：向当前会话发送文本与媒体（图片、视频、音频）并执行机器人指令。
 * <p>Chat tool context: sends text and media (images, videos, audio) to the current chat and runs bot commands.</p>
 *
 * @param client        the outbound port
 * @param targetId      group or user platform id
 * @param group         whether the target is a group
 * @param event         the triggering event
 * @param cmdDispatcher the command dispatcher
 * @param fileService   the file service
 */
public record ChatContext(
        BotClient client,
        String targetId,
        boolean group,
        BotEvent event,
        CmdDispatcher cmdDispatcher,
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
            if (file == null) continue;
            if (attach(builder, file)) attached++;
        }
        if ((text == null || text.isBlank()) && attached == 0)
            return ToolRes.error("No valid media: " + fileIds);
        if (replyId != null && !replyId.isBlank())
            builder.reply(replyId);
        MsgContent content = builder.build();
        if (group) client.sendGroupMsg(targetId, content);
        else client.sendPrivateMsg(targetId, content);
        return ToolRes.of("Message sent: " + content);
    }

    private boolean attach(MsgContent.Builder builder, File file) {
        String mime = mime(file);
        if (mime == null) return false;
        if (mime.startsWith("image/")) {
            builder.image(fileService.uri(file));
        } else if (mime.startsWith("video/")) {
            builder.video(fileService.uri(file));
        } else if (mime.startsWith("audio/")) {
            builder.voice(fileService.uri(file));
        } else {
            return false;
        }
        return true;
    }

    private static String mime(File file) {
        String mime = MimeUtils.mime(file.getStored());
        return mime != null ? mime : MimeUtils.mime(file.getOriginal());
    }

    // ════ Command ═════════════════

    @Override
    public ToolRes cmd(String name, List<String> params) {
        cmdDispatcher.dispatch(client, event, new CmdRequest(name, params));
        return ToolRes.of("Command executed: " + name);
    }
}
