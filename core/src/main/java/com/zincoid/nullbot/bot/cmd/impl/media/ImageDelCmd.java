package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import org.springframework.stereotype.Component;

/**
 * 删除图片指令：按文件 ID 删除已收藏的图片。
 * <p>Delete image command: deletes a collected image by file id.</p>
 */
@CmdMapping({"ImageDel", "删除图片"})
@Component
public class ImageDelCmd extends MediaDelCmd {

    public ImageDelCmd(FileService fileService) {
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
    protected Text deleted() {
        return Text.IMAGE_DELETED;
    }

    @Override
    protected String kind() {
        return "Image";
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                ImageDel
                Deletes a collected image.
                Usage: ImageDel [file id]
                Aliases: 删除图片
                Access: >=1(manager)""" : """
                ImageDel
                按文件 id 删除已收藏的图片。
                用法：ImageDel [file id]
                别名：删除图片
                权限：>=1(manager)""";
    }
}
