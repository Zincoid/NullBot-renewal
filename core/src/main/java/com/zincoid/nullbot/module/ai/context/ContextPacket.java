package com.zincoid.nullbot.module.ai.context;

/**
 * 上下文信息包：送入模型的上下文基本单元，支持文本与图片。
 * <p>Context packet: the basic unit of context fed to the model, supporting text and images.</p>
 */
public sealed interface ContextPacket permits
        TextPacket,
        ImagePacket {
}
