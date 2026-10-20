package com.zincoid.nullbot.bot.cmd;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a {@link Cmd} under the given command names.
 * <p>把 {@link Cmd} 注册到给定的指令名下。</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CmdMapping {

    /**
     * Command names to register.
     * <p>要注册的指令名列表。</p>
     */
    String[] value();
}
