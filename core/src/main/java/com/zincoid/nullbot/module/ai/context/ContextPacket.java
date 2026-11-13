package com.zincoid.nullbot.module.ai.context;

/**
 * 上下文信息包：送入模型的上下文基本单元，支持文本与图片。
 * <p>Context packet: the basic unit of context fed to the model, supporting text and images.</p>
 */
public sealed interface ContextPacket permits ContextPacket.Text, ContextPacket.Image {

    /**
     * 文本信息包：按顺序拼接为系统提示词。
     * <p>Text packet: joined in order into the system prompt.</p>
     *
     * @param text the text content, nullable
     */
    record Text(String text) implements ContextPacket {
    }

    /**
     * 图片信息包：模型可读取的 data URI 或网络地址，随用户消息附带。
     * <p>Image packet: a model-readable data URI or network URL attached with a user message.</p>
     *
     * @param url         the image url, nullable
     * @param description the label shown to the model, nullable
     */
    record Image(String url, String description) implements ContextPacket {
    }
}
