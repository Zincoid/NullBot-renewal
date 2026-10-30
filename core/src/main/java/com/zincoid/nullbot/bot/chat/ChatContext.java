package com.zincoid.nullbot.bot.chat;

import com.zincoid.nullbot.bot.cmd.CmdDispatcher;
import com.zincoid.nullbot.bot.cmd.CmdRequest;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
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
 * @param configService the config service
 */
public record ChatContext(
        BotClient client,
        String targetId,
        boolean group,
        BotEvent event,
        CmdDispatcher cmdDispatcher,
        FileService fileService,
        ConfigService configService
) implements ToolContext {

    private static final int MAX_MUTE_MINUTES = 30 * 24 * 60;

    // ════ Send ════════════════════

    @Override
    public ToolRes send(String text, List<Long> fileIds, String replyId) {
        MsgContent.Builder builder = MsgContent.builder();
        String compact = ChatText.compact(text);
        if (!compact.isBlank())
            builder.text(compact);
        int attached = 0;
        for (Long id : fileIds) {
            File file = fileService.getById(id);
            if (file == null) continue;
            if (attach(builder, file)) attached++;
        }
        if (compact.isBlank() && attached == 0)
            return ToolRes.error("No valid media: " + fileIds);
        if (replyId != null && !replyId.isBlank())
            builder.reply(replyId);
        MsgContent content = builder.build();
        String messageId = group
                ? client.sendGroupMsg(targetId, content)
                : client.sendPrivateMsg(targetId, content);
        return ToolRes.of(messageId == null || messageId.isBlank()
                ? "Message sent"
                : "Message sent: id=" + messageId);
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

    // ════ Mute ════════════════════

    /**
     * 禁言或解除禁言当前群成员。
     * <p>Mutes or unmutes a member of the current group.</p>
     *
     * @param userId  target platform user id
     * @param minutes duration in minutes, 0 to unmute
     * @return the tool result
     */
    @Override
    public ToolRes mute(String userId, int minutes) {
        if (!group) return ToolRes.error("Mute is only available in group chats");
        if (userId == null || userId.isBlank()) return ToolRes.error("Missing user");
        if (userId.equals(event.selfId())) return ToolRes.error("Cannot mute the bot");
        if (minutes < 0 || minutes > MAX_MUTE_MINUTES)
            return ToolRes.error("Invalid minutes: " + minutes);
        client.mute(targetId, userId, minutes * 60);
        return ToolRes.of(minutes == 0
                ? "Unmuted: " + userId
                : "Muted: " + userId + " for " + minutes + " minute(s)");
    }

    // ════ Recall ══════════════════

    /**
     * 撤回一条消息。
     * <p>Recalls a message.</p>
     *
     * @param messageId platform message id
     * @return the tool result
     */
    @Override
    public ToolRes recall(String messageId) {
        if (messageId == null || messageId.isBlank())
            return ToolRes.error("Missing message id");
        client.recall(messageId);
        return ToolRes.of("Message recalled: " + messageId);
    }

    // ════ Command ═════════════════

    @Override
    public ToolRes cmd(String name, List<String> params) {
        boolean checkAccess = configService.getBool(Conf.AI_CMD_ACCESS_CHECK);
        cmdDispatcher.dispatch(client, event, new CmdRequest(name, params), checkAccess);
        return ToolRes.of("Command executed: " + name);
    }
}
