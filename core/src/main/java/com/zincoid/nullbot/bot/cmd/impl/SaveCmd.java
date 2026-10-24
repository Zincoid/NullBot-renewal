package com.zincoid.nullbot.bot.cmd.impl;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.impl.media.AudioSaveCmd;
import com.zincoid.nullbot.bot.cmd.impl.media.ImageSaveCmd;
import com.zincoid.nullbot.bot.cmd.impl.media.VideoSaveCmd;
import com.zincoid.nullbot.bot.cmd.impl.quote.QuoteSaveCmd;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 通用保存指令：按被引用消息的内容自动选择视频、图片、音频或语录保存。
 * <p>Generic save command: picks video, image, audio, or quote saving based on the replied message.</p>
 */
@CmdMapping({"Save", "保存"})
@Component
@RequiredArgsConstructor
public class SaveCmd implements Cmd {

    private final VideoSaveCmd videoSaveCmd;
    private final ImageSaveCmd imageSaveCmd;
    private final AudioSaveCmd audioSaveCmd;
    private final QuoteSaveCmd quoteSaveCmd;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        target(ctx, event.content().replyId(), args).run(ctx, event, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        target(ctx, event.content().replyId(), args).run(ctx, event, args);
    }

    // ════ Helpers ═════════════════

    private Cmd target(CmdContext ctx, String replyId, CmdArgs args) {
        MsgDetail detail = detail(ctx, replyId, args);
        if (detail.content().segments().stream().anyMatch(MsgSegment.Video.class::isInstance))
            return videoSaveCmd;
        if (detail.content().segments().stream().anyMatch(MsgSegment.Image.class::isInstance))
            return imageSaveCmd;
        if (detail.content().segments().stream().anyMatch(MsgSegment.Voice.class::isInstance))
            return audioSaveCmd;
        return quoteSaveCmd;
    }

    private static MsgDetail detail(CmdContext ctx, String replyId, CmdArgs args) {
        boolean replied = args.hasOpt("id", "i");
        String messageId = replied
                ? args.getOpt("id", "i")
                : replyId;
        if (messageId == null)
            throw new CmdWarnException(ctx.render(Text.MISSING_REPLY));
        if ("true".equals(messageId))
            throw new CmdWarnException(ctx.render(Text.MALFORMED_OPTION_ID));
        return ctx.client().getMsg(messageId);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Save
                Saves the replied message by type, priority: video > image > audio > quote.
                Usage: [reply] Save
                Aliases: 保存
                Access: >=0(normal)""" : """
                Save
                保存引用消息，自动识别类型，优先级：视频 > 图片 > 音频 > 语录。
                用法：[回复] Save
                别名：保存
                权限：>=0(normal)""";
    }
}
