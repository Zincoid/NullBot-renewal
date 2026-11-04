package com.zincoid.hello.cmd;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 示例指令：回复配置项，或渲染图片卡片。
 * <p>Sample command replying with a config entry, or rendering an image card.</p>
 */
@Slf4j
@CmdMapping({"Hello", "你好"})
@RequiredArgsConstructor
public class HelloCmd implements Cmd {

    private static final String DATA_KEY = "hello.data";
    private static final String DEFAULT_DATA_VALUE = "hello-plugin-default";

    private final PluginContext context;
    private final String pluginId;
    private final String version;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        log.info("Hello plugin!");
        if (args.hasOpt("image", "i")) {
            send(ctx);
            return;
        }
        ctx.send("Hello from plugin! Data: "
                + context.properties().get(DATA_KEY, DEFAULT_DATA_VALUE));
    }

    // ════ Image ═══════════════════

    private void send(CmdContext ctx) {
        byte[] png = context.renderer().template("hello.jte", Map.of(
                "title", "Hello from plugin!",
                "subtitle", pluginId + " " + version
        ));
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
