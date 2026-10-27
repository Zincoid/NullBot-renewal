package com.zincoid.gd.render;

import com.zincoid.gd.model.LevelView;
import com.zincoid.gd.model.RankView;
import com.zincoid.gd.model.SearchView;
import com.zincoid.gd.service.GdScore;
import com.zincoid.gd.GdText;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.render.PluginRenderer;
import com.zincoid.nullbot.module.render.image.ImageData;
import com.zincoid.nullbot.module.render.svg.layout.SvgAlign;
import com.zincoid.nullbot.module.render.svg.layout.SvgCell;
import com.zincoid.nullbot.module.render.svg.layout.SvgColumn;
import com.zincoid.nullbot.module.render.svg.layout.SvgLine;
import com.zincoid.nullbot.module.render.svg.layout.SvgTable;
import com.zincoid.nullbot.module.render.svg.layout.SvgText;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GD 图片呈现器：用 JTE/SVG 模板渲染搜索列表、关卡详情与 B20 卡片。
 * <p>GD image presenter: renders search lists, level details, and B20 cards with JTE/SVG templates.</p>
 */
@Slf4j
public final class GdImagePresenter implements GdPresenter {

    private static final double LEVEL_THUMBNAIL_CENTER_Y = 271.5;
    // Nerd Font icons.
    private static final String STAR = "\uF005";
    private static final String ICON_DOWNLOAD = "\uF019";
    private static final String ICON_LIKES = "\uF004";
    private static final String ICON_LENGTH = "\uF017";
    private static final String ICON_OBJECTS = "\uF1B2";

    private final PluginRenderer renderer;
    private final GdPresenter fallback;

    /**
     * 创建图片呈现器。
     * <p>Creates the image presenter.</p>
     *
     * @param renderer the plugin renderer
     * @param fallback the text presenter used when rendering fails
     */
    public GdImagePresenter(PluginRenderer renderer, GdPresenter fallback) {
        this.renderer = renderer;
        this.fallback = fallback;
    }

    // ════ Present ═════════════════

    @Override
    public MsgContent search(Language language, SearchView view) {
        try {
            List<List<String>> rows = new ArrayList<>();
            for (int i = 0; i < view.levels().size(); i++) {
                SearchView.LevelSummary level = view.levels().get(i);
                rows.add(List.of(
                        String.format(Locale.ROOT, "%02d.", i + 1),
                        level.name(),
                        level.difficulty(),
                        level.author(),
                        STAR + " " + level.stars()));
            }
            List<SvgColumn> columns = List.of(
                    new SvgColumn("#", 40, SvgAlign.START),
                    new SvgColumn(GdText.HEAD_LEVEL.of(language), 300, SvgAlign.START),
                    new SvgColumn(GdText.DIFFICULTY.of(language), 180, SvgAlign.START),
                    new SvgColumn(GdText.AUTHOR.of(language), 300, SvgAlign.START),
                    new SvgColumn(GdText.STARS.of(language), 60, SvgAlign.END));
            List<SvgCell> cells = SvgTable.build(
                    40, 158, 192, 34, columns, rows, 14, 16, "#C9CFD8", "#E6E9EE");
            int height = 190 + (rows.size() + 1) * 34 + 60;
            Map<String, Object> model = Map.ofEntries(
                    Map.entry("title", SvgText.escape(GdText.SEARCH_TITLE.of(
                            language, truncate(view.query(), 24), view.page()))),
                    Map.entry("cells", cells),
                    Map.entry("prompt", SvgText.escape(GdText.SEARCH_PROMPT.of(language, view.waitSeconds()))),
                    Map.entry("height", height)
            );
            return image(renderer.template("search.jte", model));
        } catch (RuntimeException e) {
            log.warn("GD search card render failed; falling back to text", e);
            return fallback.search(language, view);
        }
    }

    @Override
    public MsgContent level(Language language, LevelView view) {
        try {
            String meta = GdText.AUTHOR.of(language) + ": " + view.author()
                    + " · " + GdText.DIFFICULTY.of(language) + ": " + view.difficulty()
                    + " · " + STAR + " " + view.stars();
            String stats1 = ICON_DOWNLOAD + " " + pad(number(view.downloads()))
                    + "  " + ICON_LIKES + " " + number(view.likes());
            String stats2 = ICON_LENGTH + " " + pad(view.length())
                    + "  " + ICON_OBJECTS + " " + number(view.objects());
            String description = clean(view.description(), 120);
            if (!description.isEmpty())
                description = GdText.DESCRIPTION.of(language) + ": " + description;
            String song = "";
            if (view.song() != null && !view.song().isBlank()) {
                String prefix = GdText.SONG.of(language) + ": ";
                String suffix = view.songId() == null ? "" : " · ID " + view.songId();
                int titleWidth = (int) Math.max(0,
                        480 - SvgText.width(prefix, 15) - SvgText.width(suffix, 15));
                song = prefix + SvgText.fit(view.song(), titleWidth, 15) + suffix;
            }
            GddlLine gddl = gddlLine(language, view);
            List<SvgLine> textLines = new ArrayList<>();
            textLines.add(new SvgLine(SvgText.escape(stats1), 0, 15, "#E6E9EE"));
            textLines.add(new SvgLine(SvgText.escape(stats2), 30, 15, "#E6E9EE"));
            textLines.add(new SvgLine(SvgText.escape(SvgText.fit(gddl.main(), 480, 15)), 40, 15, "#CDD3DC"));
            if (!gddl.extra().isBlank())
                textLines.add(new SvgLine(SvgText.escape(SvgText.fit(gddl.extra(), 480, 15)), 26, 15, "#C9CFD8"));
            if (!song.isBlank())
                textLines.add(new SvgLine(SvgText.escape(song), 32, 15, "#C9CFD8"));
            if (!description.isBlank())
                textLines.add(new SvgLine(SvgText.escape(SvgText.fit(description, 480, 15)), 30, 15, "#C9CFD8"));
            Map<String, Object> model = Map.ofEntries(
                    Map.entry("title", SvgText.escape(SvgText.fit(view.name(), 760, 32))),
                    Map.entry("levelId", String.valueOf(view.id())),
                    Map.entry("meta", SvgText.escape(SvgText.fit(meta, 870, 15))),
                    Map.entry("lines", SvgText.center(textLines, LEVEL_THUMBNAIL_CENTER_Y)),
                    Map.entry("thumbnail", ImageData.cover(view.thumbnail(), 720, 405)),
                    Map.entry("noThumbnail", SvgText.escape(GdText.NO_THUMBNAIL.of(language))),
                    Map.entry("height", 450)
            );
            return image(renderer.template("level.jte", model));
        } catch (RuntimeException e) {
            log.warn("GD level card render failed; falling back to text", e);
            return fallback.level(language, view);
        }
    }

