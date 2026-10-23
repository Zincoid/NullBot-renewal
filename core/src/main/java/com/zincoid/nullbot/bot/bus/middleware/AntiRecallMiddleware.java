package com.zincoid.nullbot.bot.bus.middleware;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupRecallEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.LanguageService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.message.MessageWindowStore;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.persistence.po.Message;
import com.zincoid.nullbot.platform.util.ChatIds;
import com.zincoid.nullbot.platform.util.MimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 防撤回中间件：按群设置重发被撤回的消息及其图片。
 * <p>Anti-recall middleware: resends recalled group messages and their images according to the group settings.</p>
 */
@Slf4j
@Order(5)
@Component
@RequiredArgsConstructor
public class AntiRecallMiddleware implements Middleware {

    private final GroupSettingService groupSettingService;
    private final LanguageService languageService;
    private final MessageWindowStore messageWindowStore;
    private final FileService fileService;

    // ════ Handle ══════════════════

    /**
     * 处理群撤回事件，需要时重发消息并消费事件。
     * <p>Handles group recall events, resending the message when needed and consuming the event.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @param chain  the remaining middleware chain
     */
    @Override
    public void handle(BotClient client, BotEvent event, MiddlewareChain chain) {
        if (event instanceof GroupRecallEvent e) {
            if (!e.selfId().equals(e.userId())
                    && groupSettingService.get(e.groupId()).getAntiRecallEnabled())
                resend(client, e);
            chain.consume();
            return;
        }
        chain.doHandle(client, event);
    }

    // ════ Helpers ═════════════════

    /**
     * 从会话窗口取出被撤回的用户消息并重发文本与图片。
     * <p>Finds the recalled user message in the chat window and resends its text and images.</p>
     *
     * @param client the outbound port
     * @param event  the group recall event
     */
    private void resend(BotClient client, GroupRecallEvent event) {
        Message message = messageWindowStore.get(ChatIds.ofGroup(event.groupId())).stream()
                .filter(m -> event.messageId().equals(m.getId()))
                .filter(m -> m.getRole() == MessageRole.USER)
                .findFirst()
                .orElse(null);
        if (message == null) {
            log.warn("Recalled message not found: {}", event.messageId());
            return;
        }
        Language language = languageService.group(event.groupId());
        String content = message.getContent() == null ? "" : message.getContent();
        MsgContent.Builder msg = MsgContent.builder().text(Text.ANTI_RECALL_DETECTED.of(
                language, nickname(client, event.userId()), event.userId(), content));
        if (message.getFiles() != null) {
            for (String fileId : message.getFiles()) {
                String uri = imageUri(fileId);
                if (uri != null) msg.image(uri);
            }
        }
        client.sendGroupMsg(event.groupId(), msg.build());
        log.info("Anti-recall: group={}, user={}, message={}",
                event.groupId(), event.userId(), event.messageId());
    }

    /**
     * 查询用户昵称，失败时回退为 ID。
     * <p>Looks up the user nickname, falling back to the id.</p>
     *
     * @param client the outbound port
     * @param userId platform user id
     * @return the nickname or the id
     */
    private static String nickname(BotClient client, String userId) {
        try {
            return client.getUserInfo(userId).nickname();
        } catch (RuntimeException e) {
            return userId;
        }
    }

    /**
     * 把附件文件 ID 解析为可发送的图片 URI，非图片或缺失时返回 null。
     * <p>Resolves an attachment file id into a sendable image URI, returning null for non-images or missing files.</p>
     *
     * @param fileId the file record id
     * @return the image URI, or null when it cannot be sent as an image
     */
    private String imageUri(String fileId) {
        if (fileId == null || fileId.isBlank()) return null;
        try {
            File file = fileService.getById(Long.valueOf(fileId));
            if (file == null) return null;
            String mime = MimeUtils.mime(file.getStored());
            if (mime == null) mime = MimeUtils.mime(file.getOriginal());
            return mime != null && mime.startsWith("image/") ? fileService.uri(file) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
