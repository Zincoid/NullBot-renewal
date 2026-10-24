package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import org.springframework.stereotype.Component;

/**
 * 删除音频指令：按文件 ID 删除已收藏的音频。
 * <p>Delete audio command: deletes a collected audio by file id.</p>
 */
@CmdMapping({"AudioDel", "删除音频"})
@Component
public class AudioDelCmd extends MediaDelCmd {

    public AudioDelCmd(FileService fileService) {
        super(fileService);
    }

    @Override
    protected BizType type() {
        return BizType.AUDIO_COLLECTION;
    }

    @Override
    protected Text notFound() {
        return Text.NOT_AUDIO;
    }

    @Override
    protected Text deleted() {
        return Text.AUDIO_DELETED;
    }

    @Override
    protected String kind() {
        return "Audio";
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                AudioDel
                Deletes a collected audio.
                Usage: AudioDel [file id]
                Aliases: 删除音频
                Access: >=1(manager)""" : """
                AudioDel
                按文件 id 删除已收藏的音频。
                用法：AudioDel [file id]
                别名：删除音频
                权限：>=1(manager)""";
    }
}
