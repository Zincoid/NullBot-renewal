package com.zincoid.gd.model;

import com.zincoid.gd.service.GdLocalService.LocalB20Result;
import com.zincoid.gd.service.GdScore;
import com.zincoid.gd.service.GdService.GddlLevel;
import com.zincoid.gd.service.GdService.ScoreResult;
import jdash.common.DemonDifficulty;
import jdash.common.Difficulty;
import jdash.common.Length;
import jdash.common.entity.GDLevel;

import java.util.List;
import java.util.Locale;

/**
 * 视图映射：把 JDash/GDDL 数据转换为与呈现方式无关的视图。
 * <p>View mapping: converts JDash/GDDL data into presentation-independent views.</p>
 */
public final class GdViewMapper {

    private GdViewMapper() {
    }

    /**
     * 把搜索结果映射为列表摘要。
     * <p>Maps search results to list summaries.</p>
     *
     * @param levels the levels
     * @return the summaries
     */
    public static List<SearchView.LevelSummary> summaries(List<GDLevel> levels) {
        return levels.stream()
                .map(level -> new SearchView.LevelSummary(
                        level.id(),
                        level.name(),
                        difficultyLabel(level),
                        level.creatorName().orElse("-"),
                        level.rewards()
                ))
                .toList();
    }

    /**
     * 把关卡与 GDDL 数据映射为详情视图。
     * <p>Maps a level and its GDDL data to a detail view.</p>
     *
     * @param level      the level
     * @param gddl       the GDDL level, nullable
     * @param gddlFailed whether the GDDL query failed
     * @param thumbnail  the thumbnail bytes, nullable
     * @return the level view
     */
    public static LevelView level(GDLevel level, GddlLevel gddl, boolean gddlFailed, byte[] thumbnail) {
        String song = level.song()
                .map(value -> safe(value.title()) + " — " + safe(value.artist()))
                .orElse(null);
        return new LevelView(
                level.id(),
                level.name(),
                level.creatorName().orElse("-"),
                difficultyLabel(level),
                level.rewards(),
                level.downloads(),
                level.likes(),
                lengthLabel(level.length()),
                level.objectCount(),
                level.songId().orElseGet(() -> level.song()
                        .map(jdash.common.entity.GDSong::id).orElse(null)),
                song,
                level.description(),
                tier(gddl),
                gddl == null ? null : gddl.rating(),
                gddl == null ? null : gddl.enjoyment(),
                gddl == null ? null : gddl.ratingCount(),
                gddl == null ? null : gddl.aredlPosition(),
                gddlFailed,
                thumbnail
        );
    }

    /**
     * 把 GDDL B20 结果映射为排行视图。
     * <p>Maps a GDDL B20 result to the rank view.</p>
     *
     * @param score  the score result
     * @param all    whether to display the full B20 list
     * @param avatar the GDDL avatar image bytes, nullable
     * @return the GDDL B20 view
     */
    public static GddlB20View gddlB20(ScoreResult score, boolean all, byte[] avatar) {
        List<GddlB20View.Play> top = score.b20().top().stream()
                .map(scored -> new GddlB20View.Play(
                        scored.play().levelId(),
                        scored.play().name(),
                        scored.play().tier(),
                        scored.play().progress(),
                        scored.rt()
                ))
                .toList();
        int limit = all ? top.size() : Math.min(5, top.size());
        return new GddlB20View(
                score.userId(),
                score.userName(),
                score.beatenTotal(),
                score.beatingTotal(),
                score.b20().score(),
                score.highestTier(),
                score.b20().sample(),
                top,
                limit,
                avatar
        );
    }

    /**
     * 把本地 B20 结果映射为本地排行视图。
     * <p>Maps a local B20 result to the local rank view.</p>
     *
     * @param result   the local B20 result
     * @param all      whether to display the full B20 list
     * @param avatar   the chat user avatar bytes, nullable
     * @param userName the chat user name
     * @return the local rank view
     */
    public static LocalB20View localB20(LocalB20Result result, boolean all,
                                        byte[] avatar, String userName) {
        List<LocalB20View.Play> top = result.top().stream()
                .map(scored -> new LocalB20View.Play(
                        scored.play().levelId(),
                        scored.play().name(),
                        scored.play().tier(),
                        scored.play().progress(),
                        scored.rt()
                ))
                .toList();
        int limit = all ? top.size() : Math.min(5, top.size());
        return new LocalB20View(
                result.userId(),
                userName,
                avatar,
                result.beatenTotal(),
                result.beatingTotal(),
                result.ratedTotal(),
                result.unratedTotal(),
                result.score(),
                result.highestTier(),
                result.sample(),
                top,
                limit
        );
    }

    // ════ Helpers ═════════════════

    private static String difficultyLabel(GDLevel level) {
        if (level.isDemon()) {
            DemonDifficulty demon = level.demonDifficulty();
            return demon == null ? "Demon" : title(demon.name()) + " Demon";
        }
        if (level.isAuto()) return "Auto";
        Difficulty difficulty = level.votedDifficulty();
        if (difficulty == null || difficulty == Difficulty.NA) return "-";
        return title(difficulty.name());
    }

    private static String lengthLabel(Length length) {
        if (length == null) return "-";
        return switch (length) {
            case TINY -> "Tiny";
            case SHORT -> "Short";
            case MEDIUM -> "Medium";
            case LONG -> "Long";
            case XL -> "XL";
            case PLATFORMER -> "Platformer";
        };
    }

    private static Integer tier(GddlLevel level) {
        if (level == null || level.rating() == null) return null;
        double rating = level.rating();
        return Double.isNaN(rating) ? null : Math.max(1, (int) Math.round(rating));
    }

    private static String title(String value) {
        if (value == null || value.isEmpty()) return "-";
        return value.charAt(0) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "-" : value;
    }
}
