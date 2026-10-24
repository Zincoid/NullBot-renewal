package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
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
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 随机视频指令：从视频收藏中随机发送一个。
 * <p>Random video command: sends a random video from the collection.</p>
 */
@Slf4j
@CmdMapping({"VideoRand", "video", "vid", "随机视频", "视频"})
@Component
@RequiredArgsConstructor
public class VideoRandCmd implements Cmd {

    private final FileService fileService;

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

    private void handle(CmdContext ctx) {
        File file = fileService.random(BizType.VID_COLL);
        if (file == null)
            throw new CmdInfoException(ctx.render(Text.NO_VIDEOS));
        ctx.send(MsgContent.builder().video(fileService.uri(file)).build());
        log.info("Video sent: {}", file.getId());
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.RESTRICTED;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                VideoRand
                Sends a random collected video.
                Usage: VideoRand
                Aliases: video/vid/随机视频/视频
                Access: >=-1(restricted)""" : """
                VideoRand
                随机发送一个已收藏的视频。
                用法：VideoRand
                别名：video/vid/随机视频/视频
                权限：>=-1(restricted)""";
    }

    @Override
    public String getAiHelp() {
        return """
                VideoRand
                Sends a random collected video.
                Usage: VideoRand""";
    }

    @Override
    public boolean isAiCallable() {
        return true;
    }
}
