package com.zincoid.nullbot.bot.cmd.impl.video;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
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
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存视频指令：把引用消息中的视频存入视频收藏。
 * <p>Save video command: stores videos of a quoted message into the video collection.</p>
 */
@Slf4j
@CmdMapping({"VideoSave", "保存视频"})
@Component
@RequiredArgsConstructor
public class VideoSaveCmd implements Cmd {

    private final FileService fileService;

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

    private void handle(CmdContext ctx, String userId, String replyId, CmdArgs args) {
        boolean replied = args.hasOpt("id", "i");
        String messageId = replied
                ? args.getOpt("id", "i")
                : replyId;
        if (messageId == null)
            throw new CmdWarnException(ctx.text(Text.MISSING_REPLY));
        if ("true".equals(messageId))
            throw new CmdWarnException(ctx.text(Text.MALFORMED_OPTION_ID));
        MsgDetail detail = ctx.client().getMsg(messageId);
        List<MsgSegment.Video> videos = detail.content().segments().stream()
                .filter(MsgSegment.Video.class::isInstance)
                .map(MsgSegment.Video.class::cast)
                .toList();
        if (videos.isEmpty())
            throw new CmdWarnException(ctx.text(Text.NO_VIDEOS_IN_REPLY));
        List<File> saved = new ArrayList<>();
        for (MsgSegment.Video video : videos) {
            try {
                saved.add(fileService.save(
                        userId, video.url(), video.file(), BizType.VID_COLL));
            } catch (FileException e) {
                throw new CmdWarnException(e.getMessage());
            }
        }
        saved.forEach(file -> log.info("Video saved: {}", file.getId()));
        MsgContent.Builder builder = MsgContent.builder()
                .text(saved.size() == 1
                        ? ctx.text(Text.VIDEO_SAVED_ONE, saved.getFirst().getId())
                        : ctx.text(Text.VIDEO_SAVED_MANY, saved.size()));
        if (replied) builder.reply(messageId);
        ctx.send(builder.build());
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                VideoSave
                Saves videos from a replied message.
                Usage: [reply] VideoSave
                Aliases: 保存视频
                Access: >=0(normal)""" : """
                VideoSave
                保存引用消息中的视频。
                用法：[回复] VideoSave
                别名：保存视频
                权限：>=0(normal)""";
    }

    @Override
    public String getAiHelp() {
        return """
                VideoSave
                Saves all videos of one message into the video collection.
                Usage: VideoSave -i=<message id>
                Note: the id is the [id=...] marker shown before messages in the conversation.""";
    }
}
