package com.zincoid.nullbot.module.ai.tool;

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

    private final Map<String, Tool> toolMap = new ConcurrentHashMap<>();

    public ToolRegistry(List<Tool> tools) {
        if (!register(tools))
            throw new IllegalStateException("Built-in tool registration failed");
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
        return name == null ? null : toolMap.get(name);
    }

    /**
     * 返回全部工具定义，按名称排序。
     * <p>Returns all tool definitions, sorted by name.</p>
     *
     * @return the tool definitions
     */
    public List<ToolDefinition> getAll() {
        return List.copyOf(toolMap.values()).stream()
                .map(Tool::definition)
                .sorted(Comparator.comparing(ToolDefinition::getName))
                .toList();
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
        Map<String, Tool> pending = new LinkedHashMap<>();
        for (Tool tool : tools) {
            if (tool == null) return false;
            ToolDefinition definition = tool.definition();
            if (definition == null) return false;
            String name = definition.getName();
            if (name == null || !NAME.matcher(name).matches()) {
                log.warn("Invalid tool name: {} ({})", name, tool.getClass().getSimpleName());
                return false;
            }
            Tool registered = toolMap.get(name);
            Tool queued = pending.get(name);
            if ((registered != null && registered != tool) || (queued != null && queued != tool)) {
                log.warn("Tool name conflict: {} ({})", name, tool.getClass().getSimpleName());
                return false;
            }
            pending.put(name, tool);
        }
        toolMap.putAll(pending);
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
        toolMap.entrySet().removeIf(entry -> targets.contains(entry.getValue()));
    }
}
