package com.zincoid.nullbot.bot.cmd.impl.manage;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.message.MessageWindowStore;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.Message;
import com.zincoid.nullbot.platform.util.ChatIds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 历史指令：查看当前会话的聊天历史，或用 -r/--reset、-c/--clear 清空历史。
 * <p>History command: views the current chat history, or clears it with -r/--reset or -c/--clear.</p>
 */
@CmdMapping({"History", "聊天历史", "历史"})
@Component
@RequiredArgsConstructor
public class HistoryCmd implements Cmd {

    private static final int PREVIEW_LIMIT = 10;
    private static final int CONTENT_LIMIT = 80;

    private final MessageWindowStore messageWindowStore;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        run(ctx, ChatIds.ofGroup(event.groupId()), args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        run(ctx, ChatIds.ofPrivate(event.userId()), args);
    }

    private void run(CmdContext ctx, String chatId, CmdArgs args) {
        if (args.hasOpt("reset", "r") || args.hasOpt("clear", "c")) {
            reset(ctx, chatId);
            return;
        }
        if (args.isEmpty()) {
            list(ctx, chatId);
            return;
        }
        throw new CmdWarnException(ctx.render(Text.HISTORY_UNKNOWN_OP, args.peek()));
    }

    // ════ Operations ══════════════

    private void list(CmdContext ctx, String chatId) {
        List<Message> all = messageWindowStore.get(chatId);
        if (all.isEmpty()) {
            ctx.send(Text.HISTORY_EMPTY);
            return;
        }
        List<Message> shown = all.subList(Math.max(0, all.size() - PREVIEW_LIMIT), all.size());
        StringBuilder text = new StringBuilder(ctx.render(Text.HISTORY_HEADER, shown.size()));
        for (Message message : shown) {
            text.append("\n- [id=").append(message.getId()).append("] ")
                    .append(speaker(ctx, message)).append(": ")
                    .append(preview(message.getContent()));
        }
        ctx.send(text.toString());
    }

    private void reset(CmdContext ctx, String chatId) {
        ctx.send(Text.HISTORY_CLEARED, messageWindowStore.clear(chatId));
    }

    // ════ Helpers ═════════════════

    private static String speaker(CmdContext ctx, Message message) {
        if (message.getRole() == MessageRole.ASSISTANT)
            return ctx.render(Text.HISTORY_ASSISTANT);
        return message.getNickname() == null ? message.getUserId() : message.getNickname();
    }

    private static String preview(String content) {
        if (content == null || content.isBlank()) return "";
        String text = content.replaceAll("\\s+", " ").trim();
        return text.length() <= CONTENT_LIMIT ? text : text.substring(0, CONTENT_LIMIT) + "...";
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                History
                Views the current chat history, or clears it.
                Usage: History
                       History -r|--reset
                       History -c|--clear
                Aliases: 聊天历史 / 历史
                Access: >=1(manager)""" : """
                History
                查看当前会话的聊天历史，或清空历史。
                用法：History
                      History -r|--reset
                      History -c|--clear
                别名：聊天历史 / 历史
                权限：>=1(manager)""";
    }
}
