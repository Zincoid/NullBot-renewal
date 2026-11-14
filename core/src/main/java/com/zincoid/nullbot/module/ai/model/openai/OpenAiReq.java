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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的 Chat Completions 请求体。
 * <p>OpenAI-compatible Chat Completions request body.</p>
 *
 * @param model       the model name
 * @param messages    the OpenAI message maps
 * @param tools       the tool definitions, nullable
 * @param stream      whether to stream the response
 * @param temperature the sampling temperature
 * @param effort      the reasoning effort, nullable
 * @param maxTokens   the output token limit
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiReq(String model, List<Map<String, Object>> messages,
                        List<Tool> tools, boolean stream, double temperature,
                        @JsonProperty("reasoning_effort") String effort,
                        @JsonProperty("max_tokens") int maxTokens) {

    private static final String REASONING_FALLBACK = """
            (reasoning content unavailable)""";

    @Builder
    public record Tool(String type, Function function) {}

    @Builder
    public record Function(String name, String description,
                           @JsonRawValue String parameters) {}

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
                .messages(req.messages().stream().map(OpenAiReq::toMap).toList())
                .stream(false)
                .temperature(1.0)
                .effort(req.effort() == null ? null : req.effort().getLabel())
                .maxTokens(req.maxTokens());

        List<ToolDefinition> toolDefinitions = req.tools();
        if (!toolDefinitions.isEmpty()) {
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

    private static Map<String, Object> system(SystemMsg m) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", "system");
        map.put("content", m.content());
        return map;
    }

    private static Map<String, Object> tool(ToolMsg m) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", "tool");
        map.put("content", m.content());
        map.put("tool_call_id", m.tcId());
        return map;
    }

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
