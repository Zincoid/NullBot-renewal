package com.zincoid.nullbot.module.ai.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.zincoid.nullbot.module.ai.message.AiMsg;
import com.zincoid.nullbot.module.ai.message.AssistantMsg;
import com.zincoid.nullbot.module.ai.message.SystemMsg;
import com.zincoid.nullbot.module.ai.message.ToolMsg;
import com.zincoid.nullbot.module.ai.message.UserMsg;
import com.zincoid.nullbot.module.ai.model.ModelReq;
import com.zincoid.nullbot.module.ai.tool.ToolCall;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的 Chat Completions 请求体。
 * <p>OpenAI-compatible Chat Completions request body.</p>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class OpenAiReq {

    /**
     * 思考模型要求助手消息回传推理内容，缺失时使用的占位文本。
     * <p>Placeholder used when an assistant message has no reasoning to replay to a thinking model.</p>
     */
    private static final String REASONING_FALLBACK = "(reasoning content unavailable)";

    private String model;
    private List<Map<String, Object>> messages;
    private List<Tool> tools;
    private boolean stream;
    private double temperature;
    @JsonProperty("reasoning_effort")
    private String effort;
    @JsonProperty("max_tokens")
    private int maxTokens;

    @Data
    @Builder
    public static final class Tool {
        private String type;
        private Function function;
    }

    @Data
    @Builder
    public static final class Function {
        private String name;
        private String description;
        @JsonRawValue
        private String parameters;
    }

    /**
     * 由模型请求构造 API 请求体。
     * <p>Builds the API request body from a model request.</p>
     *
     * @param req       the model request
     * @param modelName the model name
     * @return the API request body
     */
    public static OpenAiReq from(ModelReq req, String modelName) {
        OpenAiReqBuilder builder = OpenAiReq.builder()
                .model(modelName)
                .messages(req.getMessages().stream().map(OpenAiReq::toMap).toList())
                .stream(false)
                .temperature(1.0)
                .effort(req.getEffort() == null ? null : req.getEffort().getLabel())
                .maxTokens(req.getMaxTokens());

        List<ToolDefinition> toolDefinitions = req.getTools();
        if (toolDefinitions != null && !toolDefinitions.isEmpty()) {
            List<Tool> tools = new ArrayList<>();
            for (ToolDefinition td : toolDefinitions) {
                tools.add(Tool.builder()
                        .type("function")
                        .function(Function.builder()
                                .name(td.getName())
                                .description(td.getDescription())
                                .parameters(td.getParameters().toString())
                                .build())
                        .build());
            }
            builder.tools(tools);
        }
        return builder.build();
    }

    // ════ Messages ═══════════════

    /**
     * 将领域消息转换为 OpenAI 消息 JSON。
     * <p>Converts a domain message to the OpenAI message JSON.</p>
     *
     * @param message the domain message
     * @return the message map
     */
    private static Map<String, Object> toMap(AiMsg message) {
        return switch (message) {
            case SystemMsg m -> system(m);
            case ToolMsg m -> tool(m);
            case UserMsg m -> user(m);
            case AssistantMsg m -> assistant(m);
        };
    }

    /**
     * 转换系统消息。
     * <p>Converts a system message.</p>
     *
     * @param m the system message
     * @return the OpenAI message map
     */
    private static Map<String, Object> system(SystemMsg m) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", "system");
        map.put("content", m.content());
        return map;
    }

    /**
     * 转换工具结果消息。
     * <p>Converts a tool result message.</p>
     *
     * @param m the tool message
     * @return the OpenAI message map
     */
    private static Map<String, Object> tool(ToolMsg m) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", "tool");
        map.put("content", m.content());
        map.put("tool_call_id", m.tcId());
        return map;
    }

    /**
     * 转换用户消息。
     * <p>Converts a user message.</p>
     *
     * @param m the user message
     * @return the OpenAI message map
     */
    private static Map<String, Object> user(UserMsg m) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", "user");
        String name = m.name();
        if (name != null)
            map.put("name", name);
        List<String> images = m.images();
        if (images.isEmpty()) {
            map.put("content", m.content());
            return map;
        }
        List<Map<String, Object>> parts = new ArrayList<>();
        String content = m.content();
        if (content != null)
            parts.add(Map.of(
                    "type", "text",
                    "text", content));
        for (String image : images) {
            Map<String, Object> url = new HashMap<>();
            url.put("url", image);
            parts.add(Map.of(
                    "type", "image_url",
                    "image_url", url));
        }
        map.put("content", parts);
        return map;
    }

    /**
     * 转换助手消息。
     * <p>Converts an assistant message.</p>
     *
     * @param m the assistant message
     * @return the OpenAI message map
     */
    private static Map<String, Object> assistant(AssistantMsg m) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", "assistant");
        String content = m.content();
        if (content != null)
            map.put("content", content);
        String reasoning = m.reasoning() == null || m.reasoning().isBlank()
                ? REASONING_FALLBACK : m.reasoning();
        map.put("reasoning_content", reasoning);
        List<ToolCall> tcs = m.tcs();
        if (!tcs.isEmpty())
            map.put("tool_calls", tcs.stream()
                    .map(tc -> Map.of(
                            "id", tc.getId(),
                            "type", "function",
                            "function", Map.of(
                                    "name", tc.getName(),
                                    "arguments", tc.getArgs())))
                    .toList());
        return map;
    }
}
