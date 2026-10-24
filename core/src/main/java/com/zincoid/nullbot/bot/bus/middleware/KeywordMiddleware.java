package com.zincoid.nullbot.bot.bus.middleware;

import com.zincoid.nullbot.bot.cmd.CmdDispatcher;
import com.zincoid.nullbot.bot.cmd.CmdRequest;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.setting.GroupKeywordService;
import com.zincoid.nullbot.platform.persistence.po.GroupKeyword;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 关键词中间件：按群规则把包含关键词的消息合成指令派发，事件不消费。
 * <p>Keyword middleware: dispatches commands synthesized from keyword rules, without consuming the event.</p>
 */
@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class KeywordMiddleware implements Middleware {

    private final GroupKeywordService groupKeywordService;
    private final CmdDispatcher cmdDispatcher;

    // ════ Handle ══════════════════

    /**
     * 按群关键词规则派发合成指令，事件继续传递。
     * <p>Dispatches commands synthesized from group keyword rules and passes the event on.</p>
     *
     * @param client the outbound port
     * @param event  the inbound event
     * @param chain  the remaining middleware chain
     */
    @Override
    public void handle(BotClient client, BotEvent event, MiddlewareChain chain) {
        if (event instanceof GroupMsgEvent e) react(client, e);
        chain.doHandle(client, event);
    }

    // ════ Helpers ═════════════════

    /**
     * 匹配群关键词规则并派发对应指令；规则由管理员配置，因此跳过群和用户权限校验。
     * <p>Matches group keyword rules and dispatches the corresponding commands; rules are configured by
     * administrators, so both group and user access checks are skipped.</p>
     *
     * @param client the outbound port
     * @param event  the triggering group message event
     */
    private void react(BotClient client, GroupMsgEvent event) {
        String text = text(event.content());
        if (text.isBlank()) return;
        for (GroupKeyword rule : groupKeywordService.listEnabled(event.groupId())) {
            if (rule.getKeyword() == null || rule.getKeyword().isBlank()) continue;
            if (rule.getCmd() == null || rule.getCmd().isBlank()) continue;
            if (!text.contains(rule.getKeyword())) continue;
            List<String> args = rule.getArgs() == null ? List.of() : rule.getArgs();
            List<String> resolved = CmdArgs.of(args).resolve(Map.of(
                    "userId", event.userId(),
                    "groupId", event.groupId()
            )).getRaw();
            log.info("Keyword matched: group={}, keyword={}, cmd={}", event.groupId(), rule.getKeyword(), rule.getCmd());
            cmdDispatcher.dispatch(client, event, new CmdRequest(rule.getCmd(), resolved), false);
        }
    }

    /**
     * 提取消息中的文本段。
     * <p>Extracts text segments from the message.</p>
     *
     * @param content the message content
     * @return the concatenated text
     */
    private static String text(MsgContent content) {
        StringBuilder text = new StringBuilder();
        for (MsgSegment segment : content.segments())
            if (segment instanceof MsgSegment.Text(String s)) text.append(s);
        return text.toString();
    }
}
