package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.bot.BotClients;
import com.zincoid.nullbot.bot.BotException;
import com.zincoid.nullbot.bot.BotHolder;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.console.dto.AttachmentView;
import com.zincoid.nullbot.console.dto.ChatView;
import com.zincoid.nullbot.console.dto.MessageSend;
import com.zincoid.nullbot.console.dto.MessageSendView;
import com.zincoid.nullbot.console.dto.MessageView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.console.util.ConsoleDownloads;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.message.AttachmentService;
import com.zincoid.nullbot.module.message.MessageService;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.Attachment;
import com.zincoid.nullbot.platform.persistence.po.Group;
import com.zincoid.nullbot.platform.persistence.po.Message;
import com.zincoid.nullbot.platform.persistence.po.User;
import com.zincoid.nullbot.platform.util.ChatIds;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制台消息接口：按会话浏览聊天记录与附件，并支持主动发送文本。
 * <p>Console message API: browses chat history and attachments by chat, and sends text proactively.</p>
 */
@Slf4j
@RestController
@RequestMapping("/console/messages")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class MessageController {

    private static final String SELF_NAME = "NullBot";

    private final MessageService messageService;
    private final AttachmentService attachmentService;
    private final GroupService groupService;
    private final UserService userService;
    private final BotHolder botHolder;

    // ════ Chats ═══════════════════

    /**
     * 列出保留有消息的会话，按最后消息时间倒序。
     * <p>Lists chats with retained messages, newest activity first.</p>
     *
     * @return the chat views
     */
    @GetMapping("/chats")
    public List<ChatView> chats() {
        List<MessageService.ChatSummary> summaries = messageService.chats();
        if (summaries.isEmpty()) return List.of();

        List<String> groupIds = rawIds(summaries, ChatType.GROUP);
        List<String> userIds = rawIds(summaries, ChatType.PRIVATE);

        Map<String, String> names = new HashMap<>();
        if (!groupIds.isEmpty()) {
            for (Group group : groupService.listByIds(groupIds))
                names.put(ChatIds.ofGroup(group.getId()), group.getName());
        }
        if (!userIds.isEmpty()) {
            for (User user : userService.listByIds(userIds))
                names.put(ChatIds.ofPrivate(user.getId()), user.getName());
        }

        BotClient client = clientOrNull();
        return summaries.stream()
                .map(summary -> {
                    ChatType type = ChatIds.type(summary.chatId());
                    String raw = ChatIds.rawId(summary.chatId());
                    String name = names.get(summary.chatId());
                    if (missing(name, raw)) name = liveName(client, type, raw);
                    if (name == null || name.isBlank()) name = raw;
                    if (name == null || name.isBlank()) name = summary.chatId();
                    return new ChatView(
                            summary.chatId(),
                            name,
                            type == null ? "unknown" : type.getLabel(),
                            summary.messages(),
                            summary.lastAt());
                })
                .toList();
    }

    // ════ Messages ════════════════

    /**
     * 返回指定会话保留的消息，按时间正序。
     * <p>Returns the retained messages of a chat in chronological order.</p>
     *
     * @param chatId composite chat id
     * @return the message views
     */
    @GetMapping
    public List<MessageView> list(@RequestParam(required = false) String chatId) {
        String id = ConsoleValidation.required(chatId, 64, "chatId");
        List<Message> messages = messageService.window(id);
        if (messages.isEmpty()) return List.of();
        Map<String, Long> counts = attachmentService.countByMessageIds(
                messages.stream().map(Message::getId).toList());
        return messages.stream()
                .map(message -> MessageView.of(message,
                        counts.getOrDefault(message.getId(), 0L)))
                .toList();
    }

    /**
     * 清空指定会话的历史与聊天附件。
     * <p>Clears the history and chat attachments of the given chat.</p>
     *
     * @param chatId composite chat id
     * @return an empty response
     */
    @DeleteMapping
    public ResponseEntity<Void> reset(@RequestParam(required = false) String chatId) {
        String id = ConsoleValidation.required(chatId, 64, "chatId");
        messageService.clear(id);
        return ResponseEntity.noContent().build();
    }

    // ════ Send ════════════════════

    /**
     * 以机器人身份向指定会话发送文本消息，并写入历史。
     * <p>Sends a text message to the given chat as the bot and records it in history.</p>
     *
     * @param body the send request
     * @return the sent message id and whether it was recorded
     */
    @PostMapping
    public MessageSendView send(@Valid @RequestBody MessageSend body) {
        String chatId = body.chatId().trim();
        String content = body.content().trim();
        ChatType type = ChatIds.type(chatId);
        String targetId = ChatIds.rawId(chatId);
        if (type == null || targetId == null || targetId.isBlank())
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Unknown chat: " + chatId);

        BotClient client;
        String selfId;
        try {
            client = botHolder.getClient();
            selfId = botHolder.onlineId();
        } catch (BotException e) {
            throw new ConsoleException(HttpStatus.SERVICE_UNAVAILABLE, "Bot offline");
        }
        if (selfId == null)
            throw new ConsoleException(HttpStatus.SERVICE_UNAVAILABLE, "Bot offline");

        String messageId;
        try {
            messageId = type == ChatType.GROUP
                    ? client.sendGroupMsg(targetId, MsgContent.of(content))
                    : client.sendPrivateMsg(targetId, MsgContent.of(content));
        } catch (RuntimeException e) {
            log.warn("Console message send failed: chat={}", chatId, e);
            String reason = e.getMessage();
            if (reason == null || reason.isBlank()) reason = e.getClass().getSimpleName();
            throw new ConsoleException(HttpStatus.BAD_GATEWAY,
                    "Send failed: " + reason, e);
        }
        if (messageId == null || messageId.isBlank())
            throw new ConsoleException(HttpStatus.BAD_GATEWAY, "Missing message id");

        boolean stored = recordSent(chatId, messageId, selfId, content);
        log.info("Console message sent: chat={}, id={}, stored={}", chatId, messageId, stored);
        return new MessageSendView(messageId, stored);
    }

    /**
     * 将已发送的消息写入会话历史。
     * <p>Records a sent message in the chat history.</p>
     *
     * @param chatId    composite chat id
     * @param messageId platform message id
     * @param selfId    bot account id
     * @param content   message text
     * @return true when the record was appended
     */
    private boolean recordSent(String chatId, String messageId, String selfId, String content) {
        try {
            messageService.append(Message.builder()
                    .id(messageId)
                    .chatId(chatId)
                    .userId(selfId)
                    .role(MessageRole.ASSISTANT)
                    .nickname(SELF_NAME)
                    .content(content)
                    .build());
            return true;
        } catch (RuntimeException e) {
            log.warn("Console message sent but not recorded: chat={}, id={}",
                    chatId, messageId, e);
            return false;
        }
    }

    // ════ Attachments ═════════════

    /**
     * 返回指定消息的附件元数据，不读取磁盘内容。
     * <p>Returns the attachment metadata of a message without reading stored content.</p>
     *
     * @param messageId platform message id
     * @return the attachment views
     */
    @GetMapping("/{messageId}/attachments")
    public List<AttachmentView> attachments(@PathVariable String messageId) {
        String id = ConsoleValidation.required(messageId, 64, "messageId");
        if (messageService.getById(id) == null)
            throw new ConsoleException(HttpStatus.NOT_FOUND, "Message not found: " + id);
        return attachmentService.listByMessageId(id).stream()
                .map(AttachmentView::of)
                .toList();
    }

    /**
     * 以内联方式读取指定消息的附件内容。
     * <p>Reads the stored content of a message attachment inline.</p>
     *
     * @param messageId    platform message id
     * @param attachmentId attachment record id
     * @return the inline attachment content
     */
    @GetMapping("/{messageId}/attachments/{attachmentId}/content")
    public ResponseEntity<Resource> content(@PathVariable String messageId,
                                            @PathVariable Long attachmentId) {
        String id = ConsoleValidation.required(messageId, 64, "messageId");
        Attachment attachment = attachmentService.getById(attachmentId);
        if (attachment == null || !id.equals(attachment.getMessageId()))
            throw new ConsoleException(HttpStatus.NOT_FOUND, "Attachment not found: " + attachmentId);
        Path path = attachmentService.path(attachment);
        if (!Files.isRegularFile(path))
            throw new ConsoleException(HttpStatus.NOT_FOUND,
                    "Attachment content not found: " + attachmentId);
        return ConsoleDownloads.inline(path, attachment.getOriginal(),
                AttachmentView.contentType(attachment));
    }

    // ════ Helpers ═════════════════

    /**
     * 提取指定类型的会话原始 ID。
     * <p>Extracts raw ids of the given chat type.</p>
     *
     * @param summaries chat summaries
     * @param type      chat type
     * @return distinct non-blank raw ids
     */
    private static List<String> rawIds(List<MessageService.ChatSummary> summaries, ChatType type) {
        return summaries.stream()
                .filter(summary -> ChatIds.type(summary.chatId()) == type)
                .map(summary -> ChatIds.rawId(summary.chatId()))
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
    }

    /**
     * 返回当前在线机器人客户端，离线时返回 null。
     * <p>Returns the online bot client, or null when the bot is offline.</p>
     *
     * @return the online client, or null
     */
    private BotClient clientOrNull() {
        try {
            return botHolder.getClient();
        } catch (BotException e) {
            return null;
        }
    }

    /**
     * 名称为空或只是原始 ID 时，尝试从平台查询显示名称，不写数据库。
     * <p>Looks up a display name from the platform when the stored name is missing or only the raw id, without writing it back.</p>
     *
     * @param client the online bot client, nullable
     * @param type   the chat type, nullable
     * @param rawId  the raw platform id, nullable
     * @return the resolved name, or null when unavailable
     */
    private static String liveName(BotClient client, ChatType type, String rawId) {
        if (client == null || type == null || rawId == null || rawId.isBlank()) return null;
        String name = type == ChatType.GROUP
                ? BotClients.groupName(client, rawId)
                : BotClients.userName(client, rawId);
        return missing(name, rawId) ? null : name;
    }

    /**
     * 判断名称是否缺失或只是原始 ID。
     * <p>Whether the name is missing or only the raw id.</p>
     *
     * @param name  the candidate name, nullable
     * @param rawId the raw platform id, nullable
     * @return true when the name is missing or equal to the raw id
     */
    private static boolean missing(String name, String rawId) {
        return name == null || name.isBlank() || name.equals(rawId);
    }
}
