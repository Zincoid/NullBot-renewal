package com.zincoid.nullbot.bot.cmd.impl.manage;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 禁言指令：禁言当前群的指定成员。
 * <p>Mute command: mutes the specified member in the current group.</p>
 */
@Slf4j
@CmdMapping({"Mute", "禁言"})
@Component
public class MuteCmd implements Cmd {

    private static final int MAX_MUTE_MINUTES = 30 * 24 * 60;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        Target target = target(event, args);
        if (target == null || target.userId().equals(event.selfId())) return;
        try {
            ctx.client().mute(event.groupId(), target.userId(), target.minutes() * 60);
            log.info("Mute applied: group={}, target={}, minutes={}",
                    event.groupId(), target.userId(), target.minutes());
        } catch (RuntimeException e) {
            log.warn("Mute failed: group={}, target={}", event.groupId(), target.userId(), e);
        }
    }

    // ════ Helpers ═════════════════

    private static Target target(GroupMsgEvent event, CmdArgs args) {
        String targetId;
        int minutes;
        try {
            if (args.size() == 2) {
                targetId = args.get(0);
                minutes = args.getInt(1);
            } else if (args.size() == 1) {
                List<String> at = event.content().atIds();
                if (at.size() != 1) return null;
                targetId = at.getFirst();
                minutes = args.getInt(0);
            } else {
                return null;
            }
        } catch (CmdWarnException e) {
            return null;
        }
        if (minutes < 0 || minutes > MAX_MUTE_MINUTES) return null;
        return new Target(targetId, minutes);
    }

    private record Target(String userId, int minutes) {
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Mute
                Mutes a member in the current group.
                Usage: Mute <user id> <minutes>
                Use 0 minutes to unmute.
                Aliases: 禁言
                Access: >=1(manager)""" : """
                Mute
                禁言当前群成员。
                用法：Mute <用户 ID> <分钟数>
                0 分钟解除禁言。
                别名：禁言
                权限：>=1(manager)""";
    }
}
