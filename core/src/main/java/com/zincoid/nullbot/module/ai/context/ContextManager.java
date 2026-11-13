package com.zincoid.nullbot.module.ai.context;

import com.zincoid.nullbot.module.ai.message.AiMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 上下文管理器：把上下文信息包组织为模型消息，文本包拼接为系统提示词，图片包附在历史之后。
 * <p>Context manager: turns context packets into model messages, joining text packets into the system prompt and attaching image packets after the history.</p>
 */
@Component
public class ContextManager {

    // ════ Build ══════════════════

    /**
     * 组装本次对话的模型消息：系统提示词、历史消息与附带的图片。
     * <p>Builds the model messages of the conversation: system prompt, history, and attached images.</p>
     *
     * @param history the conversation history
     * @param context the context packets
     * @return the model messages, mutable for the tool loop
     */
    public List<AiMessage> build(List<AiMessage> history, List<ContextPacket> context) {
        List<AiMessage> messages = new ArrayList<>();
        AiMessage system = system(context);
        if (system != null) messages.add(system);
        messages.addAll(history);
        messages.addAll(images(context));
        return messages;
    }

    // ════ Helpers ═════════════════

    /**
     * 拼接文本信息包为系统消息，没有文本时返回 null。
     * <p>Joins text packets into the system message, returning null when there is no text.</p>
     *
     * @param packets the context packets
     * @return the system message, or null when there is no text
     */
    private static AiMessage system(List<ContextPacket> packets) {
        String text = packets.stream()
                .filter(ContextPacket.Text.class::isInstance)
                .map(ContextPacket.Text.class::cast)
                .map(ContextPacket.Text::text)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("\n\n"));
        return text.isBlank() ? null : AiMessage.system(text);
    }

    /**
     * 按描述分组，把图片信息包转换为用户消息。
     * <p>Groups image packets by description and converts them into user messages.</p>
     *
     * @param packets the context packets
     * @return the user messages carrying the images
     */
    private static List<AiMessage> images(List<ContextPacket> packets) {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (ContextPacket packet : packets) {
            if (!(packet instanceof ContextPacket.Image(String url, String description))) continue;
            if (url == null || url.isBlank()) continue;
            groups.computeIfAbsent(description, _ -> new ArrayList<>())
                    .add(url);
        }
        List<AiMessage> messages = new ArrayList<>();
        groups.forEach((description, urls) ->
                messages.add(AiMessage.user(null, description, urls)));
        return messages;
    }
}
