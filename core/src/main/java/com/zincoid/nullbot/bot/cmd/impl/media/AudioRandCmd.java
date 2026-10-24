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
 * 随机音频指令：从音频收藏中随机发送一个。
 * <p>Random audio command: sends a random audio from the collection.</p>
 */
@CmdMapping({"AudioRand", "audio", "aud", "随机音频", "音频"})
@Component
public class AudioRandCmd extends MediaRandCmd {

    public AudioRandCmd(FileService fileService) {
        super(fileService);
    }

    @Override
    protected BizType type() {
        return BizType.AUDIO_COLLECTION;
    }

    @Override
    protected Text empty() {
        return Text.NO_AUDIOS;
    }

    @Override
    protected MsgContent content(File file) {
        return MsgContent.builder().voice(fileService.uri(file)).build();
    }

    @Override
    protected String kind() {
        return "Audio";
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                AudioRand
                Sends a random collected audio.
                Usage: AudioRand
                Aliases: audio/aud/随机音频/音频
                Access: >=-1(restricted)""" : """
                AudioRand
                随机发送一个已收藏的音频。
                用法：AudioRand
                别名：audio/aud/随机音频/音频
                权限：>=-1(restricted)""";
    }
}
