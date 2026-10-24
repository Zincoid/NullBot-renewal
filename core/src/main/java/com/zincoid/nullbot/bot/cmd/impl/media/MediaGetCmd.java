package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.extern.slf4j.Slf4j;

/**
 * 获取媒体指令基类：按文件 ID 发送指定业务类型的文件。
 * <p>Base of get-media commands: sends a file of the given business type by id.</p>
 */
@Slf4j
abstract class MediaGetCmd implements Cmd {

    protected final FileService fileService;

    protected MediaGetCmd(FileService fileService) {
        this.fileService = fileService;
    }

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
        long id = args.nextLong();
        File file = fileService.getById(id);
        if (file == null || file.getType() != type())
            throw new CmdWarnException(ctx.render(notFound(), id));
        ctx.send(content(file));
        log.info("{} sent: {}", kind(), id);
    }

    /**
     * 媒体文件的业务类型。
     * <p>The business type of the media files.</p>
     *
     * @return the business type
     */
    protected abstract BizType type();

    /**
     * 文件不存在或类型不匹配时的提示文案。
     * <p>The message shown when the file is missing or has another type.</p>
     *
     * @return the not-found text
     */
    protected abstract Text notFound();

    /**
     * 把文件记录转换为可发送的消息内容。
     * <p>Converts a file record into sendable message content.</p>
     *
     * @param file the file record
     * @return the message content
     */
    protected abstract MsgContent content(File file);

    /**
     * 媒体类型名称，用于日志。
     * <p>The media kind name, used for logs.</p>
     *
     * @return the media kind
     */
    protected abstract String kind();
}
