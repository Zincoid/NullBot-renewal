package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import org.springframework.stereotype.Component;

/**
 * 保存图片指令：把引用消息中的图片存入图片收藏。
 * <p>Save image command: stores images of a quoted message into the image collection.</p>
 */
@CmdMapping({"ImageSave", "保存图片"})
@Component
public class ImageSaveCmd extends MediaSaveCmd {

    public ImageSaveCmd(FileService fileService) {
        super(fileService);
    }

    @Override
    protected BizType type() {
        return BizType.IMAGE_COLLECTION;
    }

    @Override
    protected Class<? extends MsgSegment.Media> segmentType() {
        return MsgSegment.Image.class;
    }

    @Override
    protected Text none() {
        return Text.NO_IMAGES_IN_REPLY;
    }

    @Override
    protected Text savedOne() {
        return Text.IMAGE_SAVED_ONE;
    }

    @Override
    protected Text savedMany() {
        return Text.IMAGE_SAVED_MANY;
    }

    @Override
    protected String kind() {
        return "Image";
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                ImageSave
                Saves images from a replied message.
                Usage: [reply] ImageSave
                Aliases: 保存图片
                Access: >=0(normal)""" : """
                ImageSave
                保存引用消息中的图片。
                用法：[回复] ImageSave
                别名：保存图片
                权限：>=0(normal)""";
    }
}
