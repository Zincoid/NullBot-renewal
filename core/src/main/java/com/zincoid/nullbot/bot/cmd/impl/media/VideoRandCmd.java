package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.File;
import org.springframework.stereotype.Component;

/**
 * 随机视频指令：从视频收藏中随机发送一个。
 * <p>Random video command: sends a random video from the collection.</p>
 */
@CmdMapping({"VideoRand", "video", "vid", "随机视频", "视频"})
@Component
public class VideoRandCmd extends MediaRandCmd {

    public VideoRandCmd(FileService fileService) {
        super(fileService);
    }

    @Override
    protected BizType type() {
        return BizType.VIDEO_COLLECTION;
    }

    @Override
    protected Text empty() {
        return Text.NO_VIDEOS;
    }

    @Override
    protected MsgContent content(File file) {
        return MsgContent.builder().video(fileService.uri(file)).build();
    }

    @Override
    protected String kind() {
        return "Video";
    }

    // ════ Cmd ═════════════════════

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
}
