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
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 获取视频指令：按文件 ID 发送已收藏的视频。
 * <p>Get video command: sends a collected video by file id.</p>
 */
@Slf4j
@CmdMapping({"VideoGet", "获取视频"})
@Component
@RequiredArgsConstructor
public class VideoGetCmd implements Cmd {

    private final FileService fileService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    private void handle(CmdContext ctx, CmdArgs args) {
        long id = args.nextLong();
        File file = fileService.getById(id);
        if (file == null)
            throw new CmdWarnException(ctx.text(Text.FILE_NOT_FOUND, id));
        if (file.getType() != BizType.VID_COLL)
            throw new CmdWarnException(ctx.text(Text.NOT_VIDEO, id));
        ctx.send(MsgContent.builder().video(fileService.uri(file)).build());
        log.info("Video sent: {}", id);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                VideoGet
                Sends a collected video by file id.
                Usage: VideoGet [file id]
                Aliases: 获取视频
                Access: >=0(normal)""" : """
                VideoGet
                按文件 id 发送已收藏的视频。
                用法：VideoGet [file id]
                别名：获取视频
                权限：>=0(normal)""";
    }

    @Override
    public String getAiHelp() {
        return """
                VideoGet
                Sends a collected video by file id.
                Usage: VideoGet [file id]""";
    }
}
