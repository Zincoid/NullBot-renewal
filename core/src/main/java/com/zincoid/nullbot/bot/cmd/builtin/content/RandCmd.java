package com.zincoid.nullbot.bot.cmd.builtin.content;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.extern.slf4j.Slf4j;

/**
 * 随机媒体指令基类：从指定收藏中随机发送一个文件。
 * <p>Base of random media commands: sends a random file from the given collection.</p>
 */
@Slf4j
abstract class RandCmd implements Cmd {

    protected final FileService fileService;
    private final BizType type;
    private final Text emptyText;

    protected RandCmd(FileService fileService, BizType type, Text emptyText) {
        this.fileService = fileService;
        this.type = type;
        this.emptyText = emptyText;
    }

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx);
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.RESTRICTED;
    }

    // ════ Helpers ═════════════════

    private void handle(CmdContext ctx) {
        File file = fileService.random(type);
        if (file == null) throw ctx.info(emptyText);
        ctx.send(content(file));
        log.info("Media sent: {}", file.getId());
    }

    private MsgContent content(File file) {
        return switch (file.getType()) {
            case IMAGE -> MsgContent.builder().image(fileService.uri(file)).build();
            case AUDIO -> MsgContent.builder().voice(fileService.uri(file)).build();
            case VIDEO -> MsgContent.builder().video(fileService.uri(file)).build();
            default -> throw new IllegalArgumentException("Not media: " + file.getType());
        };
    }
}
