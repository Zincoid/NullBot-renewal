package com.zincoid.nullbot.bot.cmd.input;

/**
 * 输入等待模式：PS 按用户单值、GS 按群单值、GM 按群多值。
 * <p>Input wait mode: PS per user single, GS per group single, GM per group multiple.</p>
 */
public enum InputMode {

    /**
     * 个人单值模式
     * <p>Personal single value</p>
     */
    PS,

    /**
     * 群组单值模式
     * <p>Group single values</p>
     */
    GS,

    /**
     * 群组多值模式
     * <p>Group multiple values</p>
     */
    GM
}
