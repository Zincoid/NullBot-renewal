package com.zincoid.nullbot.bot.chat;

import com.zincoid.nullbot.bot.BotClients;
import com.zincoid.nullbot.bot.cmd.CmdDispatcher;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.event.PokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.ai.client.AgentClient;
import com.zincoid.nullbot.module.ai.client.AgentOptions;
import com.zincoid.nullbot.module.ai.memory.Memory;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.module.setting.UserSettingService;
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
            Messages are prefixed with [id=<message id>].
            A [Reply id=<message id>] marker quotes another message; use the read tool when you need its content or attachments.
            @你 means the user mentioned you.
            Image data is not included in history; use read to list attachment file ids and view to view an image.
            You can call send to send a short intermediate message while working; still send a final reply.
            The trigger note tells you which message to reply to.
            Do not include the id, reply, or mention markers in your reply.
            
            Reply like a person texting in a chat app:
            - Keep it short and conversational, and match the user's language.
            - Never write documents, reports, headings, bullet lists, or tables.
            - Avoid markdown formatting unless the user asks for code.
            - Use a line break only when it is truly needed, and never output blank lines.""";

    private static final int LOCK_STRIPES = 64;

    private final AgentClient agentClient;
    private final Memory memory;
    private final ConfigService configService;
    private final GroupSettingService groupSettingService;
    private final UserSettingService userSettingService;
    private final CmdDispatcher cmdDispatcher;
    private final ChatGuard chatGuard;
    private final FileService fileService;
    private final ReentrantLock[] locks = stripes();

    // ════ Reply ═══════════════════

    public void chat(BotClient client, GroupMsgEvent event, Language language) {
        chat(client, ChatIds.ofGroup(event.groupId()), event.selfId(), event.groupId(), true, language, event);
    }

    public void chat(BotClient client, PrivateMsgEvent event, Language language) {
        chat(client, ChatIds.ofPrivate(event.userId()), event.selfId(), event.userId(), false, language, event);
    }

    public void poke(BotClient client, GroupPokeEvent event, Language language) {
        chat(client, ChatIds.ofGroup(event.groupId()), event.selfId(), event.groupId(), true, language, event);
    }

    public void poke(BotClient client, PrivatePokeEvent event, Language language) {
        chat(client, ChatIds.ofPrivate(event.userId()), event.selfId(), event.userId(), false, language, event);
    }

    // ════ Core ════════════════════

    /**
     * 执行一次会话：校验注入、组装上下文，锁定会话后调用模型并写回回复。
     * <p>Runs one conversation: checks injection, builds the context, then locks the chat to invoke the model and store the reply.</p>
     *
     * @param client   the outbound port
     * @param chatId   chat identifier
     * @param selfId   the bot's platform user id
     * @param targetId group or user platform id
     * @param group    whether the target is a group
     * @param language the reply language
     * @param event    the triggering event
     */
    private void chat(BotClient client, String chatId, String selfId, String targetId,
                      boolean group, Language language, BotEvent event) {
        GroupSetting groupSetting = group ? groupSettingService.get(targetId) : null;
        UserSetting userSetting = group ? null : userSettingService.get(targetId);
        boolean guard = group
                ? groupSetting.getAiInjectionGuard()
                : userSetting.getAiInjectionGuard();
        if (event instanceof MsgEvent msg && guard && chatGuard.check(msg.content().text())) {
            send(client, targetId, group, MsgContent.of(Text.AI_GUARD_REFUSED.of(language)));
            log.info("Injection blocked: chat={}", chatId);
            return;
        }
        String base = promptBase(group ? groupSetting.getAiPrompt() : userSetting.getAiPrompt());
        Effort effort = group ? groupSetting.getAiEffort() : userSetting.getAiEffort();
        AgentOptions options = options();
        String trigger = trigger(event);
        String selfName = BotClients.selfName(client, selfId);
        ChatContext ctx = new ChatContext(client, targetId, group, event, cmdDispatcher, fileService);
        ReentrantLock lock = lockOf(chatId);
        lock.lock();
        try {
            List<AiMessage> history = new ArrayList<>(memory.get(chatId));
            if (event instanceof PokeEvent poke) {
                AiMessage incoming = AiMessage.user(poke.userId(), "[Rubbed you]", List.of());
                history.add(incoming);
                memory.add(chatId, "poke-" + UUID.randomUUID(),
                        poke.userId(), poke.userId(), incoming);
            }
            ModelRes reply = agentClient.invoke(history, prompt(base, trigger), effort, ctx, options);
            String content = ChatText.compact(reply.getContent());
            String replyId = send(client, targetId, group, MsgContent.of(content));
            memory.add(chatId, replyId, selfId, selfName,
                    AiMessage.assistant(content).withReasoning(reply.getReasoning()));
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
    private static String prompt(String base, String trigger) {
        return Stream.of(base, FORMAT_PROMPT, trigger)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 构建触发说明。
     * <p>Builds the trigger note.</p>
     *
     * @param event the triggering event
     * @return the trigger note, or null for unsupported events
     */
    private static String trigger(BotEvent event) {
        return switch (event) {
            case GroupMsgEvent e -> "The message to respond to is id=" + e.messageId() + " in a group chat.";
            case PrivateMsgEvent e -> "The message to respond to is id=" + e.messageId() + " in a private chat.";
            case GroupPokeEvent e -> "User " + e.userId() + " just rubbed you in a group chat; respond to it.";
            case PrivatePokeEvent e -> "User " + e.userId() + " just rubbed you in a private chat; respond to it.";
            default -> null;
        };
    }

    /**
     * 选择会话的提示词基底：群设置或用户设置优先，否则使用全局配置。
     * <p>Selects the prompt base of the chat: the group or user setting wins, otherwise the global configuration.</p>
     *
     * @param prompt the per-chat prompt, nullable
     * @return the prompt base
     */
    private String promptBase(String prompt) {
        if (prompt != null && !prompt.isBlank()) return prompt;
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
