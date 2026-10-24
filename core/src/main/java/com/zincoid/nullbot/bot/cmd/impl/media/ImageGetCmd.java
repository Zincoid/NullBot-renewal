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
 * 获取图片指令：按文件 ID 发送已收藏的图片。
 * <p>Get image command: sends a collected image by file id.</p>
 */
@CmdMapping({"ImageGet", "获取图片"})
@Component
public class ImageGetCmd extends MediaGetCmd {

    public ImageGetCmd(FileService fileService) {
        super(fileService);
    }

    @Override
    protected BizType type() {
        return BizType.IMAGE_COLLECTION;
    }

    @Override
    protected Text notFound() {
        return Text.NOT_IMAGE;
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
                ImageGet
                Sends a collected image by file id.
                Usage: ImageGet [file id]
                Aliases: 获取图片
                Access: >=0(normal)""" : """
                ImageGet
                按文件 id 发送已收藏的图片。
                用法：ImageGet [file id]
                别名：获取图片
                权限：>=0(normal)""";
    }
}
