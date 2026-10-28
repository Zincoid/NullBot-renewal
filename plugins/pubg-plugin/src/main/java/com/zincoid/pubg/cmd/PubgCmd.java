package com.zincoid.pubg.cmd;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.pubg.PubgText;
import com.zincoid.pubg.service.PubgResources;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

/**
 * PUBG 指令：按地图名称发送对应的资源地图。
 * <p>PUBG command: sends the resource map matching the given map name.</p>
 */
@Slf4j
@CmdMapping({"PUBG", "吃鸡地图"})
@RequiredArgsConstructor
public class PubgCmd implements Cmd {

    private final PubgResources resources;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    private void handle(CmdContext ctx, CmdArgs args) {
        String name = args.hasNext() ? args.next().trim() : "";
        if (name.isEmpty()) {
            ctx.send(PubgText.AVAILABLE_MAPS.of(
                    ctx.language(), String.join(" / ", resources.maps().keySet())));
            return;
        }
        Path path = resources.path(name);
        if (path == null)
            throw new CmdWarnException(PubgText.UNSUPPORTED_MAP.of(ctx.language(), name));
        ctx.send(MsgContent.builder().image(path.toUri().toString()).build());
        log.info("PUBG map sent: {}", path.getFileName());
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return PubgText.HELP.of(language);
    }
}
