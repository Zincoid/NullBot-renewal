package com.zincoid.nullbot.bot.aichat;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.ai.agent.ChatAgent;
import com.zincoid.nullbot.module.ai.agent.ChatRes;
import com.zincoid.nullbot.module.ai.agent.ChatOptions;
import com.zincoid.nullbot.module.ai.context.ContextPacket;
import com.zincoid.nullbot.module.ai.model.ModelUsage;
import com.zincoid.nullbot.module.ai.tool.ToolHost;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.memory.MemoryService;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.module.setting.UserSettingService;
import com.zincoid.nullbot.module.system.AiUsageService;
import com.zincoid.nullbot.platform.persistence.enums.AiSource;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import com.zincoid.nullbot.platform.persistence.po.UserSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 机器人聊天流水线：锁定会话、调用模型、发送并写回回复。
 * <p>Bot chat pipeline: locks the chat, invokes the model, sends and stores the reply.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPipeline {

    private static final int LOCK_STRIPES = 64;

    private final ReentrantLock[] locks = initLocks();

    private final ChatAgent chatAgent;
    private final ConfigService configService;
    private final GroupSettingService groupSettingService;
    private final UserSettingService userSettingService;
    private final MemoryService memoryService;
    private final AiUsageService aiUsageService;
    private final ChatGuard chatGuard;
    private final ChatActions chatActions;
    private final ChatCommands chatCommands;

    // ════ Chat ════════════════════

    /**
     * 按触发事件执行一次 AI 回复，被注入防护拦截时返回 false。
     * <p>Runs one AI reply for the triggering event; returns false when the injection guard blocks it.</p>
     *
     * @param client the outbound port
     * @param event  the triggering event
     * @param source the trigger source
     * @return true when the AI turn ran, false when the message was blocked by the injection guard
     */
    public boolean chat(BotClient client, BotEvent event, AiSource source) {
        return run(ChatTurn.of(client, event, source));
    }

    // ════ Core ════════════════════

    /**
     * 执行一次会话：校验注入、组装上下文，锁定会话后调用模型并写回回复。
     * <p>Runs one conversation: checks injection, builds the context, then locks the chat to invoke the model and store the reply.</p>
     *
     * @param turn the turn data
     * @return true when the AI turn ran, false when the message was blocked by the injection guard
     */
    private boolean run(ChatTurn turn) {
        Profile profile = profile(turn);
        if (turn.event() instanceof MsgEvent e
                && profile.guard() && chatGuard.check(e.content().text())) {
            log.info("Injection blocked: chat={}", turn.chatId());
            return false;
        }
        ChatOptions options = options(profile);
        List<ContextPacket> promptContext = ChatPrompt.context(
                turn.event(), turn.source(), profile.prompt(),
                configService.getString(Conf.AI_DEFAULT_PROMPT));
        List<ContextPacket> imageContext = ChatImages.context(
                turn.client(), turn.event());
        ToolHost host = new ChatToolHost(turn, chatActions, chatCommands);
        ReentrantLock lock = getLock(turn.chatId());
        lock.lock();
        try {
            ChatRes res = chatAgent.chat(turn.chatId())
                    .options(options)
                    .context(promptContext)
                    .context(imageContext)
                    .tool(host)
                    .call();
            record(turn, res.usage());
            if (res.finalSent()) return true;
            String content = ChatText.compact(res.content());
            chatActions.deliver(turn, MsgContent.of(content), content, res.reasoning());
        } finally {
            lock.unlock();
        }
        return true;
    }

    // ════ Helpers ═════════════════

    /**
     * 读取单次对话的调用参数。
     * <p>Reads the invocation options for one conversation.</p>
     *
     * @param profile the chat profile
     * @return the agent options
     */
    private ChatOptions options(Profile profile) {
        return new ChatOptions(
                profile.effort(),
                configService.getInt(Conf.AI_MAX_TOKENS),
                configService.getInt(Conf.AI_MAX_TOOL_CALLS),
                profile.episodicMemory(),
                profile.semanticMemory());
    }

    /**
     * 读取会话的 AI 设置。
     * <p>Reads the AI settings of the chat.</p>
     *
     * @param turn the turn data
     * @return the chat profile
     */
    private Profile profile(ChatTurn turn) {
        String chatId = turn.chatId();
        boolean episodicMemory = memoryService.isEpisodicEnabled(chatId);
        boolean semanticMemory = memoryService.isSemanticEnabled(chatId);
        return switch (turn.chatType()) {
            case GROUP -> {
                GroupSetting setting = groupSettingService.get(turn.targetId());
                yield new Profile(
                        setting.getAiPrompt(),
                        setting.getAiEffort(),
                        setting.getAiInjectionGuardEnabled(),
                        episodicMemory,
                        semanticMemory);
            }
            case PRIVATE -> {
                UserSetting setting = userSettingService.get(turn.targetId());
                yield new Profile(
                        setting.getPrivateAiPrompt(),
                        setting.getPrivateAiEffort(),
                        setting.getPrivateAiInjectionGuardEnabled(),
                        episodicMemory,
                        semanticMemory);
            }
        };
    }

    private record Profile(String prompt, Effort effort, boolean guard,
                           boolean episodicMemory, boolean semanticMemory) {
    }

    /**
     * 记录一次 AI 回合的用量，失败时不影响对话。
     * <p>Records one AI turn usage; failures do not affect the conversation.</p>
     *
     * @param turn  the turn data
     * @param usage the token usage, nullable
     */
    private void record(ChatTurn turn, ModelUsage usage) {
        try {
            aiUsageService.increase(
                    turn.chatType(),
                    turn.targetId(),
                    turn.event().userId(),
                    turn.source(),
                    usage == null ? null : usage.inputTokens(),
                    usage == null ? null : usage.outputTokens());
        } catch (RuntimeException e) {
            log.warn("Failed to record AI usage: {}", e.getMessage());
        }
    }

    /**
     * 选择会话对应的条带锁。
     * <p>Selects the striped lock for the chat.</p>
     *
     * @param chatId chat identifier
     * @return the striped lock
     */
    private ReentrantLock getLock(String chatId) {
        return locks[Math.floorMod(chatId.hashCode(), locks.length)];
    }

    /**
     * 创建固定数量的条带锁。
     * <p>Creates the fixed set of striped locks.</p>
     *
     * @return the striped locks
     */
    private static ReentrantLock[] initLocks() {
        ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];
        for (int i = 0; i < locks.length; i++) locks[i] = new ReentrantLock();
        return locks;
    }
}
