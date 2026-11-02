package com.zincoid.nullbot.bot.chat;

import com.zincoid.nullbot.bot.BotClients;
import com.zincoid.nullbot.bot.cmd.CmdDispatcher;
import com.zincoid.nullbot.bot.cmd.CmdRequest;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.event.PokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.ai.memory.Memory;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.util.MimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
 * @param memory        the chat memory
 * @param chatId        chat identifier
 * @param selfName      the bot nickname
 */
public record ChatContext(
        BotClient client,
        String targetId,
        boolean group,
        BotEvent event,
        CmdDispatcher cmdDispatcher,
        FileService fileService,
        ConfigService configService,
        Memory memory,
        String chatId,
        String selfName
) implements ToolContext {

    private static final int MAX_MUTE_MINUTES = 30 * 24 * 60;

    // ════ Send ════════════════════

    @Override
    public ToolRes send(String text, List<Long> fileIds, String replyId) {
        MsgContent.Builder builder = MsgContent.builder();
        String compact = ChatText.compact(text);
        StringBuilder recorded = new StringBuilder(compact);
        List<String> recordedFiles = new ArrayList<>();
        if (!compact.isBlank())
            builder.text(compact);
        int attached = 0;
        for (Long id : fileIds) {
            File file = fileService.getById(id);
            if (file == null) continue;
            String label = attach(builder, file);
            if (label != null) {
                attached++;
                recorded.append(label);
                recordedFiles.add(String.valueOf(id));
            }
        }
        if (compact.isBlank() && attached == 0)
            return ToolRes.error("No valid media: " + fileIds);
        if (replyId != null && !replyId.isBlank())
            builder.reply(replyId);
        MsgContent content = builder.build();
        String messageId = group
                ? client.sendGroupMsg(targetId, content)
                : client.sendPrivateMsg(targetId, content);
        if (messageId != null && !messageId.isBlank())
            memory.add(chatId, messageId, event.selfId(), selfName,
                    AiMessage.assistant(recorded.toString()), recordedFiles);
        return ToolRes.of(messageId == null || messageId.isBlank()
                ? "Message sent"
                : "Message sent: id=" + messageId);
    }

    private String attach(MsgContent.Builder builder, File file) {
        String mime = mime(file);
        if (mime == null) return null;
        if (mime.startsWith("image/")) {
            builder.image(fileService.uri(file));
            return "[Image]";
        } else if (mime.startsWith("video/")) {
            builder.video(fileService.uri(file));
            return "[Video]";
        } else if (mime.startsWith("audio/")) {
            builder.voice(fileService.uri(file));
            return "[Voice]";
        } else return null;
    }

    private static String mime(File file) {
        String mime = MimeUtils.mime(file.getStored());
        return mime != null ? mime : MimeUtils.mime(file.getOriginal());
    }

    // ════ Mute ════════════════════

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
        MsgEvent cmdEvent = cmdEvent();
        if (cmdEvent == null) return ToolRes.error("Command not available for this event");
        boolean checkAccess = configService.getBool(Conf.AI_CMD_ACCESS_CHECK);
        cmdDispatcher.dispatch(client, cmdEvent, new CmdRequest(name, params), checkAccess);
        return ToolRes.of("Command executed: " + name);
    }

    // ════ Helpers ═════════════════

    /**
     * 指令只接受消息事件；戳一戳触发 AI 时合成为对应的消息事件。
     * <p>Commands accept message events only; a poke-triggered AI call is converted to the matching message event.</p>
     *
     * @return the command message event, or null when unsupported
     */
    private MsgEvent cmdEvent() {
        if (event instanceof MsgEvent e) return e;
        if (!(event instanceof PokeEvent)) return null;
        String messageId = "poke-" + UUID.randomUUID();
        String nickname = BotClients.userName(client, event.userId());
        MsgContent content = MsgContent.of("");
        if (event instanceof GroupPokeEvent e)
            return new GroupMsgEvent(e.selfId(), e.userId(), e.groupId(),
                    messageId, nickname, content, false, false);
        if (event instanceof PrivatePokeEvent e)
            return new PrivateMsgEvent(e.selfId(), e.userId(),
                    messageId, nickname, content, false, false);
        return null;
    }
}
