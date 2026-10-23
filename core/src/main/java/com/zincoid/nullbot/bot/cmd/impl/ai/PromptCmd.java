package com.zincoid.nullbot.bot.cmd.impl.ai;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.module.message.MessageWindowStore;
import com.zincoid.nullbot.module.setting.UserSettingService;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.UserSetting;
import com.zincoid.nullbot.platform.util.ChatIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 提示词指令：查看、设置或重置用户级 AI 提示词，仅支持私聊。
 * <p>Prompt command: views, sets, or resets the user-level AI prompt; private chat only.</p>
 */
@Slf4j
@CmdMapping({"Prompt", "提示词"})
@Component
@RequiredArgsConstructor
public class PromptCmd implements Cmd {

    private final UserSettingService userSettingService;
    private final ConfigService configService;
    private final MessageWindowStore messageWindowStore;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        throw new CmdWarnException(ctx.render(Text.PROMPT_GROUP_HINT));
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        String userId = event.userId();
        if (args.hasOpt("view", "v")) {
            view(ctx, userId);
            return;
        }
        if (args.hasOpt("reset", "r")) {
            update(ctx, userId, "");
            return;
        }
        if (args.hasOpt("set", "s")) {
            update(ctx, userId, prompt(ctx, args));
            return;
        }
        throw new CmdWarnException(ctx.render(Text.PROMPT_UNKNOWN_OP));
    }

    // ════ Operations ══════════════

    private void view(CmdContext ctx, String userId) {
        String prompt = userSettingService.get(userId).getAiPrompt();
        if (prompt == null || prompt.isBlank())
            prompt = configService.getString(Conf.AI_DEFAULT_PROMPT);
        ctx.send(Text.PROMPT_VIEW, prompt);
    }

    private void update(CmdContext ctx, String userId, String prompt) {
        UserSetting setting = userSettingService.get(userId);
        setting.setAiPrompt(prompt);
        userSettingService.update(setting);
        int cleared = messageWindowStore.clear(ChatIds.ofPrivate(userId));
        ctx.send(prompt.isBlank() ? Text.PROMPT_RESET : Text.PROMPT_UPDATED);
        log.info("Prompt updated: user={}, cleared={}", userId, cleared);
    }

    // ════ Helpers ═════════════════

    private static String prompt(CmdContext ctx, CmdArgs args) {
        String prompt = args.getOpt("set", "s");
        if ("true".equals(prompt)) prompt = args.rest("");
        prompt = prompt.trim();
        if (prompt.isEmpty()) throw new CmdWarnException(ctx.render(Text.PROMPT_MISSING));
        return prompt;
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Prompt
                Views, sets, or resets the private-chat AI prompt.
                Usage: Prompt -v|--view
                       Prompt -s|--set <text>
                       Prompt -r|--reset
                Aliases: 提示词
                Access: >=0(normal)
                Note: manages private-chat prompts only; change group prompts with GroupSet -a prompt.
                      Set and reset clear the current chat history.""" : """
                Prompt
                查看、设置或重置私聊 AI 提示词。
                用法：Prompt -v|--view
                      Prompt -s|--set <文本>
                      Prompt -r|--reset
                别名：提示词
                权限：>=0(normal)
                说明：仅管理私聊提示词，群提示词请通过 GroupSet -a prompt 修改。
                      设置和重置会清空当前私聊历史。""";
    }
}
