package com.zincoid.nullbot.module.system;

/**
 * 输入等待模式：PS 按用户单值、GS 按群单值、GM 按群多值。
 * <p>Input wait mode: PS per user single, GS per group single, GM per group multiple.</p>
 */
public enum InputMode {
    PS,  // Personal single value
    GS,  // Group single value
    GM   // Group multiple values
}
