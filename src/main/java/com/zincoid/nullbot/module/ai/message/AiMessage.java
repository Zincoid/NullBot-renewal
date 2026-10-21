package com.zincoid.nullbot.module.ai.message;

import com.zincoid.nullbot.module.ai.tool.ToolCall;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型对话消息：角色、内容、图片、工具调用与推理内容。
 * <p>Model chat message: role, content, images, tool calls, and reasoning.</p>
 */
@Getter
@ToString
public class AiMessage {

    private final AiRole role;
    private final String name;
    private final String content;
    private final List<ToolCall> tcs;
    private final String tcId;
    private final List<String> images;
    private String reasoning;

    private AiMessage(AiRole role, String name, String content, List<ToolCall> tcs, String tcId, List<String> images) {
        this.role = role;
        this.name = name;
        this.content = content;
        this.tcs = tcs;
        this.tcId = tcId;
        this.images = images;
    }

    /**
     * 附加推理内容。
     * <p>Attaches the reasoning content.</p>
     *
     * @param reasoning the reasoning content
     * @return this message
     */
    public AiMessage withReasoning(String reasoning) {
        this.reasoning = reasoning;
        return this;
    }

    /**
     * 转换为 OpenAI 兼容的消息 JSON。
     * <p>Converts to the OpenAI-compatible message JSON.</p>
     *
     * @return the message map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("role", role.getValue());
        if (name != null && !name.isEmpty())
            map.put("name", name);
        if (images != null && !images.isEmpty()) {
            List<Map<String, Object>> parts = new ArrayList<>();
            if (content != null && !content.isEmpty())
                parts.add(Map.of("type", "text", "text", content));
            for (String image : images) {
                Map<String, Object> url = new HashMap<>();
                url.put("url", image);
                parts.add(Map.of("type", "image_url", "image_url", url));
            }
            map.put("content", parts);
        } else if (content != null && !content.isEmpty()) {
            map.put("content", content);
        }
        if (reasoning != null && !reasoning.isEmpty())
            map.put("reasoning_content", reasoning);
        if (tcs != null && !tcs.isEmpty()) {
            List<Map<String, Object>> calls = new ArrayList<>();
            for (ToolCall tc : tcs) {
                Map<String, Object> function = new HashMap<>();
                function.put("name", tc.getName());
                function.put("arguments", tc.getArgs());
                Map<String, Object> call = new HashMap<>();
                call.put("id", tc.getId());
                call.put("type", "function");
                call.put("function", function);
                calls.add(call);
            }
            map.put("tool_calls", calls);
        }
        if (tcId != null)
            map.put("tool_call_id", tcId);
        return map;
    }

    public static AiMessage user(String content) {
        return user(null, content, null);
    }

    public static AiMessage user(String name, String content, List<String> images) {
        return new AiMessage(AiRole.USER, name, content, null, null, images != null ? images : List.of());
    }

    public static AiMessage assistant(String content) {
        return new AiMessage(AiRole.ASSISTANT, null, content, null, null, List.of());
    }

    public static AiMessage assistant(List<ToolCall> tcs) {
        return new AiMessage(AiRole.ASSISTANT, null, null, tcs, null, List.of());
    }

    public static AiMessage system(String content) {
        return new AiMessage(AiRole.SYSTEM, null, content, null, null, List.of());
    }

    public static AiMessage tool(String tcId, String content) {
        return new AiMessage(AiRole.TOOL, null, content, null, tcId, List.of());
    }
}
