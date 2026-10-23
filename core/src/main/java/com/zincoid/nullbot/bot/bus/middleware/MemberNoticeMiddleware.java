package com.zincoid.nullbot.bot.bus.middleware;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.i18n.LanguageService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 成员通知中间件：按群设置发送入群欢迎与退群提示。
 * <p>Member notice middleware: sends join welcomes and leave notices according to the group settings.</p>
 */
@Slf4j
@Order(4)
@Component
@RequiredArgsConstructor
public class MemberNoticeMiddleware implements Middleware {

    private final GroupSettingService groupSettingService;
    private final LanguageService languageService;

    // ════ Handle ══════════════════

    /**
     * 处理入群与退群事件，发送通知后消费事件。
     * <p>Handles join and leave events, sending notices and consuming the event.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @param chain  the remaining middleware chain
     */
    @Override
    public void handle(BotClient client, BotEvent event, MiddlewareChain chain) {
        if (event instanceof JoinEvent e) {
            if (!e.selfId().equals(e.userId())
                    && groupSettingService.get(e.groupId()).getJoinNoticeEnabled())
                notice(client, e.groupId(), e.userId(), Text.MEMBER_JOINED);
            chain.consume();
            return;
        }
        if (event instanceof LeaveEvent e) {
            if (!e.selfId().equals(e.userId())
                    && groupSettingService.get(e.groupId()).getLeaveNoticeEnabled())
                notice(client, e.groupId(), e.userId(),
                        e.kicked() ? Text.MEMBER_KICKED : Text.MEMBER_LEFT);
            chain.consume();
            return;
        }
        chain.doHandle(client, event);
    }

    // ════ Helpers ═════════════════

    /**
     * 按群语言发送成员通知。
     * <p>Sends a member notice in the group language.</p>
     *
     * @param client  the outbound port
     * @param groupId platform group id
     * @param userId  platform user id
     * @param text    the notice template
     */
    private void notice(BotClient client, String groupId, String userId, Text text) {
        Language language = languageService.group(groupId);
        String nickname = nickname(client, userId);
        client.sendGroupMsg(groupId, text.of(language, nickname, userId));
        log.info("Member notice: group={}, user={}, type={}", groupId, userId, text.name());
    }

    /**
     * 查询用户昵称，失败时回退为 ID。
     * <p>Looks up the user nickname, falling back to the id.</p>
     *
     * @param client the outbound port
     * @param userId platform user id
     * @return the nickname or the id
     */
    private static String nickname(BotClient client, String userId) {
        try {
            return client.getUserInfo(userId).nickname();
        } catch (RuntimeException e) {
            return userId;
        }
    }
}
