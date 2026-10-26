package com.zincoid.gd;

import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * GD 文本呈现器：当前以文字（关卡详情附带缩略图）输出视图。
 * <p>GD text presenter: currently renders views as text (with the thumbnail attached to level details).</p>
 */
final class GdTextPresenter implements GdPresenter {

    // ════ Present ═════════════════

    @Override
    public MsgContent search(Language language, SearchView view) {
        StringBuilder body = new StringBuilder();
        body.append(GdText.SEARCH_TITLE.of(language, view.query(), view.page())).append('\n');
        for (int i = 0; i < view.levels().size(); i++) {
            SearchView.LevelSummary level = view.levels().get(i);
            body.append(i + 1).append(". ").append(level.name())
                    .append(" (").append(level.id()).append(") · ")
                    .append(level.difficulty()).append(" · ")
                    .append(level.author()).append(" · ⭐")
                    .append(level.stars()).append('\n');
        }
        body.append(GdText.SEARCH_PROMPT.of(language, view.waitSeconds()));
        return MsgContent.of(body.toString());
    }

    @Override
    public MsgContent level(Language language, LevelView view) {
        MsgContent.Builder builder = MsgContent.builder().text(levelText(language, view));
        if (view.thumbnail() != null) builder.imageBase64(view.thumbnail());
        return builder.build();
    }

    @Override
    public MsgContent rank(Language language, RankView view) {
        StringBuilder body = new StringBuilder();
        body.append("GDDL: ").append(view.userName())
                .append(" (ID ").append(view.userId()).append(")\n");
        body.append(GdText.COMPLETED.of(language)).append(" ")
                .append(view.beatenTotal()).append(" · ")
                .append(GdText.IN_PROGRESS.of(language)).append(" ")
                .append(view.beatingTotal()).append('\n');
        body.append(GdText.B20.of(language)).append(": ")
                .append(format(view.score())).append('\n');
        body.append(GdText.HIGHEST_TIER.of(language)).append(": ")
                .append(view.highestTier()).append(" · ")
                .append(GdText.SAMPLE.of(language)).append(": ")
                .append(view.sample());
        if (view.sample() < GdScore.B20_SIZE)
            body.append(" (").append(GdText.FEWER_THAN_20.of(language)).append(")");
        body.append('\n');
        int limit = Math.min(view.limit(), view.top().size());
        if (limit > 0) {
            body.append(GdText.TOP_N.of(language, limit)).append(":\n");
            for (int i = 0; i < limit; i++) {
                RankView.Play play = view.top().get(i);
                body.append(i + 1).append(". ").append(play.name())
                        .append(" (").append(play.levelId()).append(") — ")
                        .append(GdText.TIER.of(language)).append(" ")
                        .append(play.tier()).append(" · ").append(play.progress())
                        .append("% · rt ").append(format(play.rt())).append('\n');
            }
        }
        body.append(GdText.RANK_NOTE.of(language));
        return MsgContent.of(body.toString());
    }

    // ════ Level ══════════════════

    private static String levelText(Language language, LevelView view) {
        StringBuilder body = new StringBuilder();
        body.append(view.name()).append(" (").append(view.id()).append(")\n");
        body.append(GdText.AUTHOR.of(language)).append(": ")
                .append(view.author()).append(" · ")
                .append(GdText.DIFFICULTY.of(language)).append(": ")
                .append(view.difficulty()).append(" · ⭐")
                .append(view.stars()).append('\n');
        body.append(GdText.DOWNLOADS.of(language)).append(": ")
                .append(number(view.downloads())).append(" · ")
                .append(GdText.LIKES.of(language)).append(": ")
                .append(number(view.likes())).append('\n');
        body.append(GdText.LENGTH.of(language)).append(": ")
                .append(view.length()).append(" · ")
                .append(GdText.OBJECTS.of(language)).append(": ")
                .append(number(view.objects())).append('\n');
        if (view.tier() != null) {
            body.append("GDDL: ").append(GdText.TIER.of(language)).append(" ")
                    .append(view.tier())
                    .append(" · ").append(GdText.RATING.of(language)).append(" ")
                    .append(decimal(view.rating()))
                    .append(" · ").append(GdText.ENJOYMENT.of(language)).append(" ")
                    .append(decimal(view.enjoyment()));
            if (view.ratingCount() != null)
                body.append(" · ").append(number(view.ratingCount())).append(" ")
                        .append(GdText.RATINGS.of(language));
            if (view.aredlPosition() != null)
                body.append(" · AREDL #").append(view.aredlPosition());
            body.append('\n');
        } else {
            body.append("GDDL: ").append(view.gddlFailed()
                    ? GdText.GDDL_QUERY_FAILED.of(language)
                    : GdText.GDDL_NOT_RATED.of(language)).append('\n');
        }
        if (view.song() != null && !view.song().isBlank()) {
            body.append(GdText.SONG.of(language)).append(": ").append(view.song());
            if (view.songId() != null) body.append(" · ID ").append(view.songId());
            body.append('\n');
        }
        String description = clean(view.description());
        if (!description.isEmpty())
            body.append(GdText.DESCRIPTION.of(language)).append(": ").append(description);
        return body.toString().stripTrailing();
    }

    // ════ Helpers ═════════════════

    private static String number(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static String decimal(Double value) {
        return value == null || value.isNaN() ? "-" : format(value);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String clean(String value) {
        if (value == null) return "";
        String text = value.replaceAll("\\s+", " ").strip();
        if (text.codePointCount(0, text.length()) <= 200) return text;
        return text.substring(0, text.offsetByCodePoints(0, 200)) + "…";
    }
}
