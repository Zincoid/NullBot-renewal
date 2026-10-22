package com.zincoid.nullbot.bot.cmd.impl.image;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 获取图片指令：按文件 ID 发送已收藏的图片。
 * <p>Get image command: sends a collected image by file id.</p>
 */
@Slf4j
@CmdMapping({"ImageGet", "获取图片"})
@Component
@RequiredArgsConstructor
public class ImageGetCmd implements Cmd {

    private final FileService fileService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    private void handle(CmdContext ctx, CmdArgs args) {
        long id = args.nextLong();
        File file = fileService.getById(id);
        if (file == null)
            throw new CmdWarnException(ctx.render(Text.FILE_NOT_FOUND, id));
        if (file.getType() != BizType.IMG_COLL)
            throw new CmdWarnException(ctx.render(Text.NOT_IMAGE, id));
        ctx.send(MsgContent.builder().image(fileService.uri(file)).build());
        log.info("Image sent: {}", id);
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

    @Override
    public String getAiHelp() {
        return """
                ImageGet
                Sends a collected image by file id.
                Usage: ImageGet [file id]""";
    }
}
