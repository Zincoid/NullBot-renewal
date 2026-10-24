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
 * 获取视频指令：按文件 ID 发送已收藏的视频。
 * <p>Get video command: sends a collected video by file id.</p>
 */
@CmdMapping({"VideoGet", "获取视频"})
@Component
public class VideoGetCmd extends MediaGetCmd {

    public VideoGetCmd(FileService fileService) {
        super(fileService);
    }

    @Override
    protected BizType type() {
        return BizType.VIDEO_COLLECTION;
    }

    @Override
    protected Text notFound() {
        return Text.NOT_VIDEO;
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
}
