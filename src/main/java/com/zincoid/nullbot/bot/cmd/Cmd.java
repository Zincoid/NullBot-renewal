package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.*;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;

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
        throw new CmdWarnException(ctx.render(Text.CMD_UNSUPPORTED_GROUP_MSG));
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
        throw new CmdWarnException(ctx.render(Text.CMD_UNSUPPORTED_PRIVATE_MSG));
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
        throw new CmdWarnException(ctx.render(Text.CMD_UNSUPPORTED_GROUP_POKE));
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
        throw new CmdWarnException(ctx.render(Text.CMD_UNSUPPORTED_PRIVATE_POKE));
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
        throw new CmdWarnException(ctx.render(Text.CMD_UNSUPPORTED_GROUP_RECALL));
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
        throw new CmdWarnException(ctx.render(Text.CMD_UNSUPPORTED_PRIVATE_RECALL));
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
        throw new CmdWarnException(ctx.render(Text.CMD_UNSUPPORTED_JOIN));
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
        throw new CmdWarnException(ctx.render(Text.CMD_UNSUPPORTED_LEAVE));
    }

    /**
     * 展示给用户的帮助文本。
     * <p>Help text shown to users.</p>
     *
     * @param language the reply language
     * @return the localized help text
     */
    default String getHelp(Language language) {
        return Text.HELP_UNAVAILABLE.of(language);
    }

    /**
     * 供 AI 文档使用的帮助文本。
     * <p>Help text exposed to AI documentation.</p>
     */
    default String getAiHelp() {
        return "Not supported yet";
    }

    /**
     * 指令所需限权等级。
     * <p>Required access level of the command.</p>
     *
     * @return the required access level
     */
    default Access getAccess() {
        return Access.NORMAL;
    }
}
