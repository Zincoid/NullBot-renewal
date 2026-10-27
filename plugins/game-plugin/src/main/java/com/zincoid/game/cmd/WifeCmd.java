package com.zincoid.game.cmd;

import com.zincoid.game.GameText;
import com.zincoid.game.service.GameResources;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 今日老婆指令：随机抽选群友或资源目录中的二次元老婆，两类每日各一次。
 * <p>Wife command: draws a random group member or an anime wife from the resource directory, once per day for each kind.</p>
 */
@Slf4j
@CmdMapping({"Wife", "今日老婆"})
@RequiredArgsConstructor
public class WifeCmd implements Cmd {

    private final PluginContext context;
    private final GameResources resources;

    private final Map<String, Pick> memberPicks = new ConcurrentHashMap<>();
    private final Map<String, Pick> animePicks = new ConcurrentHashMap<>();

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        String userId = event.userId();
        if (args.hasOpt("member", "m")) {
            requireManager(ctx, userId);
            setMember(ctx, event, optValue(ctx, args, "member", "m"));
            return;
        }
        if (args.hasOpt("anime", "a")) {
            requireManager(ctx, userId);
            setAnime(ctx, event, optValue(ctx, args, "anime", "a"));
            return;
        }
        if (args.hasNext()) randomAnime(ctx, event, args.rest().trim());
        else randomMember(ctx, event);
    }

    // ════ Member wife ═════════════

    private void randomMember(CmdContext ctx, GroupMsgEvent event) {
        String userId = event.userId();
        Pick pick = memberPicks.get(userId);
        if (today(pick)) {
            sendMember(ctx, event, pick, true);
            return;
        }
        List<UserInfo> members = ctx.client().getGroupMembers(event.groupId()).stream()
                .filter(member -> !member.userId().equals(userId))
                .filter(member -> !member.userId().equals(event.selfId()))
                .toList();
        if (members.isEmpty())
            throw new CmdInfoException(GameText.WIFE_NO_MEMBERS.of(ctx.language()));
        setMember(ctx, event, members.get(
                ThreadLocalRandom.current().nextInt(members.size())).userId());
    }

    private void setMember(CmdContext ctx, GroupMsgEvent event, String wifeId) {
        UserInfo wife = ctx.client().getUserInfo(wifeId);
        Pick pick = new Pick(wife.userId(), wife.nickname(), wife.avatar(), null, LocalDate.now());
        memberPicks.put(event.userId(), pick);
        sendMember(ctx, event, pick, false);
        log.info("Member wife picked: {} -> {}", event.userId(), pick.id());
    }

    private void sendMember(CmdContext ctx, GroupMsgEvent event, Pick pick, boolean picked) {
        MsgContent.Builder builder = MsgContent.builder()
                .at(event.userId())
                .text((picked ? GameText.WIFE_MEMBER_ALREADY : GameText.WIFE_MEMBER_PICKED)
                        .of(ctx.language(), pick.name(), pick.id()));
        if (pick.image() != null) builder.image(pick.image());
        ctx.send(builder.build());
    }

    // ════ Anime wife ══════════════

    private void randomAnime(CmdContext ctx, GroupMsgEvent event, String keyword) {
        String userId = event.userId();
        Pick pick = animePicks.get(userId);
        if (today(pick)) {
            sendAnime(ctx, event, pick, true);
            return;
        }
        setAnime(ctx, event, keyword);
    }

    private void setAnime(CmdContext ctx, GroupMsgEvent event, String keyword) {
        if (keyword.isBlank())
            throw new CmdWarnException(ctx.render(Text.ARG_MALFORMED));
        GameResources.Resource wife = resources.random(keyword);
        if (wife == null)
            throw new CmdInfoException(GameText.WIFE_NO_RESULT.of(ctx.language()));
        Pick pick = new Pick(wife.path().toString(), wife.character(),
                wife.path().toUri().toString(), wife.category(), LocalDate.now());
        animePicks.put(event.userId(), pick);
        sendAnime(ctx, event, pick, false);
        log.info("Anime wife picked: {} -> {}", event.userId(), pick.name());
    }

    private void sendAnime(CmdContext ctx, GroupMsgEvent event, Pick pick, boolean picked) {
        String text = picked
                ? GameText.WIFE_ANIME_ALREADY.of(ctx.language(), pick.name())
                : GameText.WIFE_ANIME_PICKED.of(ctx.language(), pick.category(), pick.name());
        MsgContent.Builder builder = MsgContent.builder()
                .at(event.userId())
                .text(text);
        if (pick.image() != null) builder.image(pick.image());
        ctx.send(builder.build());
    }

    // ════ Helpers ═════════════════

    private void requireManager(CmdContext ctx, String userId) {
        if (context.users().getAccess(userId).getValue() < Access.MANAGER.getValue())
            throw new CmdWarnException(GameText.WIFE_NOT_ALLOWED.of(ctx.language()));
    }

    private static String optValue(CmdContext ctx, CmdArgs args, String name, String alias) {
        String value = args.getOpt(name, alias).trim();
        if (value.isEmpty() || "true".equals(value))
            throw new CmdWarnException(ctx.render(Text.ARG_MALFORMED));
        return value;
    }

    private static boolean today(Pick pick) {
        return pick != null && pick.date().equals(LocalDate.now());
    }

    private record Pick(String id, String name, String image, String category, LocalDate date) {
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return GameText.WIFE_HELP.of(language);
    }
}
