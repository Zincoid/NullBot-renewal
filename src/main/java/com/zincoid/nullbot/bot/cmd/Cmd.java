package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.*;

/**
 * 机器人指令：每种路由事件一个 run 槽位。
 * <p>A bot command: one run slot per routed event type.</p>
 */
public interface Cmd {

    /**
     * 在群聊消息事件上执行指令。
     * <p>Runs the command on a group message event.</p>
     *
     * @param ctx   the command context
     * @param event the triggering event
     * @param args  the parsed arguments
     */
    default void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        throw new CmdWarnException("Command does not support group message events");
    }

    /**
     * 在私聊消息事件上执行指令。
     * <p>Runs the command on a private message event.</p>
     *
     * @param ctx   the command context
     * @param event the triggering event
     * @param args  the parsed arguments
     */
    default void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        throw new CmdWarnException("Command does not support private message events");
    }

    /**
     * 在群聊戳一戳事件上执行指令。
     * <p>Runs the command on a group poke event.</p>
     *
     * @param ctx   the command context
     * @param event the triggering event
     * @param args  the parsed arguments
     */
    default void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        throw new CmdWarnException("Command does not support group poke events");
    }

    /**
     * 在私聊戳一戳事件上执行指令。
     * <p>Runs the command on a private poke event.</p>
     *
     * @param ctx   the command context
     * @param event the triggering event
     * @param args  the parsed arguments
     */
    default void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        throw new CmdWarnException("Command does not support private poke events");
    }

    /**
     * 在群聊撤回事件上执行指令。
     * <p>Runs the command on a group recall event.</p>
     *
     * @param ctx   the command context
     * @param event the triggering event
     * @param args  the parsed arguments
     */
    default void run(CmdContext ctx, GroupRecallEvent event, CmdArgs args) {
        throw new CmdWarnException("Command does not support group recall events");
    }

    /**
     * 在私聊撤回事件上执行指令。
     * <p>Runs the command on a private recall event.</p>
     *
     * @param ctx   the command context
     * @param event the triggering event
     * @param args  the parsed arguments
     */
    default void run(CmdContext ctx, PrivateRecallEvent event, CmdArgs args) {
        throw new CmdWarnException("Command does not support private recall events");
    }

    /**
     * 在入群事件上执行指令。
     * <p>Runs the command on a group join event.</p>
     *
     * @param ctx   the command context
     * @param event the triggering event
     * @param args  the parsed arguments
     */
    default void run(CmdContext ctx, JoinEvent event, CmdArgs args) {
        throw new CmdWarnException("Command does not support group join events");
    }

    /**
     * 在离群事件上执行指令。
     * <p>Runs the command on a group leave event.</p>
     *
     * @param ctx   the command context
     * @param event the triggering event
     * @param args  the parsed arguments
     */
    default void run(CmdContext ctx, LeaveEvent event, CmdArgs args) {
        throw new CmdWarnException("Command does not support group leave events");
    }

    /**
     * 展示给用户的帮助文本。
     * <p>Help text shown to users.</p>
     */
    default String getHelp() {
        return "No help available";
    }

    /**
     * 供 AI 文档使用的帮助文本。
     * <p>Help text exposed to AI documentation.</p>
     */
    default String getAiHelp() {
        return getHelp();
    }

    /**
     * 指令所需限权等级。
     * <p>Required access level of the command.</p>
     */
    default int getAccess() {
        return 0;
    }
}
