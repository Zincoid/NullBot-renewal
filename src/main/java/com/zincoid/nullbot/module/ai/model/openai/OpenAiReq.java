package com.zincoid.nullbot.module.ai.model.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.model.ModelReq;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
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

    private String model;
    private List<Map<String, Object>> messages;
    private List<Tool> tools;
    private boolean stream;
    private double temperature;
    @JsonProperty("reasoning_effort")
    private Effort effort;
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
                .messages(req.getMessages().stream().map(AiMessage::toMap).toList())
                .stream(false)
                .temperature(1.0)
                .effort(req.getEffort())
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
}
