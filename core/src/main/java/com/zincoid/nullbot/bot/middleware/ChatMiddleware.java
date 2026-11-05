package com.zincoid.nullbot.bot.middleware;

import com.zincoid.nullbot.bot.BotRegistrar;
import com.zincoid.nullbot.bot.chat.ChatPipeline;
import com.zincoid.nullbot.bot.cmd.CmdParser;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.i18n.LanguageService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.setting.GroupRateLimiter;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.platform.persistence.enums.AiSource;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 对话中间件：把群 @、私聊、戳一戳和自动聊天转为 AI 对话。
 * <p>Chat middleware: turns group mentions, private messages, pokes, and auto chat into AI conversations.</p>
 */
@Slf4j
@Order(3)
@Component
@RequiredArgsConstructor
public class ChatMiddleware implements Middleware {

    private static final String LIMIT_KEY = "AI";

    private final ChatPipeline chatPipeline;
    private final CmdParser cmdParser;
    private final GroupSettingService groupSettingService;
    private final LanguageService languageService;
    private final BotRegistrar botRegistrar;
    private final GroupService groupService;
    private final UserService userService;
    private final GroupRateLimiter groupRateLimiter;

    // ════ Handle ══════════════════

    /**
     * 识别 AI 触发条件，命中时调用聊天流水线并消费事件。
     * <p>Detects AI triggers, invokes the chat pipeline on a hit, and consumes the event.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @param chain  the remaining middleware chain
     */
    @Override
    public void handle(BotClient client, BotEvent event, MiddlewareChain chain) {
        AiSource source = source(event);
        if (source == null) {
            chain.doHandle(client, event);
            return;
        }
        botRegistrar.ensure(client, event);
        Language language = language(event);
        try {
            if (!isAllowed(event) || isLimited(client, event, language)) return;
            if (!chatPipeline.chat(client, event, source))
                send(client, event, MsgContent.of(Text.AI_GUARD_REFUSED.of(language)));
        } catch (RuntimeException e) {
            log.error("AI handling failed: {}", e.getMessage(), e);
            send(client, event, MsgContent.of(Text.CMD_ERROR.of(language, e.getMessage())));
        } finally {
            chain.consume();
        }
    }

    // ════ Helpers ═════════════════

    /**
     * 判断事件对应的 AI 来源。
     * <p>Resolves the AI source of the event.</p>
     *
     * @param event the inbound event
     * @return the source, or null when the event is not an AI trigger
     */
    private AiSource source(BotEvent event) {
        return switch (event) {
            case GroupMsgEvent e -> {
                if (e.selfId().equals(e.userId()) || cmdParser.isCmd(e.content())) yield null;
                if (e.at()) yield AiSource.GROUP_CHAT;
                if (!isAuto(e)) yield null;
                log.info("Auto chat: group={}, user={}", e.groupId(), e.userId());
                yield AiSource.GROUP_AUTO_CHAT;
            }
            case PrivateMsgEvent e -> cmdParser.isCmd(e.content()) ? null : AiSource.PRIVATE_CHAT;
            case GroupPokeEvent e -> e.selfId().equals(e.targetId()) ? AiSource.GROUP_POKE : null;
            case PrivatePokeEvent e -> e.selfId().equals(e.targetId()) ? AiSource.PRIVATE_POKE : null;
            default -> null;
        };
    }

    /**
     * 选择回复语言：群事件使用群语言，私聊使用默认语言。
     * <p>Selects the reply language: the group language for group events, the default language for private events.</p>
     *
     * @param event the inbound event
     * @return the reply language
     */
    private Language language(BotEvent event) {
        return event instanceof GroupEvent e
                ? languageService.group(e.groupId())
                : languageService.defaultLanguage();
    }

    /**
     * 判断当前会话是否允许 AI 对话。
     * <p>Whether the chat allows an AI conversation.</p>
     *
     * @param event the triggering event
     * @return true when AI chat is allowed
     */
    private boolean isAllowed(BotEvent event) {
        if (event instanceof GroupEvent e) {
            boolean allowed = groupService.isAiChatEnabled(e.groupId());
            if (!allowed) log.info("AI disabled: group={}", e.groupId());
            return allowed;
        }
        boolean allowed = userService.isPrivateAiChatEnabled(event.userId());
        if (!allowed) log.info("AI disabled: user={}", event.userId());
        return allowed;
    }

    /**
     * 对群 AI 触发应用群功能限速。
     * <p>Applies the group rate limit to AI triggers.</p>
     *
     * @param client   the outbound port
     * @param event    the triggering event
     * @param language the reply language
     * @return true when the trigger is rate limited
     */
    private boolean isLimited(BotClient client, BotEvent event, Language language) {
        if (!(event instanceof GroupEvent e)) return false;
        if (groupRateLimiter.tryConsume(e.groupId(), event.userId(), LIMIT_KEY)) return false;
        log.info("AI rate limited: group={}, user={}", e.groupId(), event.userId());
        client.sendGroupMsg(e.groupId(), MsgContent.of(Text.TOO_MANY_REQUESTS.of(language)));
        return true;
    }

    /**
     * 判断普通群消息是否触发自动聊天。
     * <p>Whether an ordinary group message triggers an automatic chat.</p>
     *
     * @param event the group message event
     * @return true when the message should trigger an automatic chat
     */
    private boolean isAuto(GroupMsgEvent event) {
        GroupSetting setting = groupSettingService.get(event.groupId());
        if (!Boolean.TRUE.equals(setting.getAiAutoChatEnabled())) return false;
        Double frequency = setting.getAiChatFrequency();
        return frequency != null && ThreadLocalRandom.current().nextDouble() < frequency;
    }

    /**
     * 按事件类型发送消息。
     * <p>Sends a message according to the event type.</p>
     *
     * @param client  the outbound port
     * @param event   the triggering event
     * @param content the message content
     */
    private static void send(BotClient client, BotEvent event, MsgContent content) {
        if (event instanceof GroupEvent e) client.sendGroupMsg(e.groupId(), content);
        else client.sendPrivateMsg(event.userId(), content);
    }
}
