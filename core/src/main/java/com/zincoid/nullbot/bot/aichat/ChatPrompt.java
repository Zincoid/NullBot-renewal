package com.zincoid.nullbot.bot.aichat;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.module.ai.context.ContextPacket;
import com.zincoid.nullbot.module.ai.context.TextPacket;
import com.zincoid.nullbot.platform.persistence.enums.AiSource;
import com.zincoid.nullbot.platform.util.UserLabels;

import java.util.List;

/**
 * 聊天上下文：把系统提示词与触发说明组装为上下文信息包。
 * <p>Chat context: assembles the system prompt and trigger note into context packets.</p>
 */
final class ChatPrompt {

    private static final String FORMAT_PROMPT = """
            The following describes the chat history format and how to reply.
            
            History:
            - User messages are formatted as [id=<message id>] nickname(userId): text.
            - A [Reply id=<message id>] marker quotes another message.
            - @you means the user mentioned you.
            - Media data is not included in history.
            - Notes in your own messages that record sent media are metadata, not text you said to the user.
            
            Tools:
            - "read" takes a message id and returns its content and attachment ids.
            - "view" takes attachment=<id> for a chat attachment, or file=<id> for a collection file.
            - "search" finds collection files; "send" takes file=<id> to send one.
            
            Rules:
            - Each "send" call is exactly one chat bubble; call it multiple times for multiple bubbles.
            - A bubble may contain multiple sentences; do not use line breaks to separate chat sentences.
            - Use line breaks only inside fenced code blocks.
            - Never output blank lines.
            - Set last=true on the send that completes your answer; no separate final reply is then sent.
            - If no send has last=true, give a final reply without repeating what you already sent.
            - The trigger note identifies the message to respond to and how the turn was triggered.
            - Use ids only as tool arguments; never write ids, history markers, or attachment notes in your reply.
            
            Style:
            - Keep it short and conversational, and match the user's language.
            - Never write documents, reports, headings, bullet lists, or tables.
            - Avoid markdown formatting unless the user asks for code.""";

    private static final String GROUP_TRIGGER = "The message to respond to is id=%s from %s in a group chat. %s";
    private static final String GROUP_AUTO_NOTE = "This is an automatic chat trigger; you were not mentioned.";
    private static final String GROUP_MENTION_NOTE = "The user mentioned you.";
    private static final String PRIVATE_TRIGGER = "The message to respond to is id=%s from %s in a private chat.";
    private static final String GROUP_POKE_TRIGGER = "User %s just rubbed you in a group chat; respond to it.";
    private static final String PRIVATE_POKE_TRIGGER = "User %s just rubbed you in a private chat; respond to it.";

    private ChatPrompt() {
    }

    // ════ Context ═════════════════

    /**
     * 组装本次对话的提示词信息包。
     * <p>Builds the prompt packets of the conversation.</p>
     *
     * @param event         the triggering event
     * @param source        the trigger source
     * @param prompt        the per-chat prompt, nullable
     * @param defaultPrompt the global default prompt, nullable
     * @return the prompt packets
     */
    static List<ContextPacket> context(BotEvent event, AiSource source,
                                       String prompt, String defaultPrompt) {
        return List.of(
                new TextPacket(base(prompt, defaultPrompt)),
                new TextPacket(FORMAT_PROMPT),
                new TextPacket(trigger(event, source)));
    }

    // ════ Helpers ═════════════════

    /**
     * 选择提示词基底：会话自带提示词优先，否则使用全局配置。
     * <p>Selects the prompt base: the per-chat prompt wins, otherwise the global configuration.</p>
     *
     * @param prompt        the per-chat prompt, nullable
     * @param defaultPrompt the global default prompt, nullable
     * @return the prompt base
     */
    private static String base(String prompt, String defaultPrompt) {
        if (prompt != null && !prompt.isBlank()) return prompt;
        return defaultPrompt;
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
            case GroupMsgEvent e -> GROUP_TRIGGER.formatted(e.messageId(),
                    UserLabels.of(e.nickname(), e.userId()),
                    source == AiSource.GROUP_AUTO_CHAT ? GROUP_AUTO_NOTE : GROUP_MENTION_NOTE);
            case PrivateMsgEvent e -> PRIVATE_TRIGGER.formatted(e.messageId(),
                    UserLabels.of(e.nickname(), e.userId()));
            case GroupPokeEvent e -> GROUP_POKE_TRIGGER.formatted(e.userId());
            case PrivatePokeEvent e -> PRIVATE_POKE_TRIGGER.formatted(e.userId());
            default -> null;
        };
    }
}
