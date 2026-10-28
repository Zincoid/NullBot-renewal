package com.zincoid.sb6657.cmd;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.module.http.HttpService;
import com.zincoid.nullbot.platform.exception.PlatformException;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.util.JsonUtils;
import com.zincoid.sb6657.SB6657Exception;
import com.zincoid.sb6657.SB6657Text;
import com.zincoid.sb6657.service.SB6657Http;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SB6657 指令：从接口随机获取一条玩机器烂梗并发送。
 * <p>SB6657 command: fetches a random 玩机器 meme from the API and sends it.</p>
 */
@Slf4j
@CmdMapping({"SB6657", "6657", "玩机器", "玩机器烂梗"})
@RequiredArgsConstructor
public class SB6657Cmd implements Cmd {

    private final SB6657Http http;

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
        MachineData data;
        try {
            data = fetch();
        } catch (SB6657Exception e) {
            log.warn("SB6657 request failed", e);
            throw new CmdWarnException(Thread.currentThread().isInterrupted()
                    ? SB6657Text.INTERRUPTED.of(ctx.language())
                    : SB6657Text.REQUEST_FAILED.of(ctx.language()));
        }
        ctx.send(data.barrage());
        log.info("SB6657 sent: id={}", data.id());
    }

    // ════ Helpers ═════════════════

    private MachineData fetch() {
        HttpService.Response response = http.get();
        if (response.status() != 200)
            throw new SB6657Exception("HTTP " + response.status());
        MachineResp resp;
        try {
            resp = JsonUtils.parse(response.text(), MachineResp.class);
        } catch (PlatformException e) {
            throw new SB6657Exception("Invalid response", e);
        }
        if (resp == null || resp.code() == null || resp.code() != 200
                || resp.data() == null || resp.data().barrage() == null
                || resp.data().barrage().isBlank())
            throw new SB6657Exception("Invalid response: " + resp);
        return resp.data();
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return SB6657Text.HELP.of(language);
    }

    private record MachineResp(Integer code, String msg, MachineData data) {
    }

    private record MachineData(Long id, String barrage, String cnt, String tags, String submitTime) {
    }
}
