package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import org.springframework.stereotype.Component;

/**
 * 删除视频指令：按文件 ID 删除已收藏的视频。
 * <p>Delete video command: deletes a collected video by file id.</p>
 */
@CmdMapping({"VideoDel", "删除视频"})
@Component
public class VideoDelCmd extends MediaDelCmd {

    public VideoDelCmd(FileService fileService) {
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
    protected Text deleted() {
        return Text.VIDEO_DELETED;
    }

    @Override
    protected String kind() {
        return "Video";
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                VideoDel
                Deletes a collected video.
                Usage: VideoDel [file id]
                Aliases: 删除视频
                Access: >=1(manager)""" : """
                VideoDel
                按文件 id 删除已收藏的视频。
                用法：VideoDel [file id]
                别名：删除视频
                权限：>=1(manager)""";
    }
}
