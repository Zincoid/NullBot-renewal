package com.zincoid.nullbot.module.ai.message;

import lombok.Getter;

/**
 * 模型消息角色。
 * <p>Model message role.</p>
 */
@Getter
public enum AiRole {

    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    TOOL("tool");

    private final String value;

    AiRole(String value) {
        this.value = value;
    }
}
