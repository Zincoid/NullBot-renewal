package com.zincoid.gd.cmd;

import com.zincoid.gd.GdException;
import com.zincoid.gd.GdText;
import com.zincoid.gd.model.GdViewMapper;
import com.zincoid.gd.model.LevelView;
import com.zincoid.gd.model.LocalB20View;
import com.zincoid.gd.model.SearchView;
import com.zincoid.gd.render.GdPresenter;
import com.zincoid.gd.service.GddlService;
import com.zincoid.gd.service.GddlService.GddlLevel;
import com.zincoid.gd.service.GdLevelService;
import com.zincoid.gd.service.GdProgressService;
import com.zincoid.gd.service.GdProgressService.LocalB20Result;
import com.zincoid.gd.service.GdProgressService.UpdateResult;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.cmd.input.InputException;
import com.zincoid.nullbot.bot.cmd.input.InputMode;
import com.zincoid.nullbot.bot.cmd.input.InputResult;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import jdash.common.DemonDifficulty;
import jdash.common.Difficulty;
import jdash.common.Length;
import jdash.common.LevelSearchFilter;
import jdash.common.entity.GDLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * GD 指令：关卡搜索与详情。
 * <p>GD command: level search and details.</p>
 */
@Slf4j
@CmdMapping({"GD", "几何冲刺"})
@RequiredArgsConstructor
public class GdCmd implements Cmd {

    private static final long WAIT_SECONDS = 30;
    private static final String WAIT_PATTERN = "\\s*(?:[0-9]+|(?i:end|next))\\s*";
    private static final String CONFIRM_PATTERN = "\\s*(?i:yes|y|是|确认|确定|no|n|否|取消)\\s*";

