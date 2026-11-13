package com.zincoid.nullbot.bot.aichat;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.ai.agent.Agent;
import com.zincoid.nullbot.module.ai.agent.AgentOptions;
import com.zincoid.nullbot.module.ai.memory.Memory;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import com.zincoid.nullbot.module.ai.model.TokenUsage;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.module.setting.UserSettingService;
import com.zincoid.nullbot.module.system.AiUsageService;
import com.zincoid.nullbot.platform.persistence.enums.AiSource;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import com.zincoid.nullbot.platform.persistence.po.UserSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 机器人聊天流水线：锁定会话、读取记忆、调用模型、发送并写回回复。
 * <p>Bot chat pipeline: locks the chat, reads memory, invokes the model, sends and stores the reply.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPipeline {

    private static final int LOCK_STRIPES = 64;

    private final ReentrantLock[] locks = initLocks();

    private final Agent agent;
    private final Memory memory;
    private final ConfigService configService;
    private final GroupSettingService groupSettingService;
    private final UserSettingService userSettingService;
    private final ChatGuard chatGuard;
    private final ChatPrompt chatPrompt;
    private final ChatActions chatActions;
    private final ChatCommands chatCommands;
    private final AiUsageService aiUsageService;

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
        return run(ChatTurn.of(client, event), source);
    }

    // ════ Core ════════════════════

    /**
     * 执行一次会话：校验注入、组装上下文，锁定会话后调用模型并写回回复。
     * <p>Runs one conversation: checks injection, builds the context, then locks the chat to invoke the model and store the reply.</p>
     *
     * @param turn     the turn data
     * @param aiSource the trigger source
     * @return true when the AI turn ran, false when the message was blocked by the injection guard
     */
    private boolean run(ChatTurn turn, AiSource aiSource) {
        Profile profile = profile(turn.chatType(), turn.targetId());
        if (turn.event() instanceof MsgEvent e && profile.guard() && chatGuard.check(e.content().text())) {
            log.info("Injection blocked: chat={}", turn.chatId());
            return false;
        }
        String prompt = chatPrompt.system(turn.event(), aiSource, profile.prompt());
        AgentOptions options = options(profile.effort());
        ToolContext ctx = new ChatToolContext(turn, chatActions, chatCommands);
        ReentrantLock lock = getLock(turn.chatId());
        lock.lock();
        try {
            ModelRes res = agent.invoke(ctx, memory.get(turn.chatId()), prompt, options);
            record(turn.chatType(), turn.targetId(), turn.event().userId(), aiSource, res.getUsage());
            if (turn.hasFinalSent()) return true;
            String content = ChatText.compact(res.getContent());
            chatActions.deliver(turn, MsgContent.of(content), content, res.getReasoning(), true);
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
     * @param effort the reasoning effort
     * @return the agent options
     */
    private AgentOptions options(Effort effort) {
        return new AgentOptions(
                effort,
                configService.getInt(Conf.AI_MAX_TOKENS),
                configService.getInt(Conf.AI_MAX_TOOL_CALLS));
    }

    /**
     * 读取会话的 AI 设置。
     * <p>Reads the AI settings of the chat.</p>
     *
     * @param chatType the chat type
     * @param targetId group or user platform id
     * @return the chat profile
     */
    private Profile profile(ChatType chatType, String targetId) {
        return switch (chatType) {
            case GROUP -> {
                GroupSetting setting = groupSettingService.get(targetId);
                yield new Profile(
                        setting.getAiPrompt(),
                        setting.getAiEffort(),
                        setting.getAiInjectionGuard());
            }
            case PRIVATE -> {
                UserSetting setting = userSettingService.get(targetId);
                yield new Profile(
                        setting.getPrivateAiPrompt(),
                        setting.getPrivateAiEffort(),
                        setting.getPrivateAiInjectionGuard());
            }
        };
    }

    private record Profile(String prompt, Effort effort, boolean guard) {
    }

    /**
     * 记录一次 AI 回合的用量，失败时不影响对话。
     * <p>Records one AI turn usage; failures do not affect the conversation.</p>
     *
     * @param chatType the chat type
     * @param targetId group or user platform id
     * @param userId   the triggering user platform id
     * @param source   the trigger source
     * @param usage    the token usage, nullable
     */
    private void record(ChatType chatType, String targetId, String userId,
                        AiSource source, TokenUsage usage) {
        try {
            aiUsageService.increase(
                    chatType,
                    targetId,
                    userId,
                    source,
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
