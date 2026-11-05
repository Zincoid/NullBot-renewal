package com.zincoid.nullbot.module.ai.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.message.AiRole;
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
public class OpenAiReq {

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
    public static class Tool {
        private String type;
        private Function function;
    }

    @Data
    @Builder
    public static class Function {
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

    /**
     * 将领域消息转换为 OpenAI 消息 JSON。
     * <p>Converts a domain message to the OpenAI message JSON.</p>
     *
     * @param message the domain message
     * @return the message map
     */
    private static Map<String, Object> toMap(AiMessage message) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", message.getRole().getValue());
        if (message.getName() != null && !message.getName().isEmpty())
            map.put("name", message.getName());
        List<String> images = message.getImages();
        if (images != null && !images.isEmpty()) {
            List<Map<String, Object>> parts = new ArrayList<>();
            if (message.getContent() != null && !message.getContent().isEmpty())
                parts.add(Map.of("type", "text", "text", message.getContent()));
            for (String image : images) {
                Map<String, Object> url = new HashMap<>();
                url.put("url", image);
                parts.add(Map.of("type", "image_url", "image_url", url));
            }
            map.put("content", parts);
        } else if (message.getContent() != null && !message.getContent().isEmpty()) {
            map.put("content", message.getContent());
        }
        if (message.getRole() == AiRole.ASSISTANT)
            map.put("reasoning_content", message.getReasoning() == null || message.getReasoning().isBlank()
                    ? REASONING_FALLBACK : message.getReasoning());
        List<ToolCall> tcs = message.getTcs();
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
        if (message.getTcId() != null)
            map.put("tool_call_id", message.getTcId());
        return map;
    }
}
