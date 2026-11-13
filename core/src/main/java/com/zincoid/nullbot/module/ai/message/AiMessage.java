package com.zincoid.nullbot.module.ai.message;

import com.zincoid.nullbot.module.ai.tool.ToolCall;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * 模型对话消息：角色、内容、图片、工具调用与推理内容。
 * <p>Model chat message: role, content, images, tool calls, and reasoning.</p>
 */
@Getter
@ToString
public final class AiMessage {

    private final AiRole role;
    private final String name;
    private final String content;
    private final List<ToolCall> tcs;
    private final String tcId;
    private final List<String> images;
    private final String reasoning;

    private AiMessage(AiRole role, String name, String content, List<ToolCall> tcs,
                      String tcId, List<String> images, String reasoning) {
        this.role = role;
        this.name = name;
        this.content = content;
        this.tcs = tcs;
        this.tcId = tcId;
        this.images = images;
        this.reasoning = reasoning;
    }

    public static AiMessage system(String content) {
        return new AiMessage(AiRole.SYSTEM, null, content, null, null, List.of(), null);
    }

    public static AiMessage tool(String tcId, String content) {
        return new AiMessage(AiRole.TOOL, null, content, null, tcId, List.of(), null);
    }

    public static AiMessage user(String content) {
        return user(null, content, null);
    }

    public static AiMessage user(String name, String content, List<String> images) {
        return new AiMessage(AiRole.USER, name, content, null, null, images != null ? images : List.of(), null);
    }

    public static AiMessage assistant(String content) {
        return new AiMessage(AiRole.ASSISTANT, null, content, null, null, List.of(), null);
    }

    public static AiMessage assistant(List<ToolCall> tcs) {
        return new AiMessage(AiRole.ASSISTANT, null, null, tcs, null, List.of(), null);
    }

    public AiMessage withReasoning(String reasoning) {
        return new AiMessage(role, name, content, tcs, tcId, images, reasoning);
    }
}
