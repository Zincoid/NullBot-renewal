package com.zincoid.nullbot.bot.cmd.impl.system;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.render.RenderService;
import com.zincoid.nullbot.module.system.UsageService;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.util.HttpUtils;
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

    private final UsageService usageService;
    private final RenderService renderService;

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
        long total = total(ctx, userId);
        byte[] avatar = avatar(ctx, userId);
        byte[] png = render(ctx, avatar, total);
        ctx.send(MsgContent.builder()
                .imageBase64(png)
                .build());
        log.info("Usage sent: user={}, count={}", userId, total);
    }

    // ════ Helpers ═════════════════

    private long total(CmdContext ctx, String userId) {
        try {
            return usageService.total(userId);
        } catch (RuntimeException e) {
            log.warn("Usage count failed: user={}", userId, e);
            throw new CmdWarnException(ctx.render(Text.USAGE_COUNT_FAILED), e);
        }
    }

    private byte[] avatar(CmdContext ctx, String userId) {
        try {
            UserInfo info = ctx.client().getUserInfo(userId);
            if (info == null || info.avatar() == null || info.avatar().isBlank())
                throw new CmdWarnException(ctx.render(Text.USAGE_AVATAR_FAILED));
            byte[] bytes = HttpUtils.getBytes(info.avatar(), MAX_AVATAR_BYTES);
            String mime = MimeUtils.mime(bytes);
            if (mime == null || !mime.startsWith("image/"))
                throw new CmdWarnException(ctx.render(Text.USAGE_AVATAR_FAILED));
            return bytes;
        } catch (CmdWarnException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Usage avatar failed: user={}", userId, e);
            throw new CmdWarnException(ctx.render(Text.USAGE_AVATAR_FAILED), e);
        }
    }

    private byte[] render(CmdContext ctx, byte[] avatar, long total) {
        try {
            return renderService.usage(avatar, total);
        } catch (RuntimeException e) {
            log.warn("Usage render failed: count={}", total, e);
            throw new CmdWarnException(ctx.render(Text.USAGE_RENDER_FAILED), e);
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
