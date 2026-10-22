package com.zincoid.nullbot.bot.cmd.impl.image;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 删除图片指令：按文件 ID 删除已收藏的图片。
 * <p>Delete image command: deletes a collected image by file id.</p>
 */
@Slf4j
@CmdMapping({"ImageDel", "删除图片"})
@Component
@RequiredArgsConstructor
public class ImageDelCmd implements Cmd {

    private final FileService fileService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        long id = args.nextLong();
        File file = fileService.getById(id);
        if (file == null)
            throw new CmdWarnException(ctx.render(Text.FILE_NOT_FOUND, id));
        if (file.getType() != BizType.IMG_COLL)
            throw new CmdWarnException(ctx.render(Text.NOT_IMAGE, id));
        if (!fileService.delete(id))
            throw new CmdWarnException(ctx.render(Text.FILE_NOT_FOUND, id));
        log.info("Image deleted: {}", id);
        ctx.send(Text.IMAGE_DELETED, id);
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

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
