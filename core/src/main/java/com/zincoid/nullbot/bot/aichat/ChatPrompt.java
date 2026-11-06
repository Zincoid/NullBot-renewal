package com.zincoid.nullbot.bot.aichat;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.platform.persistence.enums.AiSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 聊天提示词：组装系统提示词与触发说明。
 * <p>Chat prompt: builds the system prompt and trigger note.</p>
 */
@Component
@RequiredArgsConstructor
public class ChatPrompt {

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
            - Call "send" as many times as needed for separate messages or media.
            - In normal conversation, use one send per chat bubble.
            - Use a line break only when the content genuinely needs it (for example code), and never output blank lines.
            - Set last=true on the send that completes your answer; no separate final reply is then sent.
            - If no send has last=true, give a final reply without repeating what you already sent.
            - The trigger note identifies the message to respond to and how the turn was triggered.
            - Use ids only as tool arguments; never write ids, history markers, or attachment notes in your reply.
            
            Style:
            - Keep it short and conversational, and match the user's language.
            - Never write documents, reports, headings, bullet lists, or tables.
            - Avoid markdown formatting unless the user asks for code.""";

    private final ConfigService configService;

    // ════ System ══════════════════

    /**
     * 组装本次对话的系统提示词。
     * <p>Builds the system prompt of the conversation.</p>
     *
     * @param event  the triggering event
     * @param source the trigger source
     * @param prompt the per-chat prompt, nullable
     * @return the system prompt
     */
    public String system(BotEvent event, AiSource source, String prompt) {
        return compose(base(prompt), trigger(event, source));
    }

    // ════ Helpers ═════════════════

    /**
     * 选择提示词基底：会话自带提示词优先，否则使用全局配置。
     * <p>Selects the prompt base: the per-chat prompt wins, otherwise the global configuration.</p>
     *
     * @param prompt the per-chat prompt, nullable
     * @return the prompt base
     */
    private String base(String prompt) {
        if (prompt != null && !prompt.isBlank()) return prompt;
        return configService.getString(Conf.AI_DEFAULT_PROMPT);
    }

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
            case GroupMsgEvent e -> "The message to respond to is id=%s from %s in a group chat. %s"
                    .formatted(e.messageId(), sender(e),
                            source == AiSource.GROUP_AUTO_CHAT
                                    ? "This is an automatic chat trigger; you were not mentioned."
                                    : "The user mentioned you.");
            case PrivateMsgEvent e -> "The message to respond to is id=%s from %s in a private chat."
                    .formatted(e.messageId(), sender(e));
            case GroupPokeEvent e -> "User %s just rubbed you in a group chat; respond to it."
                    .formatted(e.userId());
            case PrivatePokeEvent e -> "User %s just rubbed you in a private chat; respond to it."
                    .formatted(e.userId());
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
}
