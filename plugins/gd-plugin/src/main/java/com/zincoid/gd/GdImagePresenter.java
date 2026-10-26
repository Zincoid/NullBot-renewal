package com.zincoid.gd;

import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.render.FontRegistry;
import com.zincoid.nullbot.module.render.PluginRenderer;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GD 图片呈现器：用 JTE/SVG 模板渲染搜索列表、关卡详情与 B20 卡片。
 * <p>GD image presenter: renders search lists, level details, and B20 cards with JTE/SVG templates.</p>
 */
@Slf4j
final class GdImagePresenter implements GdPresenter {

    private static final double LEVEL_THUMBNAIL_CENTER_Y = 271.5;
    private static final double LINE_ASCENT_RATIO = 0.9;
    private static final double LINE_DESCENT_RATIO = 0.25;
    private static final FontRenderContext FONT_CONTEXT = new FontRenderContext(null, true, true);
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
    GdImagePresenter(PluginRenderer renderer, GdPresenter fallback) {
        this.renderer = renderer;
        this.fallback = fallback;
    }

    // ════ Present ═════════════════

    @Override
    public MsgContent search(Language language, SearchView view) {
        try {
            List<String> ranks = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<String> difficulties = new ArrayList<>();
            List<String> authors = new ArrayList<>();
            List<String> stars = new ArrayList<>();
            for (int i = 0; i < view.levels().size(); i++) {
                SearchView.LevelSummary level = view.levels().get(i);
                ranks.add(String.format(Locale.ROOT, "%02d.", i + 1));
                names.add(escape(truncateToWidth(level.name(), 280, 15)));
                difficulties.add(escape(truncateToWidth(level.difficulty(), 160, 15)));
                authors.add(escape(truncateToWidth(level.author(), 170, 15)));
                stars.add(escape(STAR + " " + level.stars()));
            }
            int height = 190 + (ranks.size() + 1) * 34 + 60;
            Map<String, Object> model = Map.ofEntries(
                    Map.entry("title", escape(GdText.SEARCH_TITLE.of(
                            language, truncate(view.query(), 24), view.page()))),
                    Map.entry("headLevel", GdText.HEAD_LEVEL.of(language)),
                    Map.entry("headDifficulty", GdText.DIFFICULTY.of(language)),
                    Map.entry("headAuthor", GdText.AUTHOR.of(language)),
                    Map.entry("headStars", GdText.STARS.of(language)),
                    Map.entry("ranks", ranks),
                    Map.entry("names", names),
                    Map.entry("difficulties", difficulties),
                    Map.entry("authors", authors),
                    Map.entry("stars", stars),
                    Map.entry("prompt", escape(GdText.SEARCH_PROMPT.of(language, view.waitSeconds()))),
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
                        480 - textWidth(prefix, 13) - textWidth(suffix, 13));
                song = prefix + truncateToWidth(view.song(), titleWidth, 13) + suffix;
            }
            GddlLine gddl = gddlLine(language, view);
            List<TextLine> textLines = new ArrayList<>();
            textLines.add(new TextLine(escape(stats1), 0, 14, "#E6E9EE"));
            textLines.add(new TextLine(escape(stats2), 30, 14, "#E6E9EE"));
            textLines.add(new TextLine(escape(truncateToWidth(gddl.main(), 480, 14)), 40, 14, "#CDD3DC"));
            if (!gddl.extra().isBlank())
                textLines.add(new TextLine(escape(truncateToWidth(gddl.extra(), 480, 13)), 26, 13, "#C9CFD8"));
            if (!song.isBlank())
                textLines.add(new TextLine(escape(song), 32, 13, "#C9CFD8"));
            if (!description.isBlank())
                textLines.add(new TextLine(escape(truncateToWidth(description, 480, 13)), 30, 13, "#C9CFD8"));
            Map<String, Object> model = Map.ofEntries(
                    Map.entry("title", escape(truncateToWidth(view.name(), 760, 30))),
                    Map.entry("levelId", String.valueOf(view.id())),
                    Map.entry("meta", escape(truncateToWidth(meta, 870, 14))),
                    Map.entry("lines", layoutLines(textLines)),
                    Map.entry("thumbnail", thumbnail(view.thumbnail())),
                    Map.entry("noThumbnail", escape(GdText.NO_THUMBNAIL.of(language))),
                    Map.entry("height", 430)
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
            List<String> ranks = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            List<String> tiers = new ArrayList<>();
            List<String> progresses = new ArrayList<>();
            List<String> rts = new ArrayList<>();
            int limit = Math.min(view.limit(), view.top().size());
            for (int i = 0; i < limit; i++) {
                RankView.Play play = view.top().get(i);
                ranks.add(String.format(Locale.ROOT, "%02d.", i + 1));
                names.add(escape(truncate(play.name(), 30)));
                ids.add(String.valueOf(play.levelId()));
                tiers.add(String.valueOf(play.tier()));
                progresses.add(play.progress() + "%");
                rts.add(format(play.rt()));
            }
            String stats1 = GdText.COMPLETED.of(language) + " " + view.beatenTotal()
                    + " · " + GdText.IN_PROGRESS.of(language) + " " + view.beatingTotal();
            String stats2 = GdText.HIGHEST_TIER.of(language) + " " + view.highestTier()
                    + " · " + GdText.SAMPLE.of(language) + " " + view.sample();
            if (view.sample() < GdScore.B20_SIZE)
                stats2 += " (" + GdText.FEWER_THAN_20.of(language) + ")";
            int height = 360 + (ranks.size() + 1) * 32 + 50;
            Map<String, Object> model = Map.ofEntries(
                    Map.entry("user", escape("GDDL: " + truncate(view.userName(), 40)
                            + " (ID " + view.userId() + ")")),
                    Map.entry("labelB20", escape(GdText.B20.of(language))),
                    Map.entry("score", format(view.score())),
                    Map.entry("stats1", escape(stats1)),
                    Map.entry("stats2", escape(stats2)),
                    Map.entry("headLevel", GdText.HEAD_LEVEL.of(language)),
                    Map.entry("headTier", GdText.TIER.of(language)),
                    Map.entry("headProgress", GdText.PROGRESS.of(language)),
                    Map.entry("ranks", ranks),
                    Map.entry("names", names),
                    Map.entry("ids", ids),
                    Map.entry("tiers", tiers),
                    Map.entry("progresses", progresses),
                    Map.entry("rts", rts),
                    Map.entry("avatar", avatar(view.avatar())),
                    Map.entry("note", escape(GdText.RANK_NOTE.of(language))),
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

    private record TextLine(String text, int gapBefore, int size, String color) {
    }

    private static List<Map<String, String>> layoutLines(List<TextLine> source) {
        int[] offsets = new int[source.size()];
        for (int i = 1; i < source.size(); i++)
            offsets[i] = offsets[i - 1] + source.get(i).gapBefore();
        double ascent = source.getFirst().size() * LINE_ASCENT_RATIO;
        double descent = source.getLast().size() * LINE_DESCENT_RATIO;
        double top = LEVEL_THUMBNAIL_CENTER_Y
                - (offsets[offsets.length - 1] + descent - ascent) / 2;
        List<Map<String, String>> lines = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) {
            TextLine line = source.get(i);
            lines.add(Map.of(
                    "text", line.text(),
                    "y", String.valueOf(Math.round(top + offsets[i])),
                    "size", String.valueOf(line.size()),
                    "color", line.color()));
        }
        return lines;
    }

    private static String avatar(byte[] image) {
        if (image == null || image.length == 0) return "";
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(image));
            if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0)
                return "";
            int size = Math.min(source.getWidth(), source.getHeight());
            int sx = (source.getWidth() - size) / 2;
            int sy = (source.getHeight() - size) / 2;
            BufferedImage target = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = target.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, 160, 160,
                    sx, sy, sx + size, sy + size, null);
            graphics.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(target, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException | RuntimeException e) {
            log.warn("GD avatar conversion failed", e);
            return "";
        }
    }

    private static String thumbnail(byte[] webp) {
        if (webp == null || webp.length == 0) return "";
        ImageReader reader;
        try {
            reader = new WebPImageReaderSpi().createReaderInstance();
        } catch (IOException e) {
            log.warn("GD thumbnail conversion failed", e);
            return "";
        }
        if (reader == null) return "";
        try (ImageInputStream input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(webp))) {
            if (input == null) return "";
            reader.setInput(input, true, true);
            BufferedImage source = reader.read(0);
            if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) return "";
            double ratio = 16.0 / 9.0;
            int sx = 0;
            int sy = 0;
            int sw = source.getWidth();
            int sh = source.getHeight();
            if (sw / (double) sh > ratio) {
                sw = (int) (sh * ratio);
                sx = (source.getWidth() - sw) / 2;
            } else {
                sh = (int) (sw / ratio);
                sy = (source.getHeight() - sh) / 2;
            }
            BufferedImage target = new BufferedImage(720, 405, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = target.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, 720, 405,
                    sx, sy, sx + sw, sy + sh, null);
            graphics.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(target, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException | RuntimeException e) {
            log.warn("GD thumbnail conversion failed", e);
            return "";
        } finally {
            reader.dispose();
        }
    }

    private static String number(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String pad(String value) {
        return String.format(Locale.ROOT, "%-12s", value);
    }

    private static String truncateToWidth(String value, int maxWidth, float fontSize) {
        if (value == null || value.isEmpty()) return "";
        if (textWidth(value, fontSize) <= maxWidth) return value;
        int end = value.length();
        while (end > 0) {
            end = value.offsetByCodePoints(end, -1);
            String candidate = value.substring(0, end) + "…";
            if (textWidth(candidate, fontSize) <= maxWidth) return candidate;
        }
        return "…";
    }

    private static double textWidth(String value, float fontSize) {
        if (value == null || value.isEmpty()) return 0;
        Font font = new Font(FontRegistry.JETBRAINS_MAPLE_MONO, Font.PLAIN, 1).deriveFont(fontSize);
        return font.getStringBounds(value, FONT_CONTEXT).getWidth();
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

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
