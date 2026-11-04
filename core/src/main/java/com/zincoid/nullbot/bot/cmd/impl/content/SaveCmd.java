package com.zincoid.nullbot.bot.cmd.impl.content;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.MsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.persistence.po.Quote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通用保存指令：按被引用消息的内容自动选择视频、图片、音频或语录保存。
 * <p>Generic save command: picks video, image, audio, or quote saving based on the replied message.</p>
 */
@Slf4j
@CmdMapping({"Save", "保存"})
@Component
@RequiredArgsConstructor
public class SaveCmd implements Cmd {

    private final FileService fileService;
    private final QuoteService quoteService;

    // ════ Rules ═══════════════════

    /**
     * 媒体识别规则，按优先级排列：视频 > 图片 > 音频。
     * <p>Media detection rules in priority order: video, image, audio.</p>
     */
    private static final List<MediaRule> MEDIA_RULES = List.of(
            new MediaRule(MsgSegment.Video.class, BizType.VIDEO,
                    Text.VIDEO_SAVED_ONE, Text.VIDEO_SAVED_MANY),
            new MediaRule(MsgSegment.Image.class, BizType.IMAGE,
                    Text.IMAGE_SAVED_ONE, Text.IMAGE_SAVED_MANY),
            new MediaRule(MsgSegment.Voice.class, BizType.AUDIO,
                    Text.AUDIO_SAVED_ONE, Text.AUDIO_SAVED_MANY));

    /**
     * 单类媒体的保存规则。
     * <p>Save rule for one kind of media.</p>
     *
     * @param segment   segment type
     * @param type      collection business type
     * @param savedOne  text when a single file is saved
     * @param savedMany text when several files are saved
     */
    private record MediaRule(Class<? extends MsgSegment.Media> segment,
                             BizType type, Text savedOne, Text savedMany) {

        private List<MsgSegment.Media> select(List<MsgSegment> segments) {
            return segments.stream()
                    .filter(segment::isInstance)
                    .map(MsgSegment.Media.class::cast)
                    .toList();
        }
    }

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, event);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, event);
    }

    // ════ Helpers ═════════════════

    private void handle(CmdContext ctx, MsgEvent event) {
        String messageId = event.content().replyId();
        if (messageId == null)
            throw ctx.warn(Text.MISSING_REPLY);
        MsgDetail detail = ctx.client().getMsg(messageId);
        List<MsgSegment> segments = detail.content().segments();
        for (MediaRule rule : MEDIA_RULES) {
            List<MsgSegment.Media> media = rule.select(segments);
            if (!media.isEmpty()) {
                media(ctx, media, event.userId(), rule);
                return;
            }
        }
        quote(ctx, detail);
    }

    private void media(CmdContext ctx, List<MsgSegment.Media> media, String userId, MediaRule rule) {
        List<File> saved = media.stream()
                .map(item -> fileService.save(userId, rule.type(), item.url(), item.file()))
                .toList();
        saved.forEach(file -> log.info("Content saved: {}", file.getId()));
        if (saved.size() == 1) ctx.send(rule.savedOne(), saved.getFirst().getId());
        else ctx.send(rule.savedMany(), saved.size());
    }

    private void quote(CmdContext ctx, MsgDetail detail) {
        MsgContent content = detail.content();
        if (content.hasRaw())
            throw ctx.warn(Text.UNSUPPORTED_CONTENT);
        String text = text(content, ctx.client());
        if (text.isBlank())
            throw ctx.warn(Text.NO_TEXT_CONTENT);
        if (quoteService.isFormatted(text))
            throw ctx.warn(Text.QUOTE_ALREADY_SAVED);
        Quote quote = quoteService.add(detail.userId(), detail.nickname(), text);
        ctx.send(Text.QUOTE_SAVED, quote.getId());
        log.info("Quote saved: {}", quote.getId());
    }

    private static String text(MsgContent content, BotClient client) {
        StringBuilder text = new StringBuilder();
        for (MsgSegment segment : content.segments()) {
            if (segment instanceof MsgSegment.Text(String s))
                text.append(s);
            else if (segment instanceof MsgSegment.At(String userId))
                text.append(atText(userId, client));
        }
        return text.toString();
    }

    private static String atText(String userId, BotClient client) {
        try {
            return "@" + client.getUserInfo(userId).nickname() + "(" + userId + ")";
        } catch (RuntimeException e) {
            return "@" + userId;
        }
    }

    // ════ Help ════════════════════

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
