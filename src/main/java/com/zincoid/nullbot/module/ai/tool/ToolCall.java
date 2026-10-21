package com.zincoid.nullbot.module.ai.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型发起的一次工具调用。
 * <p>One tool call issued by the model.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolCall {

    private String id;
    private String name;
    private String args;
}
