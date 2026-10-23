package com.zincoid.nullbot.bot.cmd.impl.media;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.GroupPokeEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivatePokeEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 随机音频指令：从音频收藏中随机发送一个。
 * <p>Random audio command: sends a random audio from the collection.</p>
 */
@Slf4j
@CmdMapping({"AudioRand", "audio", "aud", "随机音频", "音频"})
@Component
@RequiredArgsConstructor
public class AudioRandCmd implements Cmd {

    private final FileService fileService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx);
    }

    @Override
    public void run(CmdContext ctx, GroupPokeEvent event, CmdArgs args) {
        handle(ctx);
    }

    @Override
    public void run(CmdContext ctx, PrivatePokeEvent event, CmdArgs args) {
        handle(ctx);
    }

    private void handle(CmdContext ctx) {
        File file = fileService.random(BizType.AUD_COLL);
        if (file == null)
            throw new CmdInfoException(ctx.render(Text.NO_AUDIOS));
        ctx.send(MsgContent.builder().voice(fileService.uri(file)).build());
        log.info("Audio sent: {}", file.getId());
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.RESTRICTED;
    }

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                AudioRand
                Sends a random collected audio.
                Usage: AudioRand
                Aliases: audio/aud/随机音频/音频
                Access: >=-1(restricted)""" : """
                AudioRand
                随机发送一个已收藏的音频。
                用法：AudioRand
                别名：audio/aud/随机音频/音频
                权限：>=-1(restricted)""";
    }

    @Override
    public String getAiHelp() {
        return """
                AudioRand
                Sends a random collected audio.
                Usage: AudioRand""";
    }

    @Override
    public boolean isAiCallable() {
        return true;
    }
}
