package com.zincoid.nullbot.bot.cmd.impl.system;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.render.RenderService;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.util.SystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 信息指令：输出应用、版本与运行环境信息。
 * <p>Info command: prints application, version, and runtime environment information.</p>
 */
@Slf4j
@CmdMapping("Info")
@Component
public class InfoCmd implements Cmd {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectProvider<BuildProperties> builds;
    private final String name;
    private final RenderService renderService;

    public InfoCmd(ObjectProvider<BuildProperties> builds,
                   @Value("${spring.application.name:NullBot}") String name,
                   RenderService renderService) {
        this.builds = builds;
        this.name = name;
        this.renderService = renderService;
    }

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
        String buildTime = build == null || build.getTime() == null
                ? "-"
                : TIME.format(build.getTime().atZone(ZoneId.systemDefault()));
        byte[] png;
        try {
            png = renderService.template("info.jte", Map.of(
                    "app", name,
                    "version", version,
                    "build", buildTime,
                    "javaVersion", SystemInfo.java(),
                    "spring", springBootVersion(),
                    "uptime", uptime(),
                    "os", SystemInfo.os(),
                    "time", LocalDateTime.now().format(TIME)
            ));
        } catch (RuntimeException e) {
            log.warn("Info image render failed: version={}", version, e);
            throw new CmdWarnException(ctx.render(Text.INFO_RENDER_FAILED), e);
        }
        ctx.send(MsgContent.builder().imageBase64(png).build());
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

    private static String springBootVersion() {
        String version = SpringBootVersion.getVersion();
        return version.isBlank() ? "-" : version;
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Info
                Shows application, version, and runtime environment information.
                Usage: Info
                Access: >=0(normal)""" : """
                Info
                查看应用、版本与运行环境信息。
                用法：Info
                权限：>=0(normal)""";
    }
}
