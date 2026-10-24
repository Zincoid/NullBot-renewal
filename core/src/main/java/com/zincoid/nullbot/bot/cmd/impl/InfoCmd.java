package com.zincoid.nullbot.bot.cmd.impl;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;

/**
 * 信息指令：输出版本与运行环境信息。
 * <p>Info command: prints version and runtime environment information.</p>
 */
@Slf4j
@CmdMapping("Info")
@Component
@RequiredArgsConstructor
public class InfoCmd implements Cmd {

    private final ObjectProvider<BuildProperties> builds;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx);
    }

    private void handle(CmdContext ctx) {
        BuildProperties build = builds.getIfAvailable();
        String version = build == null || build.getVersion() == null ? "dev" : build.getVersion();
        String buildTime = build == null || build.getTime() == null ? "-" : build.getTime().toString();
        ctx.send(Text.INFO, version, buildTime, uptime(), java(), os());
        log.info("Info sent: version={}", version);
    }

    // ════ Helpers ═════════════════

    private static String uptime() {
        long seconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        long days = seconds / 86400;
        long hours = seconds % 86400 / 3600;
        long minutes = seconds % 3600 / 60;
        long secs = seconds % 60;
        return days > 0
                ? "%dd %02dh %02dm %02ds".formatted(days, hours, minutes, secs)
                : "%dh %02dm %02ds".formatted(hours, minutes, secs);
    }

    private static String java() {
        return "%s (%s)".formatted(
                System.getProperty("java.version", "unknown"),
                System.getProperty("java.vendor", "unknown"));
    }

    private static String os() {
        return "%s %s (%s)".formatted(
                System.getProperty("os.name", "unknown"),
                System.getProperty("os.version", "unknown"),
                System.getProperty("os.arch", "unknown"));
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Info
                Shows the bot version and runtime environment.
                Usage: Info
                Access: >=0(normal)""" : """
                Info
                查看机器人版本与运行环境。
                用法：Info
                权限：>=0(normal)""";
    }
}
