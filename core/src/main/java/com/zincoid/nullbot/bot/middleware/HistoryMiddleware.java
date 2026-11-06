package com.zincoid.nullbot.bot.middleware;

import com.zincoid.nullbot.bot.BotClients;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.event.PokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.message.MessageService;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.persistence.po.Message;
import com.zincoid.nullbot.platform.util.ChatIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 历史中间件：把消息事件记录进会话窗口，并落盘其中媒体附件。
 * <p>History middleware: records message events into the chat window, storing their media attachments.</p>
 */
@Slf4j
@Order(0)
@Component
@RequiredArgsConstructor
public class HistoryMiddleware implements Middleware {

    private final MessageService messageService;
    private final FileService fileService;

    // ════ Handle ══════════════════

    /**
     * 记录消息与戳一戳事件并保存消息中的媒体附件，事件继续传递。
     * <p>Records message and poke events, stores message media attachments, then passes the event on.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @param chain  the remaining middleware chain
     */
    @Override
    public void handle(BotClient client, BotEvent event, MiddlewareChain chain) {
        switch (event) {
            case GroupMsgEvent e -> recordMsg(client, ChatIds.ofGroup(e.groupId()), e);
            case PrivateMsgEvent e -> recordMsg(client, ChatIds.ofPrivate(e.userId()), e);
            case GroupPokeEvent e -> recordPoke(client, ChatIds.ofGroup(e.groupId()), e);
            case PrivatePokeEvent e -> recordPoke(client, ChatIds.ofPrivate(e.userId()), e);
            default -> {
            }
        }
        chain.doHandle(client, event);
    }

    // ════ Helpers ═════════════════

    /**
     * 记录事件消息并落盘其中的媒体附件；跳过机器人自身的消息回显。
     * <p>Records the event message and stores its media attachments, skipping echoes of the bot's own messages.</p>
     *
     * @param client the outbound port
     * @param chatId chat identifier
     * @param event  the message event
     */
    private void recordMsg(BotClient client, String chatId, MsgEvent event) {
        if (event.selfId().equals(event.userId())) return;
        StringBuilder content = new StringBuilder();
        List<Long> files = new ArrayList<>();
        for (MsgSegment segment : event.content().segments()) {
            switch (segment) {
                case MsgSegment.Text s -> content.append(s.text());
                case MsgSegment.Image s -> {
                    content.append("[Image]");
                    Long fileId = saveAttachment(event.userId(), s.url(), s.file());
                    if (fileId != null) files.add(fileId);
                }  // Only save image files to local
                case MsgSegment.Video _ -> content.append("[Video]");
                case MsgSegment.Voice _ -> content.append("[Voice]");
                case MsgSegment.At s -> content.append(atText(client, event.selfId(), s.userId()));
                case MsgSegment.Reply s -> content.append("[Reply id=").append(s.messageId()).append("]");
                case MsgSegment.Face s -> content.append("[Face id=").append(s.id()).append("]");
                case MsgSegment.Raw s -> content.append("[Unsupported type=").append(s.type()).append("]");
            }
        }
        if (content.isEmpty() && files.isEmpty()) return;
        Message message = Message.builder()
                .id(event.messageId())
                .chatId(chatId)
                .userId(event.userId())
                .role(MessageRole.USER)
                .nickname(event.nickname())
                .content(content.toString())
                .files(files)
                .build();
        try {
            messageService.append(message);
        } catch (RuntimeException e) {
            if (messageService.getById(message.getId()) == null) {
                for (Long fileId : files) {
                    try {
                        fileService.delete(fileId);
                    } catch (RuntimeException ex) {
                        log.warn("Failed to delete attachment: {}", fileId, ex);
                    }
                }
            }
            throw e;
        }
    }

    /**
     * 记录发向机器人的戳一戳。
     * <p>Records a poke aimed at the bot.</p>
     *
     * @param client the outbound port
     * @param chatId chat identifier
     * @param event  the poke event
     */
    private void recordPoke(BotClient client, String chatId, PokeEvent event) {
        if (!event.selfId().equals(event.targetId())) return;
        if (event.selfId().equals(event.userId())) return;
        String nickname = BotClients.userName(client, event.userId());
        if (nickname == null || nickname.isBlank()) nickname = event.userId();
        messageService.append(Message.builder()
                .id(UUID.randomUUID().toString())
                .chatId(chatId)
                .userId(event.userId())
                .role(MessageRole.USER)
                .nickname(nickname)
                .content("[Rubbed you]")
                .build());
    }

    /**
     * 渲染提及段，昵称查询失败时回退为 ID。
     * <p>Renders a mention segment, falling back to the id when the nickname lookup fails.</p>
     *
     * @param client the outbound port
     * @param selfId the bot's platform user id
     * @param userId the mentioned platform user id
     * @return the rendered mention
     */
    private static String atText(BotClient client, String selfId, String userId) {
        if (userId.equals(selfId)) return "@you";
        return "@" + BotClients.userName(client, userId) + "(" + userId + ")";
    }

    /**
     * 保存一个媒体附件并返回其记录 ID。
     * <p>Stores one media attachment and returns its record id.</p>
     *
     * @param userId uploader platform user id
     * @param url    media source url
     * @param file   original filename, nullable
     * @return the file record id, or null when skipped or failed
     */
    private Long saveAttachment(String userId, String url, String file) {
        try {
            File saved = fileService.save(userId, BizType.CHAT, url, file);
            return saved.getId();
        } catch (RuntimeException e) {
            log.warn("Failed to store attachment: user={}", userId, e);
            return null;
        }
    }
}
