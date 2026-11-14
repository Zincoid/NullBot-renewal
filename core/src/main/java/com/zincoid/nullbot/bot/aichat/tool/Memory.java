package com.zincoid.nullbot.bot.aichat.tool;

import com.zincoid.nullbot.module.ai.memory.SemanticMemory;
import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolHost;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.platform.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 记忆工具：让模型新增、删除与搜索当前会话的长期事实。
 * <p>Memory tool: lets the model add, delete, and search long-term facts of the current chat.</p>
 */
@Component
@RequiredArgsConstructor
public class Memory implements Tool {

    private static final int DEFAULT_LIMIT = 16;

    private final SemanticMemory semanticMemory;

    // ════ Tool ════════════════════

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("memory", """
                        Manages long-term notes of the current chat.
                        Use "add" only for durable facts or preferences the user explicitly stated.
                        Use "delete" to remove one note by id.
                        Use "search" to find notes by keyword; an empty keyword lists the latest notes.""")
                .addEnum("action", "operation", Set.of("add", "delete", "search"), true)
                .addString("content", "note content for add")
                .addInteger("id", "note id for delete")
                .addString("keyword", "keyword for search; empty lists the latest notes")
                .addInteger("limit", "maximum number of results")
                .build();
    }

    @Override
    public ToolRes run(ToolHost host, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        if (_args.action == null || _args.action.isBlank())
            return ToolRes.error("Missing action");
        String chatId = host.chatId();
        return switch (_args.action) {
            case "add" -> add(chatId, _args);
            case "delete" -> delete(chatId, _args);
            case "search" -> search(chatId, _args);
            default -> ToolRes.error("Unsupported action: " + _args.action);
        };
    }

    // ════ Actions ═════════════════

    private ToolRes add(String chatId, Args args) {
        if (args.content == null || args.content.isBlank())
            return ToolRes.error("Missing content");
        return ToolRes.success("Memory saved: id="
                + semanticMemory.add(chatId, args.content).getId());
    }

    private ToolRes delete(String chatId, Args args) {
        if (args.id == null)
            return ToolRes.error("Missing id");
        return semanticMemory.delete(chatId, args.id)
                ? ToolRes.success("Memory deleted: id=" + args.id)
                : ToolRes.error("Memory not found: id=" + args.id);
    }

    private ToolRes search(String chatId, Args args) {
        int limit = args.limit == null ? DEFAULT_LIMIT : args.limit;
        StringBuilder text = new StringBuilder();
        semanticMemory.search(chatId, args.keyword, limit).forEach(memory -> {
            if (!text.isEmpty()) text.append('\n');
            text.append("id=").append(memory.getId())
                    .append(": ").append(memory.getContent());
        });
        return ToolRes.success(text.isEmpty()
                ? "No memories found" : text.toString());
    }

    private record Args(String action, String content, Long id, String keyword, Integer limit) {
    }
}
