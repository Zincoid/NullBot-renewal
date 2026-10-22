package com.zincoid.nullbot.module.ai.tool;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册表：按名称索引所有工具 Bean。
 * <p>Tool registry: indexes all tool beans by name.</p>
 */
@Component
public class ToolRegistry {

    private final Map<String, Tool> toolMap = new LinkedHashMap<>();

    public ToolRegistry(List<Tool> tools) {
        tools.forEach(this::register);
    }

    /**
     * 注册工具。
     * <p>Registers a tool.</p>
     *
     * @param tool the tool
     */
    public void register(Tool tool) {
        toolMap.put(tool.definition().getName(), tool);
    }

    /**
     * 按名称查找工具。
     * <p>Looks up a tool by name.</p>
     *
     * @param name the tool name
     * @return the tool, or null when unknown
     */
    public Tool get(String name) {
        return toolMap.get(name);
    }

    /**
     * 返回全部工具定义。
     * <p>Returns all tool definitions.</p>
     *
     * @return the tool definitions
     */
    public List<ToolDefinition> getAll() {
        return toolMap.values().stream()
                .map(Tool::definition)
                .toList();
    }
}