    private final GdLevelService levels;
    private final GddlService gddl;
    private final GdPresenter presenter;
    private final PluginContext context;
    private final GdProgressService progress;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args, event.groupId(), event.userId(), true, event.nickname());
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args, event.userId(), event.userId(), false, event.nickname());
    }

    private void handle(CmdContext ctx, CmdArgs args, String targetId, String userId,
                        boolean group, String nickname) {
        String sub = args.next("").toLowerCase(Locale.ROOT);
        if (sub.isBlank()) throw new CmdInfoException(getHelp(ctx.language()));
        switch (sub) {
            case "search" -> search(ctx, args, targetId, userId, group);
            case "level" -> level(ctx, args);
            case "gddl" -> throw new CmdWarnException(GdText.GDDL_SPLIT.of(ctx.language()));
            case "register" -> register(ctx, userId);
            case "update" -> update(ctx, args, userId);
            case "remove" -> remove(ctx, args, userId);
            case "unregister" -> unregister(ctx, targetId, userId, group);
            case "b20" -> b20(ctx, args, userId, nickname);
            default -> throw new CmdWarnException(
                    GdText.UNKNOWN_SUBCOMMAND.of(ctx.language(), sub));
        }
    }

    // ════ Local progress ══════════

    private void register(CmdContext ctx, String userId) {
        boolean created;
        try {
            created = progress.register(userId);
        } catch (GdException e) {
            log.warn("GD register failed: user={}", userId, e);
            throw new CmdWarnException(GdText.STORAGE_FAILED.of(ctx.language()));
        }
        if (created) {
            ctx.send(GdText.REGISTER_SUCCESS.of(ctx.language()));
            return;
        }
        throw new CmdInfoException(GdText.REGISTER_ALREADY.of(ctx.language()));
    }

    private void update(CmdContext ctx, CmdArgs args, String userId) {
        long id = levelId(ctx, args);
        int percent = parseProgress(ctx, args);
        requireRegistered(ctx, userId);
        Optional<UpdateResult> result;
        try {
            result = progress.update(userId, id, percent);
        } catch (GdException e) {
            log.warn("GD progress update failed: id={}", id, e);
            throw new CmdWarnException(GdText.SEARCH_FAILED.of(ctx.language()));
        }
        if (result.isEmpty())
            throw new CmdInfoException(GdText.LEVEL_NOT_FOUND.of(ctx.language()));
        UpdateResult value = result.get();
        String author = value.author() == null || value.author().isBlank()
                ? "-" : value.author();
        ctx.send(GdText.UPDATE_SUCCESS.of(
                ctx.language(), value.levelName(), author, id, percent));
    }

    private void remove(CmdContext ctx, CmdArgs args, String userId) {
        long id = levelId(ctx, args);
        requireRegistered(ctx, userId);
        boolean removed;
        try {
            removed = progress.remove(userId, id);
        } catch (GdException e) {
            log.warn("GD progress removal failed: id={}", id, e);
            throw new CmdWarnException(GdText.STORAGE_FAILED.of(ctx.language()));
        }
        if (!removed)
            throw new CmdInfoException(GdText.REMOVE_NONE.of(ctx.language()));
        ctx.send(GdText.REMOVE_SUCCESS.of(ctx.language(), id));
    }

    private void unregister(CmdContext ctx, String targetId, String userId, boolean group) {
        requireRegistered(ctx, userId);
        int count;
        try {
            count = progress.progressCount(userId);
        } catch (GdException e) {
            log.warn("GD progress count failed: user={}", userId, e);
            throw new CmdWarnException(GdText.STORAGE_FAILED.of(ctx.language()));
        }
        if (count > 0) {
            ctx.send(GdText.UNREGISTER_CONFIRM.of(
                    ctx.language(), count, WAIT_SECONDS));
            InputMode mode = group ? InputMode.GS : InputMode.PS;
            InputResult input = confirm(ctx, mode, targetId, userId, group);
            if (input == null || !confirmed(input.message()))
                throw new CmdInfoException(GdText.UNREGISTER_CANCELLED.of(ctx.language()));
        }
        int removed;
        try {
            removed = progress.unregister(userId);
        } catch (GdException e) {
            log.warn("GD unregister failed: user={}", userId, e);
            throw new CmdWarnException(GdText.STORAGE_FAILED.of(ctx.language()));
        }
        ctx.send(GdText.UNREGISTER_SUCCESS.of(ctx.language(), removed));
    }

    private InputResult confirm(CmdContext ctx, InputMode mode, String targetId,
                                String userId, boolean group) {
        while (true) {
            List<InputResult> inputs;
            try {
                inputs = context.inputs().request(mode, targetId, CONFIRM_PATTERN, WAIT_SECONDS);
            } catch (InputException e) {
                throw new CmdWarnException(GdText.WAIT_CONFLICT.of(ctx.language()));
            }
            if (inputs.isEmpty())
                throw new CmdInfoException(GdText.UNREGISTER_CANCELLED.of(ctx.language()));
            InputResult input = inputs.getFirst();
            if (group && !userId.equals(input.userId())) {
                ctx.send(GdText.INVOKER_ONLY.of(ctx.language()));
                continue;
            }
            return input;
        }
    }

    private static boolean confirmed(String value) {
        String text = value.trim().toLowerCase(Locale.ROOT);
        return text.equals("yes") || text.equals("y") || text.equals("是")
                || text.equals("确认") || text.equals("确定");
    }

    private void b20(CmdContext ctx, CmdArgs args, String userId, String nickname) {
        requireRegistered(ctx, userId);
        boolean all = args.hasOpt("all", "a");
        LocalB20Result result;
        try {
            result = progress.localB20(userId);
        } catch (GdException e) {
            log.warn("GD local B20 failed: user={}", userId, e);
            throw new CmdWarnException(GdText.LOCAL_B20_FAILED.of(ctx.language()));
        }
        if (result.ratedTotal() == 0) {
            if (result.unratedTotal() == 0)
                throw new CmdInfoException(GdText.NO_LOCAL_RECORDS.of(ctx.language()));
            throw new CmdInfoException(GdText.NO_RATED_RECORDS.of(ctx.language()));
        }

        String userName = nickname;
        byte[] avatar = null;
        try {
            UserInfo info = ctx.client().getUserInfo(userId);
            if (info != null) {
                if (info.nickname() != null && !info.nickname().isBlank())
                    userName = info.nickname();
                avatar = progress.avatar(info.avatar());
            }
        } catch (RuntimeException e) {
            log.warn("GD user profile fetch failed: user={}", userId, e);
        }
        if (userName == null || userName.isBlank()) userName = userId;

        LocalB20View view = GdViewMapper.localB20(result, all, avatar, userName);
        ctx.send(presenter.localB20(ctx.language(), view));
        log.info("GD local B20 sent: user={}, sample={}, score={}",
                userId, view.sample(), String.format(Locale.ROOT, "%.2f", view.score()));
    }

    private void requireRegistered(CmdContext ctx, String userId) {
        boolean registered;
        try {
            registered = progress.isRegistered(userId);
        } catch (GdException e) {
            log.warn("GD registration check failed: user={}", userId, e);
            throw new CmdWarnException(GdText.STORAGE_FAILED.of(ctx.language()));
        }
        if (!registered)
            throw new CmdInfoException(GdText.NOT_REGISTERED.of(ctx.language()));
    }

    private static int parseProgress(CmdContext ctx, CmdArgs args) {
        String raw = args.next("").trim();
        if (raw.isEmpty())
            throw new CmdWarnException(GdText.MISSING_PROGRESS.of(ctx.language()));
        if (raw.endsWith("%")) raw = raw.substring(0, raw.length() - 1).trim();
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new CmdWarnException(GdText.INVALID_PROGRESS.of(ctx.language()));
        }
        if (value < 1 || value > 100)
            throw new CmdWarnException(GdText.INVALID_PROGRESS.of(ctx.language()));
        return value;
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

        List<GDLevel> found;
        try {
            found = levels.search(query, filter, page - 1);
        } catch (GdException e) {
            log.warn("GD search failed: query={}", query, e);
            throw new CmdWarnException(GdText.SEARCH_FAILED.of(ctx.language()));
        }
        if (found.isEmpty())
            throw new CmdInfoException(GdText.NO_LEVELS.of(ctx.language()));
        if (found.size() == 1) {
            showLevel(ctx, found.getFirst());
            return;
        }
        SearchView view = new SearchView(
                query, page, GdViewMapper.summaries(found), WAIT_SECONDS);
        ctx.send(presenter.search(ctx.language(), view));
        waitAndShow(ctx, query, filter, page, found, targetId, userId, group);
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

    private void waitAndShow(CmdContext ctx, String query, LevelSearchFilter filter,
                             int page, List<GDLevel> initialLevels,
                             String targetId, String userId, boolean group) {
        InputMode mode = group ? InputMode.GS : InputMode.PS;
        int currentPage = page;
        List<GDLevel> currentLevels = initialLevels;
        while (true) {
            List<InputResult> inputs;
            try {
                inputs = context.inputs()
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
            if (value.equalsIgnoreCase("next")) {
                try {
                    List<GDLevel> next = levels.search(query, filter, currentPage);
                    if (next.isEmpty()) {
                        ctx.send(GdText.PAGE_LAST.of(ctx.language()));
                        continue;
                    }
                    currentPage++;
                    if (next.size() == 1) {
                        showLevel(ctx, next.getFirst());
                        return;
                    }
                    currentLevels = next;
                    SearchView view = new SearchView(
                            query, currentPage, GdViewMapper.summaries(next), WAIT_SECONDS);
                    ctx.send(presenter.search(ctx.language(), view));
                    continue;
                } catch (GdException e) {
                    log.warn("GD search page failed: query={}, page={}",
                            query, currentPage + 1, e);
                    ctx.send(GdText.SEARCH_FAILED.of(ctx.language()));
                    continue;
                }
            }
            int index;
            try {
                index = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                continue;
            }
            if (index < 1 || index > currentLevels.size()) {
                ctx.send(GdText.INDEX_OUT_OF_RANGE.of(ctx.language()));
                continue;
            }
            showLevel(ctx, currentLevels.get(index - 1));
            return;
        }
    }

    // ════ Level ═══════════════════

    private void level(CmdContext ctx, CmdArgs args) {
        long id = levelId(ctx, args);
        GDLevel level;
        try {
            level = levels.level(id);
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
        ctx.send(presenter.level(ctx.language(), view));
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
            return new GddlResult(gddl.gddlLevel(id), false);
        } catch (GdException e) {
            log.warn("GDDL level query failed: id={}", id, e);
            return new GddlResult(null, true);
        }
    }

    private byte[] loadThumbnail(long id) {
        try {
            return levels.thumbnail(id);
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

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return GdText.HELP.of(language);
    }
}
