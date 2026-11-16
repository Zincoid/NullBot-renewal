package com.zincoid.nullbot.bot.aichat;

import com.zincoid.nullbot.module.ai.tool.ToolHost;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 聊天工具宿主：把单轮数据与宿主能力适配为插件工具接口。
 * <p>Chat tool host: adapts per-turn data and host capabilities to the plugin tool interface.</p>
 */
@RequiredArgsConstructor
final class ChatToolHost implements ToolHost {

    private final ChatTurn turn;
    private final ChatActions actions;
    private final ChatCmds cmds;

    private boolean finalSent;
    private String reasoning;

    // ════ ToolHost ════════════════

    @Override
    public String chatId() {
        return turn.chatId();
    }

    @Override
    public boolean finalSent() {
        return finalSent;
    }

    @Override
    public void reasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    @Override
    public ToolRes send(String text, List<Long> fileIds, String replyId, boolean last) {
        ToolRes res = actions.send(turn, text, fileIds, replyId, reasoning);
        if (last && !res.error()) finalSent = true;
        return res;
    }

    @Override
    public ToolRes mute(String userId, int minutes) {
        return actions.mute(turn, userId, minutes);
    }

    @Override
    public ToolRes recall(String messageId) {
        return actions.recall(turn, messageId);
    }

    @Override
    public ToolRes cmd(String name, List<String> params) {
        return cmds.cmd(turn, name, params);
    }
}
