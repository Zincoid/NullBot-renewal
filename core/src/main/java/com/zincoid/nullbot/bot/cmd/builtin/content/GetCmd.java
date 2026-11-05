package com.zincoid.nullbot.bot.cmd.builtin.content;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.persistence.po.Quote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 获取收藏指令：按类型与 ID 发送已收藏的媒体或语录。
 * <p>Get-content command: sends collected media or a quote by type and id.</p>
 */
@Slf4j
@CmdMapping({"Get", "获取"})
@Component
@RequiredArgsConstructor
public class GetCmd implements Cmd {

    private final FileService fileService;
    private final QuoteService quoteService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    // ════ Helpers ═════════════════

    private void handle(CmdContext ctx, CmdArgs args) {
        String type = args.next().toLowerCase(Locale.ROOT);
        long id = args.nextLong();
        switch (type) {
            case "img" -> media(ctx, id, BizType.IMAGE);
            case "aud" -> media(ctx, id, BizType.AUDIO);
            case "vid" -> media(ctx, id, BizType.VIDEO);
            case "quot" -> quote(ctx, id);
            default -> throw ctx.warn(Text.UNKNOWN_TYPE, type);
        }
    }

    private void media(CmdContext ctx, long id, BizType expected) {
        File file = fileService.getById(id);
        if (file == null)
            throw ctx.warn(Text.FILE_NOT_FOUND, id);
        if (file.getType() != expected)
            throw ctx.warn(Text.TYPE_MISMATCH, id);
        ctx.send(content(file));
        log.info("Content sent: {}", id);
    }

    private MsgContent content(File file) {
        return switch (file.getType()) {
            case IMAGE -> MsgContent.builder().image(fileService.uri(file)).build();
            case AUDIO -> MsgContent.builder().voice(fileService.uri(file)).build();
            case VIDEO -> MsgContent.builder().video(fileService.uri(file)).build();
            default -> throw new IllegalArgumentException("Not media: " + file.getType());
        };
    }

    private void quote(CmdContext ctx, long id) {
        Quote quote = quoteService.getById(id);
        if (quote == null)
            throw ctx.warn(Text.QUOTE_NOT_FOUND, id);
        ctx.send(quoteService.format(quote));
        log.info("Quote sent: {}", id);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Get
                Sends collected media or a quote by type and id.
                Usage: Get <img|aud|vid|quot> <id>
                Aliases: 获取
                Access: >=0(normal)""" : """
                Get
                按类型与 id 发送已收藏的媒体或语录。
                用法：Get <img|aud|vid|quot> <id>
                别名：获取
                权限：>=0(normal)""";
    }
}
