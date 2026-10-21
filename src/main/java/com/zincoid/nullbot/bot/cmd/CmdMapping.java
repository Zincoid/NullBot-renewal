package com.zincoid.nullbot.bot.cmd;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 把 {@link Cmd} 注册到给定的指令名下。
 * <p>Registers a {@link Cmd} under the given command names.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CmdMapping {

    /**
     * 要注册的指令名列表。
     * <p>Command names to register.</p>
     */
    String[] value();
}
