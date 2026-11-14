package com.zincoid.nullbot.module.ai.agent;

import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.module.ai.context.ContextManager;
import com.zincoid.nullbot.module.ai.context.ContextPacket;
import com.zincoid.nullbot.module.ai.memory.Memory;
import com.zincoid.nullbot.module.ai.message.AiMsg;
import com.zincoid.nullbot.module.ai.model.Model;
import com.zincoid.nullbot.module.ai.model.ModelReq;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import com.zincoid.nullbot.module.ai.model.ModelUsage;
import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolCall;
import com.zincoid.nullbot.module.ai.tool.ToolHost;
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

    private static final String TOOL_LIMIT_PROMPT = """
            The maximum number of tool call rounds has been reached.
            Please provide the final answer based on the available information.
            Do not call any more tools!!!""";

    private final Memory memory;
    private final Model model;
    private final ToolRegistry toolRegistry;
    private final ContextManager contextManager;

    // ════ Chat ════════════════════

    /**
     * 创建一次对话的链式调用。
     * <p>Creates a fluent call for one conversation.</p>
     *
     * @param chatId the chat identifier whose history is loaded
     * @return the fluent call
     */
    public Call chat(String chatId) {
        return new Call(chatId);
    }

    /**
     * 单次对话的链式调用：绑定参数后通过 {@link #call()} 执行。
     * <p>Fluent call of one conversation: binds the arguments and runs through {@link #call()}.</p>
     */
    public final class Call {

        private final String chatId;
        private final List<ContextPacket> context = new ArrayList<>();
        private ChatOptions options;
        private ToolHost host;

        private Call(String chatId) {
            this.chatId = chatId;
        }

        public Call options(ChatOptions options) {
            this.options = options;
            return this;
        }

        public Call context(List<ContextPacket> packets) {
            if (packets != null) context.addAll(packets);
            return this;
        }

        public Call tool(ToolHost host) {
            this.host = host;
            return this;
        }

        public ChatRes call() {
            if (options == null) throw new AiException("Missing options");
            if (host == null) throw new AiException("Missing tool host");
            return ChatAgent.this.run(chatId, options, host, List.copyOf(context));
        }
    }

    // ════ Run ═════════════════════

    /**
     * 执行一次对话，由链式调用进入。
     * <p>Runs one conversation, entered from the fluent call.</p>
     *
     * @param chatId  the chat identifier whose history is loaded
     * @param options the chat options
     * @param host    the tool host
     * @param context the context packets of the conversation
     * @return the conversation result
     */
    private ChatRes run(String chatId, ChatOptions options, ToolHost host,
                        List<ContextPacket> context) {
        int maxTcs = options.maxToolCalls();
        List<AiMsg> history = memory.get(chatId);
        List<AiMsg> work = contextManager.build(history, context);
        if (maxTcs <= 0)
            return ChatRes.unsent(conclude(work, options, null));
        ModelUsage usage = null;
        for (int i = 0; i < maxTcs; i++) {
            ModelRes res = model.invoke(ModelReq.of(
                    work,
                    toolRegistry.getDefinitions(),
                    options.effort(),
                    options.maxTokens()
            ));
            usage = ModelUsage.plus(usage, res.usage());
            if (res.tcs().isEmpty())
                return ChatRes.unsent(res.withUsage(usage));
            log.info("Tool round {}: {} call(s)", i + 1, res.tcs().size());
            work.add(AiMsg.assistant(res.tcs())
                    .withReasoning(res.reasoning()));
            List<AiMsg> images = new ArrayList<>();
            for (ToolCall tc : res.tcs()) {
                ToolRes tr = runTool(tc, host);
                String content = tr.error()
                        ? "Error: " + tr.text() : tr.text();
                work.add(AiMsg.tool(tc.id(), content));
                if (tr.images().isEmpty()) continue;
                images.add(AiMsg.user(
                        null,
                        "Image(s) from tool \"%s\"".formatted(tc.name()),
                        tr.images()
                ));
            }
            if (host.finalSent()) {
                log.info("Tool loop ended early: final message sent");
                return ChatRes.sent(usage);
            }
            work.addAll(images);
        }
        log.warn("Tool call limit reached: {}", maxTcs);
        work.add(AiMsg.user(TOOL_LIMIT_PROMPT));
        return ChatRes.unsent(conclude(work, options, usage));
    }

    // ════ Helpers ═════════════════

    private ModelRes conclude(List<AiMsg> work, ChatOptions options,
                              ModelUsage usage) {
        ModelRes res = model.invoke(ModelReq.of(
                work, options.effort(), options.maxTokens()));
        return res.withUsage(ModelUsage.plus(usage, res.usage()));
    }

    private ToolRes runTool(ToolCall tc, ToolHost host) {
        Tool tool = toolRegistry.get(tc.name());
        if (tool == null)
            return ToolRes.error("Tool not found");
        try {
            ToolRes res = tool.run(host, tc.args());
            log.info("Tool success: {} -> {}", tc.name(), summary(res));
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
