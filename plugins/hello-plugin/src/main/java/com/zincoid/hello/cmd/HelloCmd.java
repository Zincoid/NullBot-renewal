package com.zincoid.hello.cmd;

import com.zincoid.hello.HelloPlugin;
import com.zincoid.hello.HelloText;
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
                + plugin.context().dataDir());
    }

    // ════ Image ═══════════════════

    private void send(CmdContext ctx) {
        byte[] png;
        try {
            png = plugin.context().renderer().template("hello.jte", Map.of(
                    "title", "Hello from plugin!",
                    "subtitle", plugin.id() + " " + plugin.version()
            ));
        } catch (RuntimeException e) {
            log.warn("Hello image render failed", e);
            throw new CmdWarnException(HelloText.IMAGE_FAILED.of(ctx.language()), e);
        }
        ctx.send(MsgContent.builder()
                .imageBase64(png)
                .build());
        log.info("Hello image sent: bytes={}", png.length);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return HelloText.HELP.of(language);
    }
}
