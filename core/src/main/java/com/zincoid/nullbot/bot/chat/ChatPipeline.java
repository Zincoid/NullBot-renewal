package com.zincoid.nullbot.bot.chat;

import com.zincoid.nullbot.bot.BotClients;
import com.zincoid.nullbot.bot.cmd.CmdDispatcher;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.event.PokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.ai.client.AgentClient;
import com.zincoid.nullbot.module.ai.client.AgentOptions;
import com.zincoid.nullbot.module.ai.memory.Memory;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import com.zincoid.nullbot.module.ai.model.TokenUsage;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.module.setting.UserSettingService;
import com.zincoid.nullbot.module.system.AiUsageService;
import com.zincoid.nullbot.platform.persistence.enums.AiSource;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import com.zincoid.nullbot.platform.persistence.po.UserSetting;
import com.zincoid.nullbot.platform.util.ChatIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 机器人聊天流水线：锁定会话、读取记忆、调用模型、发送并写回回复。
 * <p>Bot chat pipeline: locks the chat, reads memory, invokes the model, sends and stores the reply.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPipeline {

    private static final String FORMAT_PROMPT = """
            The following describes the chat history format and how to reply.

            History:
            - User messages are formatted as [id=<message id>] nickname(userId): text.
            - A [Reply id=<message id>] marker quotes another message; use the "read" tool to fetch its content or attachments.
            - @you means the user mentioned you.
            - Media data is not included in history; use the "read" tool to list attachment file ids and the "view" tool to view an image.
            - Notes in your own messages that record sent attachments are metadata, not text you said to the user.

            Rules:
            - You can call the "send" tool multiple times in one turn to send separate messages or media.
            - Set last=true on the send that completes your answer; no separate final reply will be sent. Otherwise, still produce a final reply, and never repeat what you already sent.
            - The trigger note identifies which message to respond to and how the conversation was triggered.
            - Use ids only as tool arguments; never write ids, history markers, or attachment notes in your reply.

            Reply like a person texting in a chat app:
            - Keep it short and conversational, and match the user's language.
            - Never write documents, reports, headings, bullet lists, or tables.
            - Avoid markdown formatting unless the user asks for code.
            - Use a line break only when it is truly needed, and never output blank lines.""";

    private static final int LOCK_STRIPES = 64;

    private final ReentrantLock[] locks = stripes();

    private final AgentClient agentClient;
    private final Memory memory;
    private final ConfigService configService;
    private final GroupSettingService groupSettingService;
    private final UserSettingService userSettingService;
    private final CmdDispatcher cmdDispatcher;
    private final ChatGuard chatGuard;
    private final FileService fileService;
    private final AiUsageService aiUsageService;

    // ════ Chat ════════════════════

    /**
     * 按触发事件执行一次 AI 回复。
     * <p>Runs one AI reply for the triggering event.</p>
     *
     * @param client   the outbound port
     * @param event    the triggering event
     * @param language the reply language
     * @param source   the trigger source
     */
    public void chat(BotClient client, BotEvent event, Language language, AiSource source) {
        if (event instanceof GroupEvent e) {
            run(client, ChatIds.ofGroup(e.groupId()), event.selfId(), e.groupId(), true, language, event, source);
        } else if (event instanceof PrivateEvent e) {
            run(client, ChatIds.ofPrivate(e.userId()), event.selfId(), e.userId(), false, language, event, source);
        }
    }

    // ════ Core ════════════════════

    /**
     * 执行一次会话：校验注入、组装上下文，锁定会话后调用模型并写回回复。
     * <p>Runs one conversation: checks injection, builds the context, then locks the chat to invoke the model and store the reply.</p>
     *
     * @param client    the outbound port
     * @param chatId    chat identifier
     * @param selfId    the bot's platform user id
     * @param targetId  group or user platform id
     * @param group     whether the target is a group
     * @param language  the reply language
     * @param event     the triggering event
     * @param aiSource  the trigger source
     */
    private void run(BotClient client, String chatId, String selfId, String targetId,
                     boolean group, Language language, BotEvent event, AiSource aiSource) {
        GroupSetting groupSetting = group ? groupSettingService.get(targetId) : null;
        UserSetting userSetting = group ? null : userSettingService.get(targetId);
        boolean guard = group
                ? groupSetting.getAiInjectionGuard()
                : userSetting.getPrivateAiInjectionGuard();
        if (event instanceof MsgEvent e && guard && chatGuard.check(e.content().text())) {
            send(client, targetId, group, MsgContent.of(Text.AI_GUARD_REFUSED.of(language)));
            log.info("Injection blocked: chat={}", chatId);
            return;
        }
        String base = prompt(group ? groupSetting.getAiPrompt() : userSetting.getPrivateAiPrompt());
        Effort effort = group ? groupSetting.getAiEffort() : userSetting.getPrivateAiEffort();
        AgentOptions options = options();
        String trigger = trigger(event, aiSource);
        String selfName = BotClients.selfName(client, selfId);
        ChatContext ctx = new ChatContext(client, targetId, group, event,
                cmdDispatcher, fileService, configService, memory, chatId, selfName,
                new AtomicBoolean());
        ReentrantLock lock = lockOf(chatId);
        lock.lock();
        try {
            List<AiMessage> history = new ArrayList<>(memory.get(chatId));
            if (event instanceof PokeEvent e) {
                AiMessage incoming = AiMessage.user(e.userId(), "[Rubbed you]", List.of());
                history.add(incoming);
                memory.add(chatId, "poke-" + UUID.randomUUID(),
                        e.userId(), e.userId(), incoming, List.of());
            }
            ModelRes reply = agentClient.invoke(history, compose(base, trigger), effort, ctx, options);
            record(group, targetId, event, aiSource, reply.getUsage());
            if (ctx.hasFinalSent()) return;
            String content = ChatText.compact(reply.getContent());
            String replyId = send(client, targetId, group, MsgContent.of(content));
            memory.add(chatId, replyId, selfId, selfName,
                    AiMessage.assistant(content).withReasoning(reply.getReasoning()), List.of());
        } finally {
            lock.unlock();
        }
    }

    // ════ Helpers ═════════════════

    /**
     * 拼接系统提示词。
     * <p>Builds the system prompt.</p>
     *
     * @param base    the base prompt
     * @param trigger the trigger note, nullable
     * @return the system prompt
     */
    private static String compose(String base, String trigger) {
        return Stream.of(base, FORMAT_PROMPT, trigger)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 构建触发说明。
     * <p>Builds the trigger note.</p>
     *
     * @param event  the triggering event
     * @param source the trigger source
     * @return the trigger note, or null for unsupported events
     */
    private static String trigger(BotEvent event, AiSource source) {
        return switch (event) {
            case GroupMsgEvent e -> "The message to respond to is id=" + e.messageId()
                    + " from " + sender(e) + " in a group chat."
                    + (source == AiSource.GROUP_AUTO_CHAT
                    ? " This is an automatic chat trigger; you were not mentioned."
                    : " The user mentioned you.");
            case PrivateMsgEvent e -> "The message to respond to is id=" + e.messageId()
                    + " from " + sender(e) + " in a private chat.";
            case GroupPokeEvent e -> "User " + e.userId() + " just rubbed you in a group chat; respond to it.";
            case PrivatePokeEvent e -> "User " + e.userId() + " just rubbed you in a private chat; respond to it.";
            default -> null;
        };
    }

    /**
     * 拼接发送者显示名与平台 ID。
     * <p>Joins the sender display name and platform id.</p>
     *
     * @param event the message event
     * @return the sender label
     */
    private static String sender(MsgEvent event) {
        String name = event.nickname();
        if (name == null || name.isBlank()) name = event.userId();
        return name + "(" + event.userId() + ")";
    }

    /**
     * 选择会话的提示词基底：群设置或用户设置优先，否则使用全局配置。
     * <p>Selects the prompt base of the chat: the group or user setting wins, otherwise the global configuration.</p>
     *
     * @param base the per-chat prompt, nullable
     * @return the prompt base
     */
    private String prompt(String base) {
        if (base != null && !base.isBlank()) return base;
        return configService.getString(Conf.AI_DEFAULT_PROMPT);
    }

    /**
     * 读取单次对话的输出与工具轮数上限。
     * <p>Reads the output and tool round limits for one conversation.</p>
     *
     * @return the agent options
     */
    private AgentOptions options() {
        return new AgentOptions(
                configService.getInt(Conf.AI_MAX_TOKENS),
                configService.getInt(Conf.AI_MAX_TOOL_CALLS));
    }

    /**
     * 记录一次 AI 回合的用量，失败时不影响对话。
     * <p>Records one AI turn usage; failures do not affect the conversation.</p>
     *
     * @param group     whether the target is a group
     * @param targetId  group or user platform id
     * @param event     the triggering event
     * @param source    the trigger source
     * @param usage     the token usage, nullable
     */
    private void record(boolean group, String targetId, BotEvent event,
                        AiSource source, TokenUsage usage) {
        try {
            aiUsageService.increase(
                    group ? ChatType.GROUP : ChatType.PRIVATE,
                    targetId,
                    event.userId(),
                    source,
                    usage == null ? null : usage.inputTokens(),
                    usage == null ? null : usage.outputTokens());
        } catch (RuntimeException e) {
            log.warn("Failed to record AI usage: {}", e.getMessage());
        }
    }

    /**
     * 按会话类型发送消息。
     * <p>Sends a message to a group or a user depending on the chat type.</p>
     *
     * @param client   the outbound port
     * @param targetId group or user platform id
     * @param group    whether the target is a group
     * @param content  the message content
     * @return the sent platform message id
     */
    private static String send(BotClient client, String targetId, boolean group, MsgContent content) {
        return group ? client.sendGroupMsg(targetId, content) : client.sendPrivateMsg(targetId, content);
    }

    /**
     * 选择会话对应的条带锁。
     * <p>Selects the striped lock for the chat.</p>
     *
     * @param chatId chat identifier
     * @return the striped lock
     */
    private ReentrantLock lockOf(String chatId) {
        return locks[Math.floorMod(chatId.hashCode(), locks.length)];
    }

    /**
     * 创建固定数量的条带锁。
     * <p>Creates the fixed set of striped locks.</p>
     *
     * @return the striped locks
     */
    private static ReentrantLock[] stripes() {
        ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];
        for (int i = 0; i < locks.length; i++) locks[i] = new ReentrantLock();
        return locks;
    }
}
