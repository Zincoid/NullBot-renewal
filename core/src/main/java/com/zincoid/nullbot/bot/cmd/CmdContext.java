package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.bot.spi.event.GroupEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.List;

/**
 * 指令执行上下文：触发消息、绑定指令与出站端口。
 * <p>Command execution context: the triggering message, the bound command and the outbound port.</p>
 *
 * @param client      the outbound port
 * @param event       the triggering message event
 * @param cmd         the bound command
 * @param params      the raw command parameters
 * @param language    the resolved reply language
 * @param checkAccess whether to run access checks
 */
public record CmdContext(
        BotClient client,
        MsgEvent event,
        Cmd cmd,
        List<String> params,
        Language language,
        boolean checkAccess
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
    public String render(Text text, Object... args) {
        return text.of(language, args);
    }

    /**
     * 构造使用当前语言文案的告警异常。
     * <p>Builds a warning exception with text in the current language.</p>
     *
     * @param text the text template
     * @param args the format arguments
     * @return the warning exception
     */
    public CmdWarnException warn(Text text, Object... args) {
        return new CmdWarnException(render(text, args));
    }

    /**
     * 构造使用当前语言文案的提示异常。
     * <p>Builds an info exception with text in the current language.</p>
     *
     * @param text the text template
     * @param args the format arguments
     * @return the info exception
     */
    public CmdInfoException info(Text text, Object... args) {
        return new CmdInfoException(render(text, args));
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
        if (event instanceof GroupEvent e)
            client.sendGroupMsg(e.groupId(), content);
        else client.sendPrivateMsg(event.userId(), content);
    }
}
