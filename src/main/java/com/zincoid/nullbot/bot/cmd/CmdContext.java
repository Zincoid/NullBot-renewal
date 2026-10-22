package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.bot.spi.event.BotEvent;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.GroupRecallEvent;
import com.zincoid.nullbot.bot.spi.event.JoinEvent;
import com.zincoid.nullbot.bot.spi.event.LeaveEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateRecallEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.List;

/**
 * 指令执行上下文：触发事件、绑定指令与出站端口。
 * <p>Command execution context: the triggering event, the bound command and the outbound port.</p>
 *
 * @param client   the outbound port
 * @param event    the triggering event
 * @param cmd      the bound command
 * @param params   the raw command parameters
 * @param language the resolved reply language
 */
public record CmdContext(
        BotClient client,
        BotEvent event,
        Cmd cmd,
        List<String> params,
        Language language
) {

    public CmdContext {
        params = List.copyOf(params);
        language = language == null ? Language.ZH : language;
    }

    /**
     * 按当前语言渲染文案。
     * <p>Renders text in the current language.</p>
     *
     * @param text the text template
     * @param args the format arguments
     * @return the rendered text
     */
    public String text(Text text, Object... args) {
        return text.of(language, args);
    }

    /**
     * 以纯文本发送到指令来源会话。
     * <p>Sends plain text to the chat the command came from.</p>
     *
     * @param text the text template
     * @param args the format arguments
     */
    public void send(Text text, Object... args) {
        send(text.of(language, args));
    }

    /**
     * 以纯文本发送到指令来源会话。
     * <p>Sends plain text to the chat the command came from.</p>
     *
     * @param text the message text
     */
    public void send(String text) {
        send(MsgContent.of(text));
    }

    /**
     * 以消息内容发送到指令来源会话。
     * <p>Sends message content to the chat the command came from.</p>
     *
     * @param content the message content
     */
    public void send(MsgContent content) {
        switch (event) {
            case GroupMsgEvent e -> client.sendGroupMsg(e.groupId(), content);
            case PrivateMsgEvent e -> client.sendPrivateMsg(e.userId(), content);
            case GroupPokeEvent e -> client.sendGroupMsg(e.groupId(), content);
            case PrivatePokeEvent e -> client.sendPrivateMsg(e.userId(), content);
            case GroupRecallEvent e -> client.sendGroupMsg(e.groupId(), content);
            case PrivateRecallEvent e -> client.sendPrivateMsg(e.userId(), content);
            case JoinEvent e -> client.sendGroupMsg(e.groupId(), content);
            case LeaveEvent e -> client.sendGroupMsg(e.groupId(), content);
        }
    }
}
