package com.zincoid.nullbot.bot.cmd.impl.image;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.module.file.FileException;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
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
            throw new CmdWarnException("File not found: " + id);
        if (file.getType() != BizType.IMG_COLL)
            throw new CmdWarnException("Not an image: " + id);
        try {
            fileService.delete(id);
        } catch (FileException e) {
            throw new CmdWarnException(e.getMessage());
        }
        log.info("Image deleted: {}", id);
        ctx.send("⚠️ Image deleted: " + id);
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    @Override
    public String getHelp() {
        return """
                ImageDel
                Deletes a collected image.
                Usage: ImageDel [file id]
                Aliases: 删除图片
                Access: >=1(manager)""";
    }
}
