package com.zincoid.gd.cmd;

import com.zincoid.gd.GdException;
import com.zincoid.gd.GdText;
import com.zincoid.gd.model.GddlB20View;
import com.zincoid.gd.model.GdViewMapper;
import com.zincoid.gd.render.GdPresenter;
import com.zincoid.gd.service.GddlService;
import com.zincoid.gd.service.GddlService.GddlB20Result;
import com.zincoid.gd.service.GddlService.GddlUser;
import com.zincoid.gd.store.GdBindings;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * GDDL 指令：B20 评分与账号绑定。
 * <p>GDDL command: B20 ratings and account bindings.</p>
 */
@Slf4j
@CmdMapping("GDDL")
@RequiredArgsConstructor
public class GddlCmd implements Cmd {

    private final GddlService gddl;
    private final GdPresenter presenter;
    private final GdBindings bindings;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        gddl(ctx, args, event.userId());
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        gddl(ctx, args, event.userId());
    }

    private void gddl(CmdContext ctx, CmdArgs args, String userId) {
        String sub = args.next("").toLowerCase(Locale.ROOT);
        if (sub.isBlank()) throw new CmdInfoException(getHelp(ctx.language()));
        switch (sub) {
            case "bind" -> bindUser(ctx, userId, bindName(ctx, args));
            case "unbind" -> unbindUser(ctx, userId);
            case "b20" -> b20(ctx, args, userId);
            default -> throw new CmdWarnException(
                    GdText.UNKNOWN_SUBCOMMAND.of(ctx.language(), sub));
        }
    }

    private static String bindName(CmdContext ctx, CmdArgs args) {
        String value = args.rest("").trim();
        if (value.isEmpty())
            throw new CmdWarnException(GdText.MISSING_USER.of(ctx.language()));
        return value;
    }

    private void b20(CmdContext ctx, CmdArgs args, String userId) {
        boolean all = args.hasOpt("all", "a");
        String raw = args.rest("").trim();
        if (raw.isEmpty()) {
            GdBindings.Binding binding = bindings.get(userId);
            if (binding == null)
                throw new CmdInfoException(GdText.NO_BINDING.of(ctx.language()));
            showB20(ctx, new GddlUser(binding.id(), binding.name(), binding.avatar()), all);
            return;
        }
        showB20(ctx, resolveUser(ctx, raw), all);
    }

    private void bindUser(CmdContext ctx, String userId, String raw) {
        GdBindings.Binding current = bindings.get(userId);
        if (current != null)
            throw new CmdInfoException(GdText.BIND_ALREADY.of(
                    ctx.language(), current.name(), current.id()));
        GddlUser user = resolveUser(ctx, raw);
        GdBindings.Binding existing;
        try {
            existing = bindings.bind(userId, user.id(), user.name(), user.avatar());
        } catch (GdException e) {
            log.warn("GDDL binding save failed: user={}", userId, e);
            throw new CmdWarnException(GdText.BIND_SAVE_FAILED.of(ctx.language()));
        }
        if (existing != null)
            throw new CmdInfoException(GdText.BIND_ALREADY.of(
                    ctx.language(), existing.name(), existing.id()));
        log.info("GDDL binding saved: user={}, gddl={} ({})", userId, user.id(), user.name());
        throw new CmdInfoException(GdText.BIND_SUCCESS.of(
                ctx.language(), user.name(), user.id()));
    }

    private void unbindUser(CmdContext ctx, String userId) {
        GdBindings.Binding existing;
        try {
            existing = bindings.unbind(userId);
        } catch (GdException e) {
            log.warn("GDDL binding removal failed: user={}", userId, e);
            throw new CmdWarnException(GdText.BIND_SAVE_FAILED.of(ctx.language()));
        }
        if (existing == null)
            throw new CmdInfoException(GdText.UNBIND_NONE.of(ctx.language()));
        log.info("GDDL binding removed: user={}, gddl={} ({})",
                userId, existing.id(), existing.name());
        throw new CmdInfoException(GdText.UNBIND_SUCCESS.of(
                ctx.language(), existing.name(), existing.id()));
    }

    private void showB20(CmdContext ctx, GddlUser user, boolean all) {
        GddlUser current = user;
        if (current.avatar() == null || current.avatar().isBlank()) {
            try {
                GddlUser loaded = gddl.user(current.id());
                if (loaded != null) current = loaded;
            } catch (GdException e) {
                log.warn("GDDL user refresh failed: user={}", current.id(), e);
            }
        }
        GddlB20Result score;
        try {
            score = gddl.gddlB20(current.id(), current.name());
        } catch (GdException e) {
            log.warn("GDDL score query failed: user={}", current.id(), e);
            throw new CmdWarnException(GdText.GDDL_FAILED.of(ctx.language()));
        }
        if (score.b20().sample() == 0)
            throw new CmdInfoException(GdText.NO_RECORDS.of(ctx.language()));
        byte[] avatar = gddl.avatar(current.id(), current.avatar());
        GddlB20View view = GdViewMapper.gddlB20(score, all, avatar);
        ctx.send(presenter.gddlB20(ctx.language(), view));
        log.info("GD B20 sent: user={}, sample={}, score={}",
                current.id(), view.sample(), String.format(Locale.ROOT, "%.2f", view.score()));
    }

    private GddlUser resolveUser(CmdContext ctx, String raw) {
        if (raw.chars().allMatch(Character::isDigit)) {
            long id;
            try {
                id = Long.parseLong(raw);
            } catch (NumberFormatException e) {
                id = -1;
            }
            if (id > 0) {
                GddlUser user;
                try {
                    user = gddl.user(id);
                } catch (GdException e) {
                    log.warn("GDDL user query failed: id={}", id, e);
                    throw new CmdWarnException(GdText.GDDL_FAILED.of(ctx.language()));
                }
                if (user != null) return user;
            }
        }
        List<GddlUser> users;
        try {
            users = gddl.searchUsers(raw);
        } catch (GdException e) {
            log.warn("GDDL user search failed: name={}", raw, e);
            throw new CmdWarnException(GdText.GDDL_FAILED.of(ctx.language()));
        }
        if (users.isEmpty())
            throw new CmdInfoException(GdText.GDDL_USER_NOT_FOUND.of(ctx.language(), raw));
        GddlUser exact = users.stream()
                .filter(user -> user.name().equalsIgnoreCase(raw))
                .findFirst()
                .orElse(null);
        if (exact != null) return exact;
        if (users.size() == 1) return users.getFirst();
        String candidates = users.stream()
                .limit(5)
                .map(user -> user.name() + " (" + user.id() + ")")
                .collect(Collectors.joining(GdText.LIST_SEPARATOR.of(ctx.language())));
        throw new CmdInfoException(GdText.MULTIPLE_USERS.of(ctx.language(), candidates));
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return GdText.GDDL_HELP.of(language);
    }
}
