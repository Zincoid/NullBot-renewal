package com.zincoid.nullbot.bot.cmd.impl.manage;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 撤回指令：撤回被引用的消息。
 * <p>Recall command: recalls the replied message.</p>
 */
@Slf4j
@CmdMapping({"Recall", "rc", "撤回"})
@Component
public class RecallCmd implements Cmd {

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        recall(ctx, event.content().replyId());
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        recall(ctx, event.content().replyId());
    }

    // ════ Helpers ═════════════════

    /**
     * 撤回引用消息。
     * <p>Recalls the replied message.</p>
     *
     * @param ctx     the command context
     * @param replyId the quoted message id, nullable
     */
    private void recall(CmdContext ctx, String replyId) {
        if (replyId == null)
            throw new CmdWarnException(ctx.render(Text.MISSING_REPLY));
        ctx.client().recall(replyId);
        log.info("Message recalled: {}", replyId);
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Recall
                Recalls the replied message.
                Usage: [reply] Recall
                Aliases: rc / 撤回
                Access: >=1(manager)""" : """
                Recall
                撤回引用的消息。
                用法：[回复] Recall
                别名：rc / 撤回
                权限：>=1(manager)""";
    }
}
