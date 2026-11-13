package com.zincoid.nullbot.bot.aichat;

import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 聊天工具上下文：把单轮数据与宿主能力适配为插件工具接口。
 * <p>Chat tool context: adapts per-turn data and host capabilities to the plugin tool interface.</p>
 */
@RequiredArgsConstructor
final class ChatToolContext implements ToolContext {

    private final ChatTurn turn;
    private final ChatActions actions;
    private final ChatCommands commands;

    // ════ ToolContext ═════════════

    @Override
    public boolean hasFinalSent() {
        return turn.hasFinalSent();
    }

    @Override
    public ToolRes send(String text, List<Long> fileIds, String replyId, boolean last) {
        return actions.send(turn, text, fileIds, replyId, last);
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
        return commands.cmd(turn, name, params);
    }
}
