package com.zincoid.hello.cmd;

import com.zincoid.hello.HelloText;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

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
        ctx.send("Hello from plugin! Data: " + DATA_KEY + "=" + data(context.dataDir()));
    }

    // ════ Image ═══════════════════

    private void send(CmdContext ctx) {
        byte[] png;
        try {
            png = context.renderer().template("hello.jte", Map.of(
                    "title", "Hello from plugin!",
                    "subtitle", pluginId + " " + version
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

    // ════ Helpers ═════════════════

    private static String data(Path dataDir) {
        Properties properties = new Properties();
        Path file = dataDir.resolve("config.properties");
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException | IllegalArgumentException e) {
                log.warn("Cannot read Hello plugin config: {}", file, e);
            }
        }
        String value = properties.getProperty(DATA_KEY, DEFAULT_DATA_VALUE);
        return value == null || value.isBlank() ? DEFAULT_DATA_VALUE : value.trim();
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return HelloText.HELP.of(language);
    }
}
