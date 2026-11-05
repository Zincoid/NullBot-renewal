package com.zincoid.nullbot.bot.cmd.builtin.content;

import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import org.springframework.stereotype.Component;

/**
 * 随机音频指令：从音频收藏中随机发送一个。
 * <p>Random audio command: sends a random audio from the collection.</p>
 */
@CmdMapping({"Audio", "aud", "音频"})
@Component
public class AudioCmd extends RandCmd {

    public AudioCmd(FileService fileService) {
        super(fileService, BizType.AUDIO, Text.NO_AUDIOS);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Audio
                Sends a random collected audio.
                Usage: Audio
                Aliases: aud/音频
                Access: >=-1(restricted)""" : """
                Audio
                随机发送一个已收藏的音频。
                用法：Audio
                别名：aud/音频
                权限：>=-1(restricted)""";
    }
}
