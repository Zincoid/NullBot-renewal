package com.zincoid.nullbot.module.ai.tool;

import com.zincoid.nullbot.module.ai.AiException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 工具注册表：按名称索引内置与插件工具。
 * <p>Tool registry: indexes built-in and plugin tools by name.</p>
 */
@Slf4j
@Component
public class ToolRegistry {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    @Getter
    private volatile List<ToolDefinition> definitions = List.of();
    private final Map<String, Entry> toolMap = new ConcurrentHashMap<>();

    public ToolRegistry(List<Tool> tools) {
        if (!register(tools))
            throw new AiException("Built-in tool registration failed");
    }

    // ════ Get ═════════════════════

    /**
     * 按名称查找工具。
     * <p>Looks up a tool by name.</p>
     *
     * @param name the tool name
     * @return the tool, or null when unknown
     */
    public Tool get(String name) {
        Entry entry = name == null ? null : toolMap.get(name);
        return entry == null ? null : entry.tool();
    }

    // ════ Register ════════════════

    /**
     * 原子注册一组工具，任一名称非法或冲突时整组不注册。
     * <p>Registers a group of tools atomically; the whole group is rejected when any name is invalid or conflicts.</p>
     *
     * @param tools the tools to register
     * @return true when all tools were registered
     */
    public synchronized boolean register(Collection<Tool> tools) {
        if (tools == null || tools.isEmpty()) return true;
        Map<String, Entry> pending = new LinkedHashMap<>();
        for (Tool tool : tools) {
            if (tool == null) return false;
            ToolDefinition definition = tool.definition();
            if (definition == null) return false;
            String name = definition.name();
            if (name == null || !NAME.matcher(name).matches()) {
                log.warn("Invalid tool name: {} ({})", name, tool.getClass().getSimpleName());
                return false;
            }
            Entry registered = toolMap.get(name);
            Entry queued = pending.get(name);
            if ((registered != null && registered.tool() != tool)
                    || (queued != null && queued.tool() != tool)) {
                log.warn("Tool name conflict: {} ({})", name, tool.getClass().getSimpleName());
                return false;
            }
            pending.put(name, new Entry(tool, definition));
        }
        toolMap.putAll(pending);
        refresh();
        return true;
    }

    /**
     * 按工具实例反注册其全部名称。
     * <p>Unregisters every name mapped to the given tool instances.</p>
     *
     * @param tools the tool instances to remove
     */
    public synchronized void unregister(Collection<Tool> tools) {
        if (tools == null || tools.isEmpty()) return;
        Set<Tool> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        targets.addAll(tools);
        if (toolMap.entrySet().removeIf(entry -> targets.contains(entry.getValue().tool())))
            refresh();
    }

    // ════ Helpers ═════════════════

    private void refresh() {
        definitions = toolMap.values().stream()
                .map(Entry::definition)
                .sorted(Comparator.comparing(ToolDefinition::name))
                .toList();
    }

    private record Entry(Tool tool, ToolDefinition definition) {
    }
}
