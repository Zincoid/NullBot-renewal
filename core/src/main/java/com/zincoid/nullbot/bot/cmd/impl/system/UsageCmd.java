package com.zincoid.nullbot.bot.cmd.impl.system;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.module.http.HttpService;
import com.zincoid.nullbot.module.render.RenderService;
import com.zincoid.nullbot.module.system.CmdUsageService;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.util.MimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用量指令：渲染当前用户的历史指令用量卡片。
 * <p>Usage command: renders the all-time command usage card of the current user.</p>
 */
@Slf4j
@CmdMapping({"Usage", "用量"})
@Component
@RequiredArgsConstructor
public class UsageCmd implements Cmd {

    private static final int MAX_AVATAR_BYTES = 5 * 1024 * 1024;

    private final CmdUsageService cmdUsageService;
    private final RenderService renderService;
    private final HttpService http;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, event.userId());
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, event.userId());
    }

    private void handle(CmdContext ctx, String userId) {
        long total = cmdUsageService.total(userId);
        UserInfo info = userInfo(ctx, userId);
        byte[] avatar = info == null ? null : avatar(info);
        String nickname = info == null ? null : info.nickname();
        byte[] png = renderService.usage(avatar, nickname, userId, total);
        ctx.send(MsgContent.builder()
                .imageBase64(png)
                .build());
        log.info("Usage sent: user={}, count={}", userId, total);
    }

    // ════ Helpers ═════════════════

    private UserInfo userInfo(CmdContext ctx, String userId) {
        try {
            return ctx.client().getUserInfo(userId);
        } catch (RuntimeException e) {
            log.warn("Usage user info failed: user={}", userId, e);
            return null;
        }
    }

    private byte[] avatar(UserInfo info) {
        if (info.avatar() == null || info.avatar().isBlank()) return null;
        try {
            HttpService.Response response = http.get(info.avatar(),
                    HttpService.Options.defaults().withMaxBytes(MAX_AVATAR_BYTES));
            if (!response.ok()) return null;
            byte[] bytes = response.body();
            String mime = MimeUtils.mime(bytes);
            if (mime == null || !mime.startsWith("image/")) return null;
            return bytes;
        } catch (RuntimeException e) {
            log.warn("Usage avatar failed: user={}", info.userId(), e);
            return null;
        }
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Usage
                Renders your all-time command usage card.
                Usage: Usage
                Aliases: 用量
                Access: >=0(normal)""" : """
                Usage
                渲染当前用户的历史指令用量卡片。
                用法：Usage
                别名：用量
                权限：>=0(normal)""";
    }
}
