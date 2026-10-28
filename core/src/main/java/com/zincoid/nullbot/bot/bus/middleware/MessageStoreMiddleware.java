package com.zincoid.nullbot.bot.bus.middleware;

import com.zincoid.nullbot.bot.BotClients;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.message.MessageWindowStore;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.util.ChatIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 把消息事件记录进会话窗口，并落盘其中媒体附件的中间件。
 * <p>Middleware that records message events into the chat window, storing their media attachments.</p>
 */
@Slf4j
@Order(0)
@Component
@RequiredArgsConstructor
public class MessageStoreMiddleware implements Middleware {

    private final MessageWindowStore messageWindowStore;
    private final FileService fileService;

    // ════ Handle ══════════════════

    /**
     * 记录消息事件并保存其中的媒体附件，事件继续传递。
     * <p>Records message events and stores their media attachments, then passes the event on.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @param chain  the remaining middleware chain
     */
    @Override
    public void handle(BotClient client, BotEvent event, MiddlewareChain chain) {
        if (event instanceof GroupMsgEvent e) {
            record(client, ChatIds.ofGroup(e.groupId()), e);
        } else if (event instanceof PrivateMsgEvent e) {
            record(client, ChatIds.ofPrivate(e.userId()), e);
        }
        chain.doHandle(client, event);
    }

    // ════ Helpers ═════════════════

    /**
     * 记录事件消息并落盘其中的媒体附件。
     * <p>Records the event message and stores its media attachments.</p>
     *
     * @param client the outbound port
     * @param chatId chat identifier
     * @param event  the message event
     */
    private void record(BotClient client, String chatId, MsgEvent event) {
        StringBuilder content = new StringBuilder();
        List<String> files = new ArrayList<>();
        for (MsgSegment segment : event.content().segments()) {
            switch (segment) {
                case MsgSegment.Text s -> content.append(s.text());
                case MsgSegment.Image s -> {
                    content.append("[Image]");
                    String fileId = saveAttachment(event.userId(), s.url(), s.file());
                    if (fileId != null) files.add(fileId);
                }  // Only save image files to local
                case MsgSegment.Video _ -> content.append("[Video]");
                case MsgSegment.Voice _ -> content.append("[Voice]");
                case MsgSegment.At s -> content.append(at(client, event.selfId(), s.userId()));
                case MsgSegment.Reply s -> content.append("[Reply id=").append(s.messageId()).append("]");
                case MsgSegment.Face s -> content.append("[Face id=").append(s.id()).append("]");
                case MsgSegment.Raw s -> content.append("[Unsupported type=").append(s.type()).append("]");
            }
        }
        if (content.isEmpty() && files.isEmpty()) return;
        messageWindowStore.add(
                chatId, event.messageId(),
                event.userId(), MessageRole.USER, event.nickname(),
                content.toString(), null, files
        );
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
    private static String at(BotClient client, String selfId, String userId) {
        if (userId.equals(selfId)) return "@you";
        return "@" + BotClients.nickname(client, userId) + "(" + userId + ")";
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
    private String saveAttachment(String userId, String url, String file) {
        try {
            File saved = fileService.save(userId, BizType.CHAT, url, file);
            return String.valueOf(saved.getId());
        } catch (RuntimeException e) {
            log.warn("Failed to store attachment: user={}", userId, e);
            return null;
        }
    }
}
