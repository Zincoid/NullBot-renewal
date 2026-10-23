package com.zincoid.hello;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.extern.slf4j.Slf4j;

/**
 * 示例指令：回复插件数据目录。
 * <p>Sample command replying with the plugin data directory.</p>
 */
@Slf4j
@CmdMapping({"Hello", "你好"})
public class HelloCmd implements Cmd {

    private final HelloPlugin plugin;

    public HelloCmd(HelloPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        log.info("Hello plugin!");
        ctx.send("""
                Hello from plugin!
                Data: %s""".formatted(plugin.getContext().dataDir()));
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN
                ? "Hello: sample plugin command."
                : "Hello：示例插件指令。";
    }
}
