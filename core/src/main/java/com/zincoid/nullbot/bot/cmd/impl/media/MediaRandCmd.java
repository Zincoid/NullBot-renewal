package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

/**
 * 随机媒体指令基类：从指定业务类型中随机发送一个文件。
 * <p>Base of random media commands: sends a random file of the given business type.</p>
 */
@Slf4j
abstract class MediaRandCmd implements Cmd {

    protected final FileService fileService;

    protected MediaRandCmd(FileService fileService) {
        this.fileService = fileService;
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

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        handle(ctx);
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        handle(ctx);
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.RESTRICTED;
    }

    @Override
    public boolean isAiCallable() {
        return true;
    }

    @Override
    public String getAiHelp() {
        String kind = kind().toLowerCase(Locale.ROOT);
        return name() + "\nSends a random collected " + kind + ".\nUsage: " + name();
    }

    // ════ Helpers ═════════════════

    private void handle(CmdContext ctx) {
        File file = fileService.random(type());
        if (file == null) throw new CmdInfoException(ctx.render(empty()));
        ctx.send(content(file));
        log.info("{} sent: {}", kind(), file.getId());
    }

    /**
     * 媒体文件的业务类型。
     * <p>The business type of the media files.</p>
     *
     * @return the business type
     */
    protected abstract BizType type();

    /**
     * 没有可发送文件时的提示文案。
     * <p>The message shown when no file is available.</p>
     *
     * @return the empty-collection text
     */
    protected abstract Text empty();

    /**
     * 把文件记录转换为可发送的消息内容。
     * <p>Converts a file record into sendable message content.</p>
     *
     * @param file the file record
     * @return the message content
     */
    protected abstract MsgContent content(File file);

    /**
     * 媒体类型名称，用于日志和 AI 帮助。
     * <p>The media kind name, used for logs and AI help.</p>
     *
     * @return the media kind
     */
    protected abstract String kind();
}
