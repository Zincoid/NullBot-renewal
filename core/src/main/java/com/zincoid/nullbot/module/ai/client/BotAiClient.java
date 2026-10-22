package com.zincoid.nullbot.module.ai.client;

import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.model.Model;
import com.zincoid.nullbot.module.ai.model.ModelReq;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolCall;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolRegistry;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 机器人 AI 客户端：带系统提示词与多轮工具调用的模型对话。
 * <p>Bot AI client: model conversation with a system prompt and multi-round tool calls.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotAiClient {

    private final Model model;
    private final ToolRegistry toolRegistry;
    private final ConfigService configService;

    // ════ Invoke ══════════════════

    /**
     * 发起一次对话。
     * <p>Runs one conversation.</p>
     *
     * @param history the conversation history
     * @param prompt  the system prompt, may be blank
     * @param effort  the reasoning effort
     * @param ctx     the tool context
     * @return the final model response
     */
    public ModelRes invoke(List<AiMessage> history, String prompt, Effort effort, ToolContext ctx) {
        int maxTcs = maxToolCalls();
        int maxTokens = maxTokens();
        List<AiMessage> work = new ArrayList<>();
        if (prompt != null && !prompt.isBlank())
            work.add(AiMessage.system(prompt));
        work.addAll(history);
        if (maxTcs <= 0)
            return replyFinal(work, effort, maxTokens);
        for (int i = 0; i < maxTcs; i++) {
            ModelRes res = model.invoke(ModelReq.of(
                    work,
                    toolRegistry.getAll(),
                    effort,
                    maxTokens
            ));
            if (!res.hasTcs()) return requireContent(res);
            log.info("Tool round {}: {} call(s)", i + 1, res.getTcs().size());
            work.add(AiMessage.assistant(res.getTcs())
                    .withReasoning(res.getReasoning()));
            List<AiMessage> images = new ArrayList<>();
            for (ToolCall tc : res.getTcs()) {
                ToolRes tr = runTool(tc, ctx);
                String content = tr.error() ? "Error: " + tr.text() : tr.text();
                work.add(AiMessage.tool(tc.getId(), content));
                if (tr.images().isEmpty()) continue;
                images.add(AiMessage.user(
                        null,
                        "Image(s) from tool \"%s\"".formatted(tc.getName()),
                        tr.images()
                ));
            }
            work.addAll(images);
        }
        log.warn("Tool call limit reached: {}", maxTcs);
        work.add(AiMessage.user("""
                The maximum number of tool call rounds has been reached.
                Please provide the final answer based on the available information.
                Do not call any more tools!!!"""
        ));
        return replyFinal(work, effort, maxTokens);
    }

    // ════ Helpers ═════════════════

    private ModelRes replyFinal(List<AiMessage> work, Effort effort, int maxTokens) {
        return requireContent(model.invoke(ModelReq.of(work, effort, maxTokens)));
    }

    private static ModelRes requireContent(ModelRes res) {
        if (res.getContent() == null || res.getContent().isBlank())
            throw new AiException("Empty AI response");
        return res;
    }

    private ToolRes runTool(ToolCall tc, ToolContext ctx) {
        Tool tool = toolRegistry.get(tc.getName());
        if (tool == null)
            return ToolRes.error("Tool not found");
        try {
            ToolRes res = tool.run(ctx, tc.getArgs());
            log.info("Tool success: {} -> {}", tc.getName(), res);
            return res;
        } catch (Exception e) {
            log.warn("Tool error: {}", e.getMessage());
            return ToolRes.error(e.getMessage());
        }
    }

    private int maxTokens() {
        int value = configService.getInt(Conf.AI_MAX_TOKENS);
        if (value <= 0) throw new AiException("Malformed value: " + value);
        return value;
    }

    private int maxToolCalls() {
        int value = configService.getInt(Conf.AI_MAX_TOOL_CALLS);
        if (value < 0) throw new AiException("Malformed value: " + value);
        return value;
    }
}
