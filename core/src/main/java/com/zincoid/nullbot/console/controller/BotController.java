package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.bot.bus.BotLifecycleTracker;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.console.dto.BotView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制台机器人账号接口：返回当前在线账号信息。
 * <p>Console bot account API: returns the current online account information.</p>
 */
@Slf4j
@RestController
@RequestMapping("/console/bot")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class BotController {

    private final BotLifecycleTracker lifecycleTracker;

    // ════ Query ══════════════════

    /**
     * 查询当前在线账号信息。
     * <p>Returns the current online account information.</p>
     *
     * @return the bot account view
     */
    @GetMapping
    public BotView bot() {
        String selfId = lifecycleTracker.onlineId();
        if (selfId == null) return new BotView(false, null, null, null);
        try {
            UserInfo info = lifecycleTracker.getBotClient().getLoginInfo();
            return new BotView(true, info.userId(), info.nickname(), info.avatar());
        } catch (RuntimeException e) {
            log.warn("Bot info unavailable: {}", selfId, e);
            return new BotView(true, selfId, selfId, null);
        }
    }
}
