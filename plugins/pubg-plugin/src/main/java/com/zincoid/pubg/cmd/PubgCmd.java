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
import com.zincoid.pubg.PubgPlugin;
import com.zincoid.pubg.PubgText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * PUBG 指令：按地图名称发送对应的资源地图。
 * <p>PUBG command: sends the resource map matching the given map name.</p>
 */
@Slf4j
@CmdMapping({"PUBG", "吃鸡地图"})
@RequiredArgsConstructor
public class PubgCmd implements Cmd {

    public static final Map<String, String> MAPS = maps();

    private final PubgPlugin plugin;

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
                    ctx.language(), String.join(" / ", MAPS.keySet())));
            return;
        }
        String file = MAPS.get(name.toLowerCase(Locale.ROOT));
        if (file == null)
            throw new CmdWarnException(PubgText.UNSUPPORTED_MAP.of(ctx.language(), name));
        Path path = plugin.mapPath(file);
        ctx.send(MsgContent.builder().image(path.toUri().toString()).build());
        log.info("PUBG map sent: {}", file);
    }

    // ════ Helpers ═════════════════

    private static Map<String, String> maps() {
        Map<String, String> maps = new LinkedHashMap<>();
        map(maps, "艾伦格", "Erangel.png");
        map(maps, "米拉玛", "Miramar.png");
        map(maps, "维寒迪", "Vikendi.png");
        map(maps, "帝斯顿", "Deston.png");
        map(maps, "荣都", "Rondo.png");
        map(maps, "泰戈", "Tiger.png");
        return Collections.unmodifiableMap(maps);
    }

    private static void map(Map<String, String> maps, String name, String file) {
        maps.put(name, file);
        maps.put(name.toLowerCase(Locale.ROOT), file);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return PubgText.HELP.of(language);
    }
}
