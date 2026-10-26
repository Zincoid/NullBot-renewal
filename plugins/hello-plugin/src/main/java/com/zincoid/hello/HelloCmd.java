package com.zincoid.hello;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 示例指令：回复插件数据目录，或渲染图片卡片。
 * <p>Sample command replying with the plugin data directory, or rendering an image card.</p>
 */
@Slf4j
@CmdMapping({"Hello", "你好"})
@RequiredArgsConstructor
public class HelloCmd implements Cmd {

    private final HelloPlugin plugin;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        log.info("Hello plugin!");
        if (args.hasOpt("image", "i")) {
            send(ctx);
            return;
        }
        ctx.send("Hello from plugin! Data: "
                + plugin.getContext().dataDir());
    }

    // ════ Image ═══════════════════

    private void send(CmdContext ctx) {
        byte[] png;
        try {
            png = plugin.getContext().renderer().template("hello.jte", Map.of(
                    "title", "Hello from plugin!",
                    "subtitle", plugin.id() + " " + plugin.version()
            ));
        } catch (RuntimeException e) {
            log.warn("Hello image render failed", e);
            throw new CmdWarnException(ctx.language() == Language.EN
                    ? "Failed to render the hello image"
                    : "Hello 图片生成失败", e);
        }
        ctx.send(MsgContent.builder()
                .imageBase64(png)
                .build());
        log.info("Hello image sent: bytes={}", png.length);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Hello
                Sends a hello message, or an image card with --image.
                Usage: Hello [--image|-i]
                Aliases: 你好
                Access: >=0(normal)""" : """
                Hello
                发送示例消息；使用 --image 输出图片卡片。
                用法：Hello [--image|-i]
                别名：你好
                权限：>=0(normal)""";
    }
}
