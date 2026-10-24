package com.zincoid.nullbot.bot.cmd.impl.manage;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.cmd.input.InputManager;
import com.zincoid.nullbot.bot.cmd.input.PageSelector;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.message.MessageWindowStore;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.enums.MessageRole;
import com.zincoid.nullbot.platform.persistence.po.Message;
import com.zincoid.nullbot.platform.util.ChatIds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 历史指令：查看当前会话的聊天历史，或用 -r/--reset、-c/--clear 清空历史。
 * <p>History command: views the current chat history, or clears it with -r/--reset or -c/--clear.</p>
 */
@CmdMapping({"History", "聊天历史", "历史"})
@Component
@RequiredArgsConstructor
public class HistoryCmd implements Cmd {

    private static final int PAGE_SIZE = 10;
    private static final int CONTENT_LIMIT = 80;
    private static final long INPUT_TIMEOUT = 60;

    private final MessageWindowStore messageWindowStore;
    private final FileService fileService;
    private final InputManager inputManager;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        run(ctx, ChatIds.ofGroup(event.groupId()), event.userId(), args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        run(ctx, ChatIds.ofPrivate(event.userId()), event.userId(), args);
    }

    private void run(CmdContext ctx, String chatId, String userId, CmdArgs args) {
        if (args.hasOpt("reset", "r") || args.hasOpt("clear", "c")) {
            reset(ctx, chatId);
            return;
        }
        if (!args.getOpt().isEmpty()) {
            throw new CmdWarnException(ctx.render(Text.HISTORY_UNKNOWN_OP,
                    args.getOpt().keySet().iterator().next()));
        }
        if (!args.isEmpty()) {
            throw new CmdWarnException(ctx.render(Text.HISTORY_UNKNOWN_OP, args.peek()));
        }
        list(ctx, chatId, userId);
    }

    // ════ Operations ══════════════

    private void list(CmdContext ctx, String chatId, String userId) {
        List<Message> messages = messageWindowStore.get(chatId);
        if (messages.isEmpty()) {
            ctx.send(Text.HISTORY_EMPTY);
            return;
        }
        PageSelector.builder(ctx.render(Text.HISTORY_TITLE), messages,
                        message -> summary(ctx, message))
                .inputManager(inputManager)
                .userId(userId)
                .language(ctx.language())
                .sender(ctx::send)
                .size(PAGE_SIZE)
                .timeout(INPUT_TIMEOUT)
                .continuous(true)
                .action(message -> detail(ctx, message))
                .build()
                .start();
    }

    private void reset(CmdContext ctx, String chatId) {
        ctx.send(Text.HISTORY_CLEARED, messageWindowStore.clear(chatId));
    }

    // ════ Helpers ═════════════════

    private static String summary(CmdContext ctx, Message message) {
        return speaker(ctx, message) + ": " + preview(message.getContent());
    }

    private void detail(CmdContext ctx, Message message) {
        StringBuilder text = new StringBuilder(speaker(ctx, message)).append(": ")
                .append(message.getContent() == null ? "" : message.getContent());
        List<String> images = new ArrayList<>();
        List<String> unsent = new ArrayList<>();
        List<String> files = message.getFiles();
        if (files != null) {
            for (String fileId : files) {
                String uri = fileService.imageUri(fileId);
                if (uri == null) unsent.add(fileId);
                else images.add(uri);
            }
        }
        if (!unsent.isEmpty())
            text.append("\n").append(ctx.render(Text.HISTORY_ATTACHMENTS,
                    String.join(", ", unsent)));
        MsgContent.Builder content = MsgContent.builder().text(text.toString());
        images.forEach(content::image);
        ctx.send(content.build());
    }

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
                Navigate with Up / Down / End; send a number to view a message and its images.
                Aliases: 聊天历史 / 历史
                Access: >=1(manager)""" : """
                History
                查看当前会话的聊天历史，或清空历史。
                用法：History
                      History -r|--reset
                      History -c|--clear
                翻页发送 Up / Down / End；发送序号查看单条消息及图片。
                别名：聊天历史 / 历史
                权限：>=1(manager)""";
    }
}
