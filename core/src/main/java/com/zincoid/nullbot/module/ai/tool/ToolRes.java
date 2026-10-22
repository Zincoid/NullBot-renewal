package com.zincoid.nullbot.module.ai.tool;

import java.util.List;

/**
 * 工具执行结果：文本、图片与错误标记。
 * <p>Tool result: text, images, and error flag.</p>
 *
 * @param text   the result text
 * @param images the images to feed back to the model
 * @param error  whether the result is an error
 */
public record ToolRes(String text, List<String> images, boolean error) {

    public static ToolRes of(String text) {
        return new ToolRes(text, List.of(), false);
    }

    public static ToolRes of(String text, List<String> images) {
        return new ToolRes(text, images != null ? images : List.of(), false);
    }

    public static ToolRes error(String text) {
        return new ToolRes(text, List.of(), true);
    }
}
