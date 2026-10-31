package com.zincoid.nullbot.bot.bus.middleware;

import com.zincoid.nullbot.bot.bus.BotRegistrar;
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
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * AI 对话中间件：把群 @、私聊、戳一戳和自动聊天转为 AI 对话。
 * <p>AI chat middleware: turns group mentions, private messages, pokes, and auto chat into AI conversations.</p>
 */
@Slf4j
@Order(3)
@Component
@RequiredArgsConstructor
public class AiChatMiddleware implements Middleware {

    private static final String RATE_KEY = "AI";

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
        if (event instanceof GroupMsgEvent e) {
            if (e.selfId().equals(e.userId()) || cmdParser.parse(e) != null) {
                chain.doHandle(client, event);
                return;
            }
            Language language = languageService.group(e.groupId());
            if (e.at()) {
                run(client, e, language, chain, () -> chatPipeline.chat(client, e, language));
                return;
            }
            if (isAuto(e)) {
                log.info("Auto chat: group={}, user={}", e.groupId(), e.userId());
                run(client, e, language, chain, () -> chatPipeline.chat(client, e, language));
                return;
            }
            chain.doHandle(client, event);
            return;
        }
        if (event instanceof PrivateMsgEvent e) {
            if (cmdParser.parse(e) != null) {
                chain.doHandle(client, event);
                return;
            }
            Language language = languageService.defaultLanguage();
            run(client, e, language, chain, () -> chatPipeline.chat(client, e, language));
            return;
        }
        if (event instanceof GroupPokeEvent e) {
            if (!e.selfId().equals(e.targetId())) {
                chain.doHandle(client, event);
                return;
            }
            Language language = languageService.group(e.groupId());
            run(client, e, language, chain, () -> chatPipeline.poke(client, e, language));
            return;
        }
        if (event instanceof PrivatePokeEvent e) {
            if (!e.selfId().equals(e.targetId())) {
                chain.doHandle(client, event);
                return;
            }
            Language language = languageService.defaultLanguage();
            run(client, e, language, chain, () -> chatPipeline.poke(client, e, language));
            return;
        }
        chain.doHandle(client, event);
    }

    // ════ Helpers ═════════════════

    /**
     * 执行 AI 调用，消费事件并统一处理异常。
     * <p>Runs an AI call, consumes the event, and handles exceptions uniformly.</p>
     *
     * @param client   the outbound port
     * @param event    the triggering event
     * @param language the reply language
     * @param chain    the remaining middleware chain
     * @param action   the AI action
     */
    private void run(BotClient client, BotEvent event, Language language,
                     MiddlewareChain chain, Runnable action) {
        try {
            botRegistrar.ensure(client, event);
            if (!isAllowed(event)) return;
            if (!isLimited(client, event, language)) return;
            action.run();
        } catch (RuntimeException e) {
            log.error("AI reply failed: {}", e.getMessage(), e);
            sendError(client, event, language, e);
        } finally {
            chain.consume();
        }
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
            if (!allowed) log.info("AI disabled: {}", e.groupId());
            return allowed;
        }
        boolean allowed = userService.isPrivateAiChatEnabled(event.userId());
        if (!allowed) log.info("AI disabled: {}", event.userId());
        return allowed;
    }

    /**
     * 对群 AI 触发应用群功能限速。
     * <p>Applies the group rate limit to AI triggers.</p>
     *
     * @param client   the outbound port
     * @param event    the triggering event
     * @param language the reply language
     * @return true when the trigger is allowed
     */
    private boolean isLimited(BotClient client, BotEvent event, Language language) {
        if (!(event instanceof GroupEvent e)) return true;
        if (groupRateLimiter.tryConsume(e.groupId(), event.userId(), RATE_KEY)) return true;
        log.info("AI rate limited: group={}, user={}, key={}",
                e.groupId(), event.userId(), RATE_KEY);
        if (event instanceof GroupMsgEvent)
            client.sendGroupMsg(e.groupId(), MsgContent.of(Text.TOO_MANY_REQUESTS.of(language)));
        return false;
    }

    /**
     * 判断普通群消息是否触发自动聊天。
     * <p>Whether an ordinary group message triggers an automatic chat.</p>
     *
     * @param event the group message event
     * @return true when the message should trigger an automatic chat
     */
    private boolean isAuto(GroupMsgEvent event) {
        if (event.selfId().equals(event.userId())) return false;
        if (event.at()) return false;
        if (cmdParser.isCmd(event.content())) return false;
        GroupSetting setting = groupSettingService.get(event.groupId());
        if (!Boolean.TRUE.equals(setting.getAiAutoChatEnabled())) return false;
        Double frequency = setting.getAiChatFrequency();
        if (frequency == null) return false;
        return ThreadLocalRandom.current().nextDouble() < frequency;
    }

    /**
     * 向触发会话发送 AI 错误提示。
     * <p>Sends an AI error notice to the triggering chat.</p>
     *
     * @param client   the outbound port
     * @param event    the triggering event
     * @param language the reply language
     * @param error    the failure
     */
    private static void sendError(BotClient client, BotEvent event,
                                  Language language, RuntimeException error) {
        MsgContent content = MsgContent.of(Text.CMD_ERROR.of(language, error.getMessage()));
        if (event instanceof GroupEvent e) client.sendGroupMsg(e.groupId(), content);
        else client.sendPrivateMsg(event.userId(), content);
    }
}
