package com.zincoid.nullbot.bot.aichat;

import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.ai.memory.Memory;
import com.zincoid.nullbot.module.ai.message.AiMsg;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.util.MimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天操作：发送与撤回消息、禁言或解除禁言当前群成员，并把助手回复写入会话记忆。
 * <p>Chat actions: sends and recalls messages, mutes or unmutes members of the current group, and stores assistant replies in the chat memory.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatActions {

    private static final String SELF_NAME = "NullBot";
    private static final int MAX_MUTE_MINUTES = 30 * 24 * 60;

    private final FileService fileService;
    private final Memory memory;

    // ════ Send ════════════════════

    /**
     * 构建并发送消息，可附带收藏文件与引用。
     * <p>Builds and sends a message, optionally with collection files and a quote.</p>
     *
     * @param turn    the turn data
     * @param text    the message text, may be blank when files are present
     * @param fileIds the collection file ids of media to attach
     * @param replyId the platform message id to quote, nullable
     * @param last    whether this message completes the turn
     * @return the tool result
     */
    ToolRes send(ChatTurn turn, String text, List<Long> fileIds, String replyId, boolean last) {
        MsgContent.Builder builder = MsgContent.builder();
        String compact = ChatText.compact(text);
        List<String> media = new ArrayList<>();
        if (!compact.isBlank())
            builder.text(compact);
        for (Long id : fileIds) {
            File file = fileService.getById(id);
            if (file == null) continue;
            String kind = attach(builder, file);
            if (kind == null) continue;
            media.add(kind + ", file id=" + id);
        }
        if (compact.isBlank() && media.isEmpty())
            return ToolRes.error("No valid media: " + fileIds);
        if (replyId != null && !replyId.isBlank())
            builder.reply(replyId);
        MsgContent message = builder.build();
        String content = annotate(compact, media);
        String messageId = deliver(turn, message, content, null, last);
        return ToolRes.of(messageId == null || messageId.isBlank()
                ? "Message sent"
                : "Message sent: id=" + messageId);
    }

    // ════ Deliver ═════════════════

    /**
     * 发送已构建的消息，并按需标记最终消息与写入会话记忆。
     * <p>Sends a built message, marking it as final and storing it in the chat memory when required.</p>
     *
     * @param turn      the turn data
     * @param message   the outbound message
     * @param content   the text stored in memory
     * @param reasoning the reasoning content stored in memory, nullable
     * @param last      whether this message completes the turn
     * @return the platform message id
     */
    String deliver(ChatTurn turn, MsgContent message, String content,
                   String reasoning, boolean last) {
        String messageId = turn.chatType() == ChatType.GROUP
                ? turn.client().sendGroupMsg(turn.targetId(), message)
                : turn.client().sendPrivateMsg(turn.targetId(), message);
        if (last) turn.markFinalSent();
        if (messageId == null || messageId.isBlank()) {
            log.warn("Missing message id after send: chat={}", turn.chatId());
            return messageId;
        }
        memory.add(turn.chatId(), messageId, turn.event().selfId(), SELF_NAME,
                AiMsg.assistant(content).withReasoning(reasoning));
        return messageId;
    }

    // ════ Recall ══════════════════

    /**
     * 撤回一条消息。
     * <p>Recalls a message.</p>
     *
     * @param turn      the turn data
     * @param messageId platform message id
     * @return the tool result
     */
    ToolRes recall(ChatTurn turn, String messageId) {
        if (messageId == null || messageId.isBlank())
            return ToolRes.error("Missing message id");
        turn.client().recall(messageId);
        return ToolRes.of("Message recalled: " + messageId);
    }

    // ════ Mute ════════════════════

    /**
     * 禁言或解除禁言当前群的成员。
     * <p>Mutes or unmutes a member of the current group.</p>
     *
     * @param turn    the turn data
     * @param userId  target platform user id
     * @param minutes duration in minutes, 0 to unmute
     * @return the tool result
     */
    ToolRes mute(ChatTurn turn, String userId, int minutes) {
        if (turn.chatType() != ChatType.GROUP)
            return ToolRes.error("Mute is only available in group chats");
        if (userId == null || userId.isBlank())
            return ToolRes.error("Missing user");
        if (userId.equals(turn.event().selfId()))
            return ToolRes.error("Cannot mute the bot");
        if (minutes < 0 || minutes > MAX_MUTE_MINUTES)
            return ToolRes.error("Invalid minutes: " + minutes);
        turn.client().mute(turn.targetId(), userId, minutes * 60);
        return ToolRes.of(minutes == 0
                ? "Unmuted: " + userId
                : "Muted: " + userId + " for " + minutes + " minute(s)");
    }

    // ════ Helpers ═════════════════

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
}
