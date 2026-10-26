package com.zincoid.gd;

import com.zincoid.gd.GdService.GddlLevel;
import com.zincoid.gd.GdService.GddlUser;
import com.zincoid.gd.GdService.ScoreResult;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.cmd.input.InputException;
import com.zincoid.nullbot.bot.cmd.input.InputMode;
import com.zincoid.nullbot.bot.cmd.input.InputResult;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import jdash.common.DemonDifficulty;
import jdash.common.Difficulty;
import jdash.common.LevelSearchFilter;
import jdash.common.Length;
import jdash.common.entity.GDLevel;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * GD 指令：关卡搜索、关卡详情与 GDDL B20。
 * <p>GD command: level search, level details, and GDDL B20.</p>
 */
@Slf4j
@CmdMapping({"GD", "几何冲刺"})
public class GdCmd implements Cmd {

    private static final long WAIT_SECONDS = 30;
    private static final String WAIT_PATTERN = "\\s*(?:[0-9]+|(?i:end))\\s*";

    private final GdPlugin plugin;

    /**
     * 创建 GD 指令。
     * <p>Creates the GD command.</p>
     *
     * @param plugin the owning plugin
     */
    public GdCmd(GdPlugin plugin) {
        this.plugin = plugin;
    }

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args, event.groupId(), event.userId(), true);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args, event.userId(), event.userId(), false);
    }

    private void handle(CmdContext ctx, CmdArgs args, String targetId, String userId, boolean group) {
        String sub = args.next("").toLowerCase(Locale.ROOT);
        if (sub.isBlank()) throw new CmdInfoException(getHelp(ctx.language()));
        switch (sub) {
            case "search" -> search(ctx, args, targetId, userId, group);
            case "level" -> level(ctx, args);
            case "gddl" -> gddl(ctx, args, userId);
            default -> throw new CmdWarnException(
                    GdText.UNKNOWN_SUBCOMMAND.of(ctx.language(), sub));
        }
    }

    // ════ Search ══════════════════

    private void search(CmdContext ctx, CmdArgs args, String targetId, String userId, boolean group) {
        String query = args.rest("").trim();
        if (query.isEmpty())
            throw new CmdWarnException(GdText.MISSING_KEYWORD.of(ctx.language()));
        LevelSearchFilter filter = filter(ctx, args);
        int page = args.optInt("page", "p", 1);
        if (page < 1)
            throw new CmdWarnException(GdText.PAGE_FROM_ONE.of(ctx.language()));

        List<GDLevel> levels;
        try {
            levels = plugin.service().search(query, filter, page - 1);
        } catch (GdException e) {
            log.warn("GD search failed: query={}", query, e);
            throw new CmdWarnException(GdText.SEARCH_FAILED.of(ctx.language()));
        }
        if (levels.isEmpty())
            throw new CmdInfoException(GdText.NO_LEVELS.of(ctx.language()));
        if (levels.size() == 1) {
            showLevel(ctx, levels.getFirst());
            return;
        }
        SearchView view = new SearchView(
                query, page, GdViewMapper.summaries(levels), WAIT_SECONDS);
        ctx.send(plugin.presenter().search(ctx.language(), view));
        waitAndShow(ctx, levels, targetId, userId, group);
    }

    private LevelSearchFilter filter(CmdContext ctx, CmdArgs args) {
        LevelSearchFilter filter = LevelSearchFilter.create();
        EnumSet<Difficulty> difficulties = EnumSet.noneOf(Difficulty.class);
        String diffValue = args.getOpt("diff", "d", "").trim();
        if (!diffValue.isEmpty())
            for (String part : diffValue.split(",")) difficulties.add(difficulty(ctx, part.trim()));

        DemonDifficulty demon = null;
        String demonValue = args.getOpt("demon", null, "").trim();
        if (!demonValue.isEmpty()) {
            demon = demonDifficulty(ctx, demonValue);
            if (!difficulties.isEmpty()
                    && !(difficulties.size() == 1 && difficulties.contains(Difficulty.AUTO)))
                throw new CmdWarnException(GdText.DEMON_COMBINATION.of(ctx.language()));
            difficulties.add(Difficulty.AUTO);
        }
        if (!difficulties.isEmpty()) filter = filter.withDifficulties(difficulties);
        if (demon != null) filter = filter.withDemonFilter(demon);

        String lengths = args.getOpt("length", "l", "").trim();
        if (!lengths.isEmpty()) {
            EnumSet<Length> values = EnumSet.noneOf(Length.class);
            for (String part : lengths.split(",")) values.add(length(ctx, part.trim()));
            filter = filter.withLengths(values);
        }
        String toggles = args.getOpt("filter", null, "").trim();
        if (!toggles.isEmpty()) {
            EnumSet<LevelSearchFilter.Toggle> values =
                    EnumSet.noneOf(LevelSearchFilter.Toggle.class);
            for (String part : toggles.split(",")) values.add(toggle(ctx, part.trim()));
            filter = filter.withToggles(values);
        }
        return filter;
    }

    private void waitAndShow(CmdContext ctx, List<GDLevel> levels,
                             String targetId, String userId, boolean group) {
        InputMode mode = group ? InputMode.GS : InputMode.PS;
        while (true) {
            List<InputResult> inputs;
            try {
                inputs = plugin.context().inputs()
                        .request(mode, targetId, WAIT_PATTERN, WAIT_SECONDS);
            } catch (InputException e) {
                throw new CmdWarnException(GdText.WAIT_CONFLICT.of(ctx.language()));
            }
            if (inputs.isEmpty())
                throw new CmdInfoException(GdText.SELECT_TIMEOUT.of(ctx.language()));
            InputResult input = inputs.getFirst();
            if (group && !userId.equals(input.userId())) {
                ctx.send(GdText.INVOKER_ONLY.of(ctx.language()));
                continue;
            }
            String value = input.message().trim();
            if (value.equalsIgnoreCase("end"))
                throw new CmdInfoException(GdText.CANCELLED.of(ctx.language()));
            int index;
            try {
                index = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                continue;
            }
            if (index < 1 || index > levels.size()) {
                ctx.send(GdText.INDEX_OUT_OF_RANGE.of(ctx.language()));
                continue;
            }
            showLevel(ctx, levels.get(index - 1));
            return;
        }
    }

    // ════ Level ═══════════════════

    private void level(CmdContext ctx, CmdArgs args) {
        long id = levelId(ctx, args);
        GDLevel level;
        try {
            level = plugin.service().level(id);
        } catch (GdException e) {
            log.warn("GD level query failed: id={}", id, e);
            throw new CmdWarnException(GdText.SEARCH_FAILED.of(ctx.language()));
        }
        if (level == null)
            throw new CmdInfoException(GdText.LEVEL_NOT_FOUND.of(ctx.language()));
        showLevel(ctx, level);
    }

    private void showLevel(CmdContext ctx, GDLevel level) {
        LevelExtras extras = loadExtras(level.id());
        LevelView view = GdViewMapper.level(
                level, extras.gddl(), extras.gddlFailed(), extras.thumbnail());
        ctx.send(plugin.presenter().level(ctx.language(), view));
        log.info("GD level sent: id={}, tier={}, thumbnail={}",
                level.id(), view.tier(), extras.thumbnail() != null);
    }

    private LevelExtras loadExtras(long id) {
        GddlResult gddl = new GddlResult(null, true);
        byte[] thumbnail = null;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var gddlFuture = CompletableFuture.supplyAsync(() -> loadGddl(id), executor);
            var thumbnailFuture = CompletableFuture.supplyAsync(() -> loadThumbnail(id), executor);
            gddl = gddlFuture.join();
            thumbnail = thumbnailFuture.join();
        } catch (RuntimeException e) {
            log.warn("GD level extras failed: id={}", id, e);
        }
        return new LevelExtras(gddl.level(), gddl.failed(), thumbnail);
    }

    private GddlResult loadGddl(long id) {
        try {
            return new GddlResult(plugin.service().gddlLevel(id), false);
        } catch (GdException e) {
            log.warn("GDDL level query failed: id={}", id, e);
            return new GddlResult(null, true);
        }
    }

    private byte[] loadThumbnail(long id) {
        try {
            return plugin.service().thumbnail(id);
        } catch (GdException e) {
            log.warn("GD thumbnail fetch failed: id={}", id, e);
            return null;
        }
    }

    private record GddlResult(GddlLevel level, boolean failed) {
    }

    private record LevelExtras(GddlLevel gddl, boolean gddlFailed, byte[] thumbnail) {
    }

    private static long levelId(CmdContext ctx, CmdArgs args) {
        String raw = args.next("").trim();
        if (raw.isEmpty())
            throw new CmdWarnException(GdText.MISSING_LEVEL_ID.of(ctx.language()));
        try {
            long id = Long.parseLong(raw);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new CmdWarnException(GdText.INVALID_LEVEL_ID.of(ctx.language(), raw));
        }
    }

    // ════ GDDL ════════════════════

    private void gddl(CmdContext ctx, CmdArgs args, String userId) {
        boolean bind = args.hasOpt("bind", "b");
        boolean unbind = args.hasOpt("unbind", "u");
        if (bind && unbind)
            throw new CmdWarnException(GdText.BIND_CONFLICT.of(ctx.language()));

        boolean all = args.hasOpt("all", "a");
        if (bind) {
            bindUser(ctx, userId, bindName(ctx, args));
            return;
        }
        if (unbind) {
            unbindUser(ctx, userId);
            return;
        }
        String raw = args.rest("").trim();
        if (raw.isEmpty()) {
            GdBindings.Binding binding = plugin.bindings().get(userId);
            if (binding == null)
                throw new CmdInfoException(GdText.NO_BINDING.of(ctx.language()));
            showRank(ctx, new GddlUser(binding.id(), binding.name()), all);
            return;
        }
        showRank(ctx, resolveUser(ctx, raw), all);
    }

    private static String bindName(CmdContext ctx, CmdArgs args) {
        String value = args.getOpt("bind", "b", "").trim();
        String rest = args.rest("").trim();
        if (value.isEmpty()) value = rest;
        else if (!rest.isEmpty()) value = value + " " + rest;
        if (value.isEmpty())
            throw new CmdWarnException(GdText.MISSING_USER.of(ctx.language()));
        return value;
    }

    private void bindUser(CmdContext ctx, String userId, String raw) {
        GdBindings.Binding current = plugin.bindings().get(userId);
        if (current != null)
            throw new CmdInfoException(GdText.BIND_ALREADY.of(
                    ctx.language(), current.name(), current.id()));
        GddlUser user = resolveUser(ctx, raw);
        GdBindings.Binding existing;
        try {
            existing = plugin.bindings().bind(userId, user.id(), user.name());
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
            existing = plugin.bindings().unbind(userId);
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

    private void showRank(CmdContext ctx, GddlUser user, boolean all) {
        ScoreResult score;
        try {
            score = plugin.service().userScore(user.id(), user.name());
        } catch (GdException e) {
            log.warn("GDDL score query failed: user={}", user.id(), e);
            throw new CmdWarnException(GdText.GDDL_FAILED.of(ctx.language()));
        }
        if (score.b20().sample() == 0)
            throw new CmdInfoException(GdText.NO_RECORDS.of(ctx.language()));
        RankView view = GdViewMapper.rank(score, all);
        ctx.send(plugin.presenter().rank(ctx.language(), view));
        log.info("GD B20 sent: user={}, sample={}, score={}",
                user.id(), view.sample(), String.format(Locale.ROOT, "%.2f", view.score()));
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
                    user = plugin.service().user(id);
                } catch (GdException e) {
                    log.warn("GDDL user query failed: id={}", id, e);
                    throw new CmdWarnException(GdText.GDDL_FAILED.of(ctx.language()));
                }
                if (user != null) return user;
            }
        }
        List<GddlUser> users;
        try {
            users = plugin.service().searchUsers(raw);
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

    // ════ Helpers ═════════════════

    private static Difficulty difficulty(CmdContext ctx, String value) {
        // JDash's AUTO(-2) is the GD search marker for demon levels; DEMON(-3) is rejected by the server.
        if ("demon".equalsIgnoreCase(value)) return Difficulty.AUTO;
        if ("auto".equalsIgnoreCase(value))
            throw new CmdWarnException(GdText.AUTO_UNSUPPORTED.of(ctx.language()));
        try {
            return Difficulty.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CmdWarnException(GdText.INVALID_DIFFICULTY.of(ctx.language(), value));
        }
    }

    private static DemonDifficulty demonDifficulty(CmdContext ctx, String value) {
        try {
            return DemonDifficulty.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CmdWarnException(GdText.INVALID_DEMON.of(ctx.language(), value));
        }
    }

    private static Length length(CmdContext ctx, String value) {
        try {
            return Length.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CmdWarnException(GdText.INVALID_LENGTH.of(ctx.language(), value));
        }
    }

    private static LevelSearchFilter.Toggle toggle(CmdContext ctx, String value) {
        String name = value.toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return LevelSearchFilter.Toggle.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new CmdWarnException(GdText.INVALID_FILTER.of(ctx.language(), value));
        }
    }

    // ════ Help ════════════════════

    @Override
    public String getHelp(Language language) {
        return GdText.HELP.of(language);
    }
}
