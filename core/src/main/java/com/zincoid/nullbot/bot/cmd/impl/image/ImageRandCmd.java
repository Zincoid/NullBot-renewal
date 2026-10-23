package com.zincoid.nullbot.bot.cmd.impl.image;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
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
 * 随机图片指令：从图片收藏中随机发送一张。
 * <p>Random image command: sends a random image from the collection.</p>
 */
@Slf4j
@CmdMapping({"ImageRand", "image", "随机图片", "图片"})
@Component
@RequiredArgsConstructor
public class ImageRandCmd implements Cmd {

    private final FileService fileService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx);
    }

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        handle(ctx);
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        handle(ctx);
    }

    private void handle(CmdContext ctx) {
        File file = fileService.random(BizType.IMG_COLL);
        if (file == null)
            throw new CmdInfoException(ctx.render(Text.NO_IMAGES));
        ctx.send(MsgContent.builder().image(fileService.uri(file)).build());
        log.info("Image sent: {}", file.getId());
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.RESTRICTED;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                ImageRand
                Sends a random collected image.
                Usage: ImageRand
                Aliases: image/随机图片/图片
                Access: >=-1(restricted)""" : """
                ImageRand
                随机发送一张已收藏的图片。
                用法：ImageRand
                别名：image/随机图片/图片
                权限：>=-1(restricted)""";
    }

    @Override
    public String getAiHelp() {
        return """
                ImageRand
                Sends a random collected image.
                Usage: ImageRand""";
    }

    @Override
    public boolean isAiCallable() {
        return true;
    }
}
