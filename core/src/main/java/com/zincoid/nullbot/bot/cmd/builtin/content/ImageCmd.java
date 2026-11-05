package com.zincoid.nullbot.bot.cmd.builtin.content;

import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import org.springframework.stereotype.Component;

/**
 * 随机图片指令：从图片收藏中随机发送一张。
 * <p>Random image command: sends a random image from the collection.</p>
 */
@CmdMapping({"Image", "img", "图片"})
@Component
public class ImageCmd extends RandCmd {

    public ImageCmd(FileService fileService) {
        super(fileService, BizType.IMAGE, Text.NO_IMAGES);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Image
                Sends a random collected image.
                Usage: Image
                Aliases: img/图片
                Access: >=-1(restricted)""" : """
                Image
                随机发送一张已收藏的图片。
                用法：Image
                别名：img/图片
                权限：>=-1(restricted)""";
    }
}
