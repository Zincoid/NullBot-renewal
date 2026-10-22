package com.zincoid.nullbot.console.dto;

/**
 * 控制台配置更新请求。
 * <p>Console request to update a configuration entry.</p>
 *
 * @param value the new value
 */
public record ConfigUpdate(String value) {
}
