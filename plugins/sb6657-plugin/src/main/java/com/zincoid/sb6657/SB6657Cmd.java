package com.zincoid.sb6657;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * SB6657 指令：从接口随机获取一条玩机器烂梗并发送。
 * <p>SB6657 command: fetches a random 玩机器 meme from the API and sends it.</p>
 */
@Slf4j
@CmdMapping({"SB6657", "6657", "玩机器", "玩机器烂梗"})
public class SB6657Cmd implements Cmd {

    private static final String API_URL = "https://api.hguofichp.cn/machine/getRandOne";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw error(ctx, "请求被中断", "Request interrupted");
        } catch (Exception e) {
            log.warn("SB6657 request failed", e);
            throw error(ctx, "接口请求失败", "API request failed");
        }
        ctx.send(data.barrage());
        log.info("SB6657 sent: id={}", data.id());
    }

    // ════ Helpers ═════════════════

    private static MachineData fetch() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200)
            throw new IllegalStateException("HTTP " + response.statusCode());
        MachineResp resp = JsonUtils.parse(response.body(), MachineResp.class);
        if (resp == null || resp.code() == null || resp.code() != 200
                || resp.data() == null || resp.data().barrage() == null
                || resp.data().barrage().isBlank())
            throw new IllegalStateException("Invalid response: " + resp);
        return resp.data();
    }

    private static CmdWarnException error(CmdContext ctx, String zh, String en) {
        return new CmdWarnException(ctx.language() == Language.EN ? en : zh);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                SB6657
                Sends a random 玩机器 meme.
                Usage: SB6657
                Aliases: 6657/玩机器/玩机器烂梗
                Access: >=0(normal)""" : """
                SB6657
                随机发送一条玩机器烂梗。
                用法：SB6657
                别名：6657/玩机器/玩机器烂梗
                权限：>=0(normal)""";
    }

    private record MachineResp(Integer code, String msg, MachineData data) {
    }

    private record MachineData(Long id, String barrage, String cnt, String tags, String submitTime) {
    }
}
