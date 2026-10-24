package com.zincoid.nullbot.bot.bus.middleware;

import com.zincoid.nullbot.bot.BotClients;
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
import com.zincoid.nullbot.platform.persistence.po.Message;
import com.zincoid.nullbot.platform.util.ChatIds;
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
                language, BotClients.nickname(client, event.userId()), event.userId(), content));
        if (message.getFiles() != null) {
            for (String fileId : message.getFiles()) {
                String uri = fileService.imageUri(fileId);
                if (uri != null) msg.image(uri);
            }
        }
        client.sendGroupMsg(event.groupId(), msg.build());
        log.info("Anti-recall: group={}, user={}, message={}",
                event.groupId(), event.userId(), event.messageId());
    }
}
