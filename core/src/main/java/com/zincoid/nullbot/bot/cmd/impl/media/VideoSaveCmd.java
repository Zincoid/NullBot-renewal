package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import org.springframework.stereotype.Component;

/**
 * 保存视频指令：把引用消息中的视频存入视频收藏。
 * <p>Save video command: stores videos of a quoted message into the video collection.</p>
 */
@CmdMapping({"VideoSave", "保存视频"})
@Component
public class VideoSaveCmd extends MediaSaveCmd {

    public VideoSaveCmd(FileService fileService) {
        super(fileService);
    }

    @Override
    protected BizType type() {
        return BizType.VIDEO_COLLECTION;
    }

    @Override
    protected Class<? extends MsgSegment.Media> segmentType() {
        return MsgSegment.Video.class;
    }

    @Override
    protected Text none() {
        return Text.NO_VIDEOS_IN_REPLY;
    }

    @Override
    protected Text savedOne() {
        return Text.VIDEO_SAVED_ONE;
    }

    @Override
    protected Text savedMany() {
        return Text.VIDEO_SAVED_MANY;
    }

    @Override
    protected String kind() {
        return "Video";
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
}
