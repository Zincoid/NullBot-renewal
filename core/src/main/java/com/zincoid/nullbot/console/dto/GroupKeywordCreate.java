package com.zincoid.nullbot.console.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 群关键词规则创建请求。
 * <p>Request to create a group keyword rule.</p>
 *
 * @param keyword the keyword to match
 * @param cmd     the command name to dispatch
 * @param args    the command arguments, nullable
 */
public record GroupKeywordCreate(
        @NotBlank(message = "Keyword is required")
        @Size(max = 255, message = "Keyword must not exceed 255 characters")
        String keyword,

        @NotBlank(message = "Command is required")
        @Size(max = 100, message = "Command must not exceed 100 characters")
        String cmd,

        @Size(max = 50, message = "Too many arguments")
        List<@NotBlank(message = "Argument must not be blank")
                @Size(max = 255, message = "Argument must not exceed 255 characters") String> args
) {
}
