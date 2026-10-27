package com.zincoid.gallery.cmd;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.gallery.GalleryPlugin;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.i18n.Text;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

/**
 * 图库随机图片指令基类：从指定分类中随机发送一张图片。
 * <p>Base gallery image command: sends a random image from a category.</p>
 */
@Slf4j
abstract class GalleryImageCmd implements Cmd {

    private final GalleryPlugin plugin;
    private final String category;

    protected GalleryImageCmd(GalleryPlugin plugin, String category) {
        this.plugin = plugin;
        this.category = category;
    }

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        send(ctx);
    }

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        send(ctx);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        send(ctx);
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        send(ctx);
    }

    // ════ Send ════════════════════

    private void send(CmdContext ctx) {
        Path image = plugin.images().randomImage(category);
        if (image == null)
            throw new CmdInfoException(ctx.render(Text.NO_IMAGES));
        ctx.send(MsgContent.builder().image(image.toUri().toString()).build());
        log.info("{} image sent: {}", name(), image);
    }
}
