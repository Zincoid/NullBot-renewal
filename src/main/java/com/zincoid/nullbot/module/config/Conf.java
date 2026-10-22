package com.zincoid.nullbot.module.config;

import lombok.Getter;

/**
 * 配置键字典：数据库值优先，条目缺失时回退默认值。
 * <p>Configuration key dictionary: the database value wins; the default value is the fallback when the entry is missing.</p>
 */
@Getter
public enum Conf {

    CMD_PREFIX("cmd_prefix", "#", "Command prefix"),
    DEFAULT_LANGUAGE("default_language", "zh", "Default reply language (zh/en)"),
    MESSAGE_HISTORY_MAX_LENGTH("message_history_max_length", "64", "Chat history window length"),
    AI_PROVIDER_API_URL("ai_provider_api_url", "", "OpenAI-compatible API base URL"),
    AI_PROVIDER_API_KEY("ai_provider_api_key", "", "OpenAI-compatible API key", true),
    AI_PROVIDER_MODEL("ai_provider_model", "", "Model name"),
    AI_MAX_TOKENS("ai_max_tokens", "1024", "Max output tokens per request"),
    AI_MAX_TOOL_CALLS("ai_max_tool_calls", "5", "Max tool call rounds"),
    AI_DEFAULT_PROMPT("ai_default_prompt", "You are a helpful assistant.", "Default chat system prompt");

    private final String key;
    private final String defaultValue;
    private final String description;
    private final boolean secret;

    Conf(String key, String defaultValue, String description) {
        this(key, defaultValue, description, false);
    }

    Conf(String key, String defaultValue, String description, boolean secret) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
        this.secret = secret;
    }
}
