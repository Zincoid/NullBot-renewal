package com.zincoid.nullbot.module.ai.agent;

import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.module.ai.context.ContextManager;
import com.zincoid.nullbot.module.ai.context.ContextPacket;
import com.zincoid.nullbot.module.ai.memory.Memory;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.model.Model;
import com.zincoid.nullbot.module.ai.model.ModelReq;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import com.zincoid.nullbot.module.ai.model.TokenUsage;
import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolCall;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolRegistry;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天代理：带系统提示词、会话记忆与多轮工具调用的模型对话。
 * <p>Chat agent: model conversation with a system prompt, chat memory, and multi-round tool calls.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatAgent {

    private final Memory memory;
    private final Model model;
    private final ToolRegistry toolRegistry;
    private final ContextManager contextManager;

    // ════ Run ═════════════════════

    /**
     * 发起一次对话。
     * <p>Runs one conversation.</p>
     *
     * @param chatId  the chat identifier whose history is loaded
     * @param options the agent options
     * @param ctx     the tool context
     * @param context the context packets of the conversation
     * @return the final model response
     */
    public ModelRes run(String chatId, AgentOptions options, ToolContext ctx,
                        List<ContextPacket> context) {
        int maxTcs = options.maxToolRounds();
        List<AiMessage> history = memory.get(chatId);
        List<AiMessage> work = contextManager.build(history, context);
        if (maxTcs <= 0)
            return conclude(work, options, null);
        TokenUsage usage = null;
        for (int i = 0; i < maxTcs; i++) {
            ModelRes res = model.invoke(ModelReq.of(
                    work,
                    toolRegistry.getAll(),
                    options.effort(),
                    options.maxTokens()
            ));
            usage = TokenUsage.plus(usage, res.getUsage());
            if (!res.hasTcs()) return requireContent(res).withUsage(usage);
            log.info("Tool round {}: {} call(s)", i + 1, res.getTcs().size());
            work.add(AiMessage.assistant(res.getTcs())
                    .withReasoning(res.getReasoning()));
            List<AiMessage> images = new ArrayList<>();
            for (ToolCall tc : res.getTcs()) {
                ToolRes tr = runTool(tc, ctx);
                String content = tr.error()
                        ? "Error: " + tr.text() : tr.text();
                work.add(AiMessage.tool(tc.getId(), content));
                if (tr.images().isEmpty()) continue;
                images.add(AiMessage.user(
                        null,
                        "Image(s) from tool \"%s\"".formatted(tc.getName()),
                        tr.images()
                ));
            }
            if (ctx.hasFinalSent()) {
                log.info("Tool loop ended early: final message sent");
                return ModelRes.stopped(usage);
            }
            work.addAll(images);
        }
        log.warn("Tool call limit reached: {}", maxTcs);
        work.add(AiMessage.user("""
                The maximum number of tool call rounds has been reached.
                Please provide the final answer based on the available information.
                Do not call any more tools!!!"""
        ));
        return conclude(work, options, usage);
    }

    // ════ Helpers ═════════════════

    private ModelRes conclude(List<AiMessage> work, AgentOptions options,
                              TokenUsage usage) {
        ModelRes res = requireContent(model.invoke(ModelReq.of(
                work, options.effort(), options.maxTokens())));
        return res.withUsage(TokenUsage.plus(usage, res.getUsage()));
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
            log.info("Tool success: {} -> {}", tc.getName(), summary(res));
            return res;
        } catch (Exception e) {
            log.warn("Tool error: {}", e.getMessage());
            return ToolRes.error(e.getMessage());
        }
    }

    private static String summary(ToolRes res) {
        String text = res.text() == null ? "" : res.text();
        if (text.length() > 200) text = "%s...".formatted(text.substring(0, 200));
        return res.images().isEmpty()
                ? text : "%s (+%d image(s))".formatted(text, res.images().size());
    }
}
