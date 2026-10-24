package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.module.file.FileException;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 保存媒体指令基类：把一条消息中指定类型的媒体存入对应收藏。
 * <p>Base of save-media commands: stores the media of one type from a message into the matching collection.</p>
 */
@Slf4j
abstract class MediaSaveCmd implements Cmd {

    protected final FileService fileService;

    protected MediaSaveCmd(FileService fileService) {
        this.fileService = fileService;
    }

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, event.userId(), event.content().replyId(), args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, event.userId(), event.content().replyId(), args);
    }

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        handle(ctx, event.userId(), null, args);
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        handle(ctx, event.userId(), null, args);
    }

    // ════ Cmd ═════════════════════

    @Override
    public boolean isAiCallable() {
        return true;
    }

    @Override
    public String getAiHelp() {
        String kind = kind().toLowerCase(Locale.ROOT);
        return name() + "\nSaves all " + kind + "s of one message into the " + kind
                + " collection.\nUsage: " + name() + " -i=<message id>\n"
                + "Note: the id is the [id=...] marker shown before messages in the conversation.";
    }

    // ════ Helpers ═════════════════

    private void handle(CmdContext ctx, String userId, String replyId, CmdArgs args) {
        boolean replied = args.hasOpt("id", "i");
        String messageId = replied ? args.getOpt("id", "i") : replyId;
        if (messageId == null)
            throw new CmdWarnException(ctx.render(Text.MISSING_REPLY));
        if ("true".equals(messageId))
            throw new CmdWarnException(ctx.render(Text.MALFORMED_OPTION_ID));
        MsgDetail detail = ctx.client().getMsg(messageId);
        List<MsgSegment.Media> media = detail.content().segments().stream()
                .filter(MsgSegment.Media.class::isInstance)
                .map(MsgSegment.Media.class::cast)
                .filter(segmentType()::isInstance)
                .toList();
        if (media.isEmpty())
            throw new CmdWarnException(ctx.render(none()));
        List<File> saved = new ArrayList<>();
        for (MsgSegment.Media item : media) {
            try {
                saved.add(fileService.save(userId, type(), item.url(), item.file()));
            } catch (FileException e) {
                throw new CmdWarnException(e.getMessage());
            }
        }
        saved.forEach(file -> log.info("{} saved: {}", kind(), file.getId()));
        MsgContent.Builder builder = MsgContent.builder()
                .text(saved.size() == 1
                        ? ctx.render(savedOne(), saved.getFirst().getId())
                        : ctx.render(savedMany(), saved.size()));
        if (replied) builder.reply(messageId);
        ctx.send(builder.build());
    }

    /**
     * 媒体文件的业务类型。
     * <p>The business type of the media files.</p>
     *
     * @return the business type
     */
    protected abstract BizType type();

    /**
     * 要保存的消息段类型。
     * <p>The message segment type to save.</p>
     *
     * @return the segment type
     */
    protected abstract Class<? extends MsgSegment.Media> segmentType();

    /**
     * 消息中没有可保存媒体时的提示文案。
     * <p>The message shown when the replied message has no matching media.</p>
     *
     * @return the empty-collection text
     */
    protected abstract Text none();

    /**
     * 只保存一个文件时的提示文案。
     * <p>The message shown when exactly one file is saved.</p>
     *
     * @return the single-file text
     */
    protected abstract Text savedOne();

    /**
     * 保存多个文件时的提示文案。
     * <p>The message shown when multiple files are saved.</p>
     *
     * @return the multi-file text
     */
    protected abstract Text savedMany();

    /**
     * 媒体类型名称，用于日志和 AI 帮助。
     * <p>The media kind name, used for logs and AI help.</p>
     *
     * @return the media kind
     */
    protected abstract String kind();
}