    @Override
    public MsgContent rank(Language language, RankView view) {
        try {
            List<List<String>> rows = new ArrayList<>();
            int limit = Math.min(view.limit(), view.top().size());
            for (int i = 0; i < limit; i++) {
                RankView.Play play = view.top().get(i);
                rows.add(List.of(
                        String.format(Locale.ROOT, "%02d.", i + 1),
                        play.name(),
                        String.valueOf(play.levelId()),
                        String.valueOf(play.tier()),
                        play.progress() + "%",
                        format(play.rt())));
            }
            List<SvgColumn> columns = List.of(
                    new SvgColumn("#", 40, SvgAlign.START),
                    new SvgColumn(GdText.HEAD_LEVEL.of(language), 390, SvgAlign.START),
                    new SvgColumn("ID", 90, SvgAlign.END),
                    new SvgColumn(GdText.TIER.of(language), 120, SvgAlign.END),
                    new SvgColumn(GdText.PROGRESS.of(language), 120, SvgAlign.END),
                    new SvgColumn("rt", 120, SvgAlign.END));
            List<SvgCell> cells = SvgTable.build(
                    40, 310, 346, 32, columns, rows, 14, 16, "#C9CFD8", "#E6E9EE");
            String stats1 = GdText.COMPLETED.of(language) + " " + view.beatenTotal()
                    + " · " + GdText.IN_PROGRESS.of(language) + " " + view.beatingTotal();
            String stats2 = GdText.HIGHEST_TIER.of(language) + " " + view.highestTier()
                    + " · " + GdText.SAMPLE.of(language) + " " + view.sample();
            if (view.sample() < GdScore.B20_SIZE)
                stats2 += " (" + GdText.FEWER_THAN_20.of(language) + ")";
            int height = 360 + (rows.size() + 1) * 32 + 50;
            Map<String, Object> model = Map.ofEntries(
                    Map.entry("user", SvgText.escape(SvgText.fit(
                            "GDDL: " + view.userName() + " (ID " + view.userId() + ")",
                            760, 26))),
                    Map.entry("labelB20", SvgText.escape(GdText.B20.of(language))),
                    Map.entry("score", format(view.score())),
                    Map.entry("stats1", SvgText.escape(stats1)),
                    Map.entry("stats2", SvgText.escape(stats2)),
                    Map.entry("cells", cells),
                    Map.entry("avatar", ImageData.square(view.avatar(), 160)),
                    Map.entry("note", SvgText.escape(GdText.RANK_NOTE.of(language))),
                    Map.entry("height", height)
            );
            return image(renderer.template("rank.jte", model));
        } catch (RuntimeException e) {
            log.warn("GD B20 card render failed; falling back to text", e);
            return fallback.rank(language, view);
        }
    }

    // ════ Helpers ═════════════════

    private static MsgContent image(byte[] png) {
        return MsgContent.builder().imageBase64(png).build();
    }

    private static GddlLine gddlLine(Language language, LevelView view) {
        if (view.tier() == null) {
            return new GddlLine("GDDL · " + (view.gddlFailed()
                    ? GdText.GDDL_QUERY_FAILED.of(language)
                    : GdText.GDDL_NOT_RATED.of(language)), "");
        }
        String main = "GDDL · " + GdText.TIER.of(language) + " " + view.tier()
                + " · " + GdText.RATING.of(language) + " " + decimal(view.rating())
                + " · " + GdText.ENJOYMENT.of(language) + " " + decimal(view.enjoyment());
        StringBuilder extra = new StringBuilder();
        if (view.ratingCount() != null)
            extra.append(number(view.ratingCount())).append(' ')
                    .append(GdText.RATINGS.of(language));
        if (view.aredlPosition() != null) {
            if (!extra.isEmpty()) extra.append(" · ");
            extra.append("AREDL #").append(view.aredlPosition());
        }
        return new GddlLine(main, extra.toString());
    }

    private record GddlLine(String main, String extra) {
    }

    private static String number(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String pad(String value) {
        return String.format(Locale.ROOT, "%-12s", value);
    }

    private static String decimal(Double value) {
        return value == null || value.isNaN() ? "-" : format(value);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String clean(String value, int max) {
        if (value == null) return "";
        return truncate(value.replaceAll("\\s+", " ").strip(), max);
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        if (value.codePointCount(0, value.length()) <= max) return value;
        return value.substring(0, value.offsetByCodePoints(0, max)) + "…";
    }
}
