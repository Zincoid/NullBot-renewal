package com.zincoid.nullbot.bot.bus.pre;

import com.zincoid.nullbot.bot.memory.MsgWindowStore;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.util.ChatIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 把消息事件记录进会话窗口，并落盘其中媒体附件的预处理器。
 * <p>Pre-handler that records message events into the chat window, storing their media attachments.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MsgStoreHandler implements PreHandler {

    private final MsgWindowStore msgWindowStore;
    private final FileService fileService;

    @Override
    public void handle(BotClient client, BotEvent event, PreChain chain) {
        if (event instanceof GroupMsgEvent e) {
            record(ChatIds.ofGroup(e.groupId()), e);
        } else if (event instanceof PrivateMsgEvent e) {
            record(ChatIds.ofPrivate(e.userId()), e);
        }
        chain.doHandle(client, event);
    }

    /**
     * 记录事件消息并落盘其中的媒体附件。
     * <p>Records the event message and stores its media attachments.</p>
     *
     * @param chatId chat identifier
     * @param event  the message event
     */
    private void record(String chatId, MsgEvent event) {
        StringBuilder content = new StringBuilder();
        List<String> files = new ArrayList<>();
        for (MsgSegment segment : event.content().segments()) {
            switch (segment) {
                case MsgSegment.Text s -> content.append(s.text());
                case MsgSegment.Image s -> {
                    content.append("[Image]");
                    addFile(files, event.userId(), s.url());
                }
                case MsgSegment.Video s -> {
                    content.append("[Video]");
                    // addFile(files, event.userId(), s.url());
                }
                case MsgSegment.Voice s -> {
                    content.append("[Voice]");
                    // addFile(files, event.userId(), s.url());
                }
                case MsgSegment.At s -> content.append("@").append(s.userId());
                case MsgSegment.Reply s -> content.append("[Reply ").append(s.messageId()).append("]");
                case MsgSegment.Face s -> content.append("[Face ").append(s.id()).append("]");
                case MsgSegment.Raw s -> content.append("[Unsupported ").append(s.type()).append("]");
            }
        }
        String text = content.toString();
        if (text.isBlank() && files.isEmpty()) return;
        msgWindowStore.add(event.messageId(), chatId, event.userId(), text, files);
    }

    /**
     * 落盘一个媒体附件，成功时追加其记录 ID。
     * <p>Stores one media attachment and appends its record id when stored.</p>
     *
     * @param files  file record id list
     * @param userId uploader platform user ID
     * @param url    media source url
     */
    private void addFile(List<String> files, String userId, String url) {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            log.warn("Skipped non-HTTP attachment: {}", url);
            return;
        }
        try {
            files.add(String.valueOf(fileService.save(userId, url, BizType.CHAT, null).getId()));
        } catch (RuntimeException e) {
            log.warn("Failed to store attachment: {}", url, e);
        }
    }
}
