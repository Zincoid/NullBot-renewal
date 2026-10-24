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
 * 获取音频指令：按文件 ID 发送已收藏的音频。
 * <p>Get audio command: sends a collected audio by file id.</p>
 */
@CmdMapping({"AudioGet", "获取音频"})
@Component
public class AudioGetCmd extends MediaGetCmd {

    public AudioGetCmd(FileService fileService) {
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
                AudioGet
                Sends a collected audio by file id.
                Usage: AudioGet [file id]
                Aliases: 获取音频
                Access: >=0(normal)""" : """
                AudioGet
                按文件 id 发送已收藏的音频。
                用法：AudioGet [file id]
                别名：获取音频
                权限：>=0(normal)""";
    }
}
