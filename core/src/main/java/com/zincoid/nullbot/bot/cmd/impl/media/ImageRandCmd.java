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
 * 随机图片指令：从图片收藏中随机发送一张。
 * <p>Random image command: sends a random image from the collection.</p>
 */
@CmdMapping({"ImageRand", "image", "img", "随机图片", "图片"})
@Component
public class ImageRandCmd extends MediaRandCmd {

    public ImageRandCmd(FileService fileService) {
        super(fileService);
    }

    @Override
    protected BizType type() {
        return BizType.IMAGE_COLLECTION;
    }

    @Override
    protected Text empty() {
        return Text.NO_IMAGES;
    }

    @Override
    protected MsgContent content(File file) {
        return MsgContent.builder().image(fileService.uri(file)).build();
    }

    @Override
    protected String kind() {
        return "Image";
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                ImageRand
                Sends a random collected image.
                Usage: ImageRand
                Aliases: image/img/随机图片/图片
                Access: >=-1(restricted)""" : """
                ImageRand
                随机发送一张已收藏的图片。
                用法：ImageRand
                别名：image/img/随机图片/图片
                权限：>=-1(restricted)""";
    }
}
