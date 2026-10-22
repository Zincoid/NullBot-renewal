package com.zincoid.nullbot.console.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 控制台配置更新请求。
 * <p>Console request to update a configuration entry.</p>
 *
 * @param value the new value
 */
public record ConfigUpdate(@NotNull(message = "Value is required") String value) {
}
