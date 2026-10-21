package com.zincoid.nullbot.bot.chat;

import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.ai.client.BotAiClient;
import com.zincoid.nullbot.module.ai.memory.Memory;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.model.Effort;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.platform.util.ChatIds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 机器人聊天编排：锁定会话、读取记忆、调用模型、发送并写回回复。
 * <p>Bot chat orchestration: locks the chat, reads memory, invokes the model, sends and stores the reply.</p>
 */
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
    private final ReentrantLock[] locks = stripes();

    // ════ Reply ═══════════════════

    /**
     * 回复群聊消息。
     * <p>Replies to a group message.</p>
     *
     * @param client    the outbound port
     * @param groupId   group platform id
     * @param selfId    the bot's platform user id
     * @param messageId the triggering platform message id
     */
    public void chatGroup(BotClient client, String groupId, String selfId, String messageId) {
        chat(client, ChatIds.ofGroup(groupId), selfId, groupId, true,
                "The message to respond to is id=" + messageId + " in a group chat.");
    }

    /**
     * 回复私聊消息。
     * <p>Replies to a private message.</p>
     *
     * @param client    the outbound port
     * @param userId    sender platform user id
     * @param selfId    the bot's platform user id
     * @param messageId the triggering platform message id
     */
    public void chatPrivate(BotClient client, String userId, String selfId, String messageId) {
        chat(client, ChatIds.ofPrivate(userId), selfId, userId, false,
                "The message to respond to is id=" + messageId + " in a private chat.");
    }

    /**
     * 回复群聊中的戳一戳。
     * <p>Responds to a poke in a group chat.</p>
     *
     * @param client  the outbound port
     * @param groupId group platform id
     * @param selfId  the bot's platform user id
     * @param userId  poker platform user id
     */
    public void pokeGroup(BotClient client, String groupId, String selfId, String userId) {
        chat(client, ChatIds.ofGroup(groupId), selfId, groupId, true,
                "User " + nickname(client, userId) + "(" + userId
                        + ") just poked you in a group chat; respond to the poke.");
    }

    /**
     * 回复私聊中的戳一戳。
     * <p>Responds to a poke in a private chat.</p>
     *
     * @param client the outbound port
     * @param userId sender platform user id
     * @param selfId the bot's platform user id
     */
    public void pokePrivate(BotClient client, String userId, String selfId) {
        chat(client, ChatIds.ofPrivate(userId), selfId, userId, false,
                "User " + nickname(client, userId) + "(" + userId
                        + ") just poked you in a private chat; respond to the poke.");
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
     * @param trigger  the trigger note appended to the prompt
     */
    private void chat(BotClient client, String chatId, String selfId, String targetId, boolean group, String trigger) {
        ReentrantLock lock = lockOf(chatId);
        lock.lock();
        try {
            String selfName = selfName(client, selfId);
            ToolContext toolContext = new BotToolCtx(client, chatId, selfId, selfName, targetId, group, memory);
            String reply = botAiClient.invoke(memory.get(chatId), prompt(trigger), Effort.HIGH, toolContext);
            String replyId = send(client, targetId, group, MsgContent.builder().text(reply).build());
            memory.add(chatId, replyId, selfId, selfName, AiMessage.assistant(reply));
        } finally {
            lock.unlock();
        }
    }

    // ════ Helpers ═════════════════

    /**
     * 拼接系统提示词。
     * <p>Builds the system prompt.</p>
     *
     * @param trigger the trigger note, nullable
     * @return the system prompt
     */
    private String prompt(String trigger) {
        String base = configService.get(Conf.AI_CHAT_PROMPT);
        return Stream.of(base, FORMAT_PROMPT, trigger)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n"));
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
                              String targetId, boolean group, Memory memory) implements ToolContext {

        @Override
        public String send(String text, String replyId) {
            MsgContent.Builder builder = MsgContent.builder().text(text);
            if (replyId != null && !replyId.isBlank()) builder.reply(replyId);
            String messageId = ChatFlow.send(client, targetId, group, builder.build());
            memory.add(chatId, messageId, selfId, selfName, AiMessage.assistant(text));
            return messageId;
        }
    }
}
