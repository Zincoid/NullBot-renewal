package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import org.springframework.stereotype.Component;

/**
 * 保存音频指令：把引用消息中的音频存入音频收藏。
 * <p>Save audio command: stores audio of a quoted message into the audio collection.</p>
 */
@CmdMapping({"AudioSave", "保存音频"})
@Component
public class AudioSaveCmd extends MediaSaveCmd {

    public AudioSaveCmd(FileService fileService) {
        super(fileService);
    }

    @Override
    protected BizType type() {
        return BizType.AUDIO_COLLECTION;
    }

    @Override
    protected Class<? extends MsgSegment.Media> segmentType() {
        return MsgSegment.Voice.class;
    }

    @Override
    protected Text none() {
        return Text.NO_AUDIOS_IN_REPLY;
    }

    @Override
    protected Text savedOne() {
        return Text.AUDIO_SAVED_ONE;
    }

    @Override
    protected Text savedMany() {
        return Text.AUDIO_SAVED_MANY;
    }

    @Override
    protected String kind() {
        return "Audio";
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                AudioSave
                Saves audio from a replied message.
                Usage: [reply] AudioSave
                Aliases: 保存音频
                Access: >=0(normal)""" : """
                AudioSave
                保存引用消息中的音频。
                用法：[回复] AudioSave
                别名：保存音频
                权限：>=0(normal)""";
    }
}
