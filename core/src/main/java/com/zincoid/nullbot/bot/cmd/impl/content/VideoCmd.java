package com.zincoid.nullbot.bot.cmd.impl.content;

import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import org.springframework.stereotype.Component;

/**
 * 随机视频指令：从视频收藏中随机发送一个。
 * <p>Random video command: sends a random video from the collection.</p>
 */
@CmdMapping({"Video", "vid", "视频"})
@Component
public class VideoCmd extends RandCmd {

    public VideoCmd(FileService fileService) {
        super(fileService, BizType.VIDEO, Text.NO_VIDEOS);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Video
                Sends a random collected video.
                Usage: Video
                Aliases: vid/视频
                Access: >=-1(restricted)""" : """
                Video
                随机发送一个已收藏的视频。
                用法：Video
                别名：vid/视频
                权限：>=-1(restricted)""";
    }
}
