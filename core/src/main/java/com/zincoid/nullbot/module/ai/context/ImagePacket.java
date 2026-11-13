package com.zincoid.nullbot.module.ai.context;

/**
 * 图片信息包：模型可读取的 data URI 或网络地址，随用户消息附带。
 * <p>Image packet: a model-readable data URI or network URL attached with a user message.</p>
 *
 * @param url         the image url, nullable
 * @param description the label shown to the model, nullable
 */
public record ImagePacket(String url, String description) implements ContextPacket {
}
