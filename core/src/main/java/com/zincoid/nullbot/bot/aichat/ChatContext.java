package com.zincoid.nullbot.bot.aichat;

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
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.util.MimeUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 聊天工具上下文：向当前会话发送文本与媒体（图片、视频、音频）并执行机器人指令。
 * <p>Chat tool context: sends text and media (images, videos, audio) to the current chat and runs bot commands.</p>
 *
 * @param client        the outbound port
 * @param chatType      the chat type
 * @param targetId      group or user platform id
 * @param chatId        chat identifier
 * @param event         the triggering event
 * @param finalSent     whether a final message has been sent through the send tool
 * @param cmdDispatcher the command dispatcher
 * @param fileService   the file service
 * @param configService the config service
 * @param memory        the chat memory
 */
@Slf4j
public record ChatContext(
        BotClient client,
        ChatType chatType,
        String targetId,
        String chatId,
        BotEvent event,
        AtomicBoolean finalSent,
        CmdDispatcher cmdDispatcher,
        FileService fileService,
        ConfigService configService,
        Memory memory
) implements ToolContext {

    private static final int MAX_MUTE_MINUTES = 30 * 24 * 60;
    private static final String SELF_NAME = "NullBot";

    // ════ Send ════════════════════

    @Override
    public boolean hasFinalSent() {
        return finalSent.get();
    }

    @Override
    public ToolRes send(String text, List<Long> fileIds, String replyId, boolean last) {
        MsgContent.Builder builder = MsgContent.builder();
        String compact = ChatText.compact(text);
        List<Long> files = new ArrayList<>();
        List<String> media = new ArrayList<>();
        if (!compact.isBlank())
            builder.text(compact);
        for (Long id : fileIds) {
            File file = fileService.getById(id);
            if (file == null) continue;
            String kind = attach(builder, file);
            if (kind == null) continue;
            files.add(id);
            media.add(kind + ", file id=" + id);
        }
        if (compact.isBlank() && files.isEmpty())
            return ToolRes.error("No valid media: " + fileIds);
        if (replyId != null && !replyId.isBlank())
            builder.reply(replyId);
        MsgContent message = builder.build();
        String content = annotate(compact, media);
        String messageId = deliver(message, content, files, null, last);
        return ToolRes.of(messageId == null || messageId.isBlank()
                ? "Message sent"
                : "Message sent: id=" + messageId);
    }

    public String deliver(MsgContent message, String content, List<Long> files,
                          String reasoning, boolean last) {
        String messageId = chatType == ChatType.GROUP
                ? client.sendGroupMsg(targetId, message)
                : client.sendPrivateMsg(targetId, message);
        if (last) finalSent.set(true);
        if (messageId == null || messageId.isBlank()) {
            log.warn("Missing message id after send: chat={}", chatId);
            return messageId;
        }
        memory.add(chatId, messageId, event.selfId(), SELF_NAME,
                AiMessage.assistant(content).withReasoning(reasoning), files);
        return messageId;
    }

    private String attach(MsgContent.Builder builder, File file) {
        String mime = mime(file);
        if (mime == null) return null;
        if (mime.startsWith("image/")) {
            builder.image(fileService.uri(file));
            return "image";
        } else if (mime.startsWith("video/")) {
            builder.video(fileService.uri(file));
            return "video";
        } else if (mime.startsWith("audio/")) {
            builder.voice(fileService.uri(file));
            return "voice";
        } else return null;
    }

    private static String mime(File file) {
        String mime = MimeUtils.mime(file.getStored());
        return mime != null ? mime : MimeUtils.mime(file.getOriginal());
    }

    private static String annotate(String text, List<String> media) {
        if (media.isEmpty()) return text;
        String note = "(sent " + String.join("; ", media) + ")";
        return text.isBlank() ? note : text + " " + note;
    }

    // ════ Mute ════════════════════

    @Override
    public ToolRes mute(String userId, int minutes) {
        if (chatType != ChatType.GROUP)
            return ToolRes.error("Mute is only available in group chats");
        if (userId == null || userId.isBlank())
            return ToolRes.error("Missing user");
        if (userId.equals(event.selfId()))
            return ToolRes.error("Cannot mute the bot");
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
        if (cmdEvent == null)
            return ToolRes.error("Command not available for this event");
        boolean checkAccess = configService.getBool(Conf.AI_CMD_ACCESS_CHECK);
        cmdDispatcher.dispatch(client, cmdEvent, new CmdRequest(name, params), checkAccess);
        return ToolRes.of("Command executed: " + name);
    }

    private MsgEvent cmdEvent() {
        if (event instanceof MsgEvent e) return e;
        if (!(event instanceof PokeEvent)) return null;
        String messageId = UUID.randomUUID().toString();
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
