package com.zincoid.nullbot.bot.bus.pre;

import com.zincoid.nullbot.bot.bus.CmdProcessor;
import com.zincoid.nullbot.bot.bus.CmdRequest;
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
 * 关键词处理器：按群规则把包含关键词的消息合成指令派发，事件不消费。
 * <p>Keyword handler: dispatches commands synthesized from keyword rules, without consuming the event.</p>
 */
@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class KeywordHandler implements PreHandler {

    private final GroupKeywordService groupKeywordService;
    private final CmdProcessor cmdProcessor;

    // ════ Handle ══════════════════

    @Override
    public void handle(BotClient client, BotEvent event, PreChain chain) {
        if (event instanceof GroupMsgEvent e) react(client, e);
        chain.doHandle(client, event);
    }

    // ════ Helpers ═════════════════

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
            cmdProcessor.process(client, event, new CmdRequest(rule.getCmd(), resolved));
        }
    }

    private static String text(MsgContent content) {
        StringBuilder text = new StringBuilder();
        for (MsgSegment segment : content.segments())
            if (segment instanceof MsgSegment.Text(String s)) text.append(s);
        return text.toString();
    }
}
