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
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.module.file.FileException;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存图片指令：把引用消息中的图片存入图片收藏。
 * <p>Save image command: stores images of a quoted message into the image collection.</p>
 */
@Slf4j
@CmdMapping({"ImageSave", "保存图片"})
@Component
@RequiredArgsConstructor
public class ImageSaveCmd implements Cmd {

    private final FileService fileService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, event.userId(), event.content().replyId(), args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, event.userId(), event.content().replyId(), args);
    }

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        handle(ctx, event.userId(), null, args);
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        handle(ctx, event.userId(), null, args);
    }

    private void handle(CmdContext ctx, String userId, String replyId, CmdArgs args) {
        boolean replied = args.hasOpt("id", "i");
        String messageId = replied
                ? args.getOpt("id", "i")
                : replyId;
        if (messageId == null)
            throw new CmdWarnException("Missing reply");
        if ("true".equals(messageId))
            throw new CmdWarnException("Malformed option: id");
        MsgDetail detail = ctx.client().getMsg(messageId);
        List<MsgSegment.Image> images = detail.content().segments().stream()
                .filter(MsgSegment.Image.class::isInstance)
                .map(MsgSegment.Image.class::cast)
                .toList();
        if (images.isEmpty())
            throw new CmdWarnException("No images in reply");
        List<File> saved = new ArrayList<>();
        for (MsgSegment.Image image : images) {
            try {
                saved.add(fileService.save(
                        userId, image.url(), image.file(), BizType.IMG_COLL));
            } catch (FileException e) {
                throw new CmdWarnException(e.getMessage());
            }
        }
        saved.forEach(file -> log.info("Image saved: {}", file.getId()));
        MsgContent.Builder builder = MsgContent.builder()
                .text(saved.size() == 1
                        ? "💾 Image saved: id=" + saved.getFirst().getId()
                        : "💾 Images saved: " + saved.size());
        if (replied) builder.reply(messageId);
        ctx.send(builder.build());
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp() {
        return """
                ImageSave
                Saves images from a replied message.
                Usage: [reply] ImageSave
                Aliases: 保存图片
                Access: >=0(normal)""";
    }

    @Override
    public String getAiHelp() {
        return """
                ImageSave
                Saves all images of one message into the image collection.
                Usage: ImageSave -i=<message id>
                Note: the id is the [id=...] marker shown before messages in the conversation.""";
    }
}
