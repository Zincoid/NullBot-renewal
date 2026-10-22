package com.zincoid.nullbot.bot.chat;

import com.zincoid.nullbot.bot.bus.CmdProcessor;
import com.zincoid.nullbot.bot.bus.CmdRequest;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.ai.client.BotAiClient;
import com.zincoid.nullbot.module.ai.memory.Memory;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import com.zincoid.nullbot.platform.util.ChatIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 机器人聊天编排：锁定会话、读取记忆、调用模型、发送并写回回复。
 * <p>Bot chat orchestration: locks the chat, reads memory, invokes the model, sends and stores the reply.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatFlow {

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

    private final BotAiClient botAiClient;
    private final Memory memory;
    private final ConfigService configService;
    private final GroupSettingService groupSettingService;
    private final CmdProcessor cmdProcessor;
    private final ChatGuard chatGuard;
    private final ReentrantLock[] locks = stripes();

    // ════ Reply ═══════════════════

    /**
     * 回复群聊消息。
     * <p>Replies to a group message.</p>
     *
     * @param client   the outbound port
     * @param event    the triggering group message event
     * @param language the reply language
     */
    public void chat(BotClient client, GroupMsgEvent event, Language language) {
        chat(client, ChatIds.ofGroup(event.groupId()), event.selfId(), event.groupId(), true, language,
                "The message to respond to is id=" + event.messageId() + " in a group chat.", event);
    }

    /**
     * 回复私聊消息。
     * <p>Replies to a private message.</p>
     *
     * @param client   the outbound port
     * @param event    the triggering private message event
     * @param language the reply language
     */
    public void chat(BotClient client, PrivateMsgEvent event, Language language) {
        chat(client, ChatIds.ofPrivate(event.userId()), event.selfId(), event.userId(), false, language,
                "The message to respond to is id=" + event.messageId() + " in a private chat.", event);
    }

    /**
     * 回复群聊中的戳一戳。
     * <p>Responds to a poke in a group chat.</p>
     *
     * @param client   the outbound port
     * @param event    the triggering group poke event
     * @param language the reply language
     */
    public void poke(BotClient client, GroupPokeEvent event, Language language) {
        chat(client, ChatIds.ofGroup(event.groupId()), event.selfId(), event.groupId(), true, language,
                "User " + nickname(client, event.userId()) + "(" + event.userId()
                        + ") just poked you in a group chat; respond to the poke.", event);
    }

    /**
     * 回复私聊中的戳一戳。
     * <p>Responds to a poke in a private chat.</p>
     *
     * @param client   the outbound port
     * @param event    the triggering private poke event
     * @param language the reply language
     */
    public void poke(BotClient client, PrivatePokeEvent event, Language language) {
        chat(client, ChatIds.ofPrivate(event.userId()), event.selfId(), event.userId(), false, language,
                "User " + nickname(client, event.userId()) + "(" + event.userId()
                        + ") just poked you in a private chat; respond to the poke.", event);
    }

    // ════ Core ════════════════════

    /**
     * 执行一次会话：锁定、组装上下文、调用模型、发送并写回回复。
     * <p>Runs one conversation: locks, builds the context, invokes the model, sends and stores the reply.</p>
     *
     * @param client   the outbound port
     * @param chatId   chat identifier
     * @param selfId   the bot's platform user id
     * @param targetId group or user platform id
     * @param group    whether the target is a group
     * @param language the reply language
     * @param trigger  the trigger note appended to the prompt
     * @param event    the triggering event
     */
    private void chat(BotClient client, String chatId, String selfId, String targetId, boolean group,
                      Language language, String trigger, BotEvent event) {
        ReentrantLock lock = lockOf(chatId);
        lock.lock();
        try {
            GroupSetting setting = group ? groupSettingService.get(targetId) : null;
            if (group && event instanceof GroupMsgEvent e
                    && Boolean.TRUE.equals(setting.getAiInjectionGuard())
                    && chatGuard.check(text(e))) {
                send(client, targetId, true,
                        MsgContent.of(Text.AI_GUARD_REFUSED.of(language)));
                log.info("Injection blocked: chat={}", chatId);
                return;
            }
            String selfName = selfName(client, selfId);
            ToolContext toolContext = new BotToolCtx(
                    client, chatId, selfId, selfName, targetId, group, memory, event, cmdProcessor);
            ModelRes reply = botAiClient.invoke(memory.get(chatId),
                    prompt(promptBase(setting), trigger), effort(setting), toolContext);
            String content = reply.getContent();
            String replyId = send(client, targetId, group,
                    MsgContent.builder().text(content).build());
            memory.add(chatId, replyId, selfId, selfName,
                    AiMessage.assistant(content).withReasoning(reply.getReasoning()));
        } finally {
            lock.unlock();
        }
    }

    // ════ Helpers ═════════════════

    /**
     * 提取消息中的纯文本。
     * <p>Extracts the plain text of a message.</p>
     *
     * @param event the group message event
     * @return the concatenated text segments
     */
    private static String text(GroupMsgEvent event) {
        StringBuilder text = new StringBuilder();
        for (MsgSegment segment : event.content().segments())
            if (segment instanceof MsgSegment.Text(String s)) text.append(s);
        return text.toString();
    }

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
     * 选择会话的提示词基底：群设置优先，否则使用全局配置。
     * <p>Selects the prompt base of the chat: the group setting wins, otherwise the global configuration.</p>
     *
     * @param setting group settings, null for private chats
     * @return the prompt base
     */
    private String promptBase(GroupSetting setting) {
        if (setting != null && setting.getAiPrompt() != null && !setting.getAiPrompt().isBlank())
            return setting.getAiPrompt();
        return configService.get(Conf.AI_DEFAULT_PROMPT);
    }

    /**
     * 选择推理强度，未设置时使用 HIGH。
     * <p>Selects the reasoning effort, falling back to HIGH when unset.</p>
     *
     * @param setting group settings, null for private chats
     * @return the reasoning effort
     */
    private static Effort effort(GroupSetting setting) {
        return setting == null || setting.getAiEffort() == null ? Effort.HIGH : setting.getAiEffort();
    }

    /**
     * 查询机器人昵称，失败时回退为 ID。
     * <p>Looks up the bot nickname, falling back to the id.</p>
     *
     * @param client the outbound port
     * @param selfId the bot's platform user id
     * @return the nickname or the id
     */
    private static String selfName(BotClient client, String selfId) {
        try {
            return client.getLoginInfo().nickname();
        } catch (RuntimeException e) {
            return selfId;
        }
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

    private record BotToolCtx(BotClient client, String chatId, String selfId, String selfName,
                              String targetId, boolean group, Memory memory, BotEvent event,
                              CmdProcessor cmdProcessor)
            implements ToolContext {

        @Override
        public String send(String text, String replyId) {
            MsgContent.Builder builder = MsgContent.builder().text(text);
            if (replyId != null && !replyId.isBlank()) builder.reply(replyId);
            String messageId = ChatFlow.send(client, targetId, group, builder.build());
            memory.add(chatId, messageId, selfId, selfName, AiMessage.assistant(text));
            return messageId;
        }

        @Override
        public ToolRes command(String name, List<String> params) {
            cmdProcessor.process(client, event, new CmdRequest(name, params));
            return ToolRes.of("Command executed: " + name);
        }
    }
}
