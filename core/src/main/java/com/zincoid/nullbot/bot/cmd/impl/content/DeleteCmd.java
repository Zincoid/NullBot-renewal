package com.zincoid.nullbot.bot.cmd.impl.content;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 删除收藏指令：按类型与 ID 删除已收藏的媒体或语录。
 * <p>Delete-content command: deletes collected media or a quote by type and id.</p>
 */
@Slf4j
@CmdMapping({"Delete", "删除"})
@Component
@RequiredArgsConstructor
public class DeleteCmd implements Cmd {

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

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    // ════ Helpers ═════════════════

    private void handle(CmdContext ctx, CmdArgs args) {
        String type = args.next().toLowerCase(Locale.ROOT);
        long id = args.nextLong();
        switch (type) {
            case "img" -> media(ctx, id, BizType.IMAGE, Text.IMAGE_DELETED);
            case "aud" -> media(ctx, id, BizType.AUDIO, Text.AUDIO_DELETED);
            case "vid" -> media(ctx, id, BizType.VIDEO, Text.VIDEO_DELETED);
            case "quot" -> quote(ctx, id);
            default -> throw ctx.warn(Text.UNKNOWN_TYPE, type);
        }
    }

    private void media(CmdContext ctx, long id, BizType expected, Text deleted) {
        File file = fileService.getById(id);
        if (file == null)
            throw ctx.warn(Text.FILE_NOT_FOUND, id);
        if (file.getType() != expected)
            throw ctx.warn(Text.TYPE_MISMATCH, id);
        fileService.delete(id);
        ctx.send(deleted, id);
        log.info("Content deleted: {}", id);
    }

    private void quote(CmdContext ctx, long id) {
        if (quoteService.getById(id) == null)
            throw ctx.warn(Text.QUOTE_NOT_FOUND, id);
        quoteService.delete(id);
        ctx.send(Text.QUOTE_DELETED, id);
        log.info("Quote deleted: {}", id);
    }

    // ════ Help ════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Delete
                Deletes collected media or a quote by type and id.
                Usage: Delete <img|aud|vid|quot> <id>
                Aliases: 删除
                Access: >=1(manager)""" : """
                Delete
                按类型与 id 删除已收藏的媒体或语录。
                用法：Delete <img|aud|vid|quot> <id>
                别名：删除
                权限：>=1(manager)""";
    }
}
