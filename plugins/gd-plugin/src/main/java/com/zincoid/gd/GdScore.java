package com.zincoid.gd;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * B20 评分：计算单关 rt，并取最高的 20 条求平均。
 * <p>B20 scoring: computes the per-level rt and averages the best 20 values.</p>
 */
final class GdScore {

    static final int B20_SIZE = 20;
    static final double PROGRESS_EXPONENT = 1.5;

    private GdScore() {
    }

    /**
     * 计算单关 rt：tier^E × (进度/100)^1.5。
     * <p>Computes a single-play rt: tier^E × (progress / 100)^1.5.</p>
     *
     * @param play          the play
     * @param tierExponent  the exponent applied to the tier
     * @return the rt value
     */
    static double rt(Play play, double tierExponent) {
        int progress = Math.clamp(play.progress(), 0, 100);
        return Math.pow(play.tier(), tierExponent)
                * Math.pow(progress / 100.0, PROGRESS_EXPONENT);
    }

    /**
     * 计算 B20：按 rt 降序取前 min(20, size) 条求平均。
     * <p>Computes B20: sorts by rt descending and averages the best min(20, size) values.</p>
     *
     * @param plays         the candidate plays
     * @param tierExponent  the exponent applied to the tier
     * @return the B20 result
     */
    static B20 b20(List<Play> plays, double tierExponent) {
        List<Scored> scored = new ArrayList<>(plays.size());
        for (Play play : plays) scored.add(new Scored(play, rt(play, tierExponent)));
        scored.sort(Comparator.comparingDouble(Scored::rt).reversed());
        int sample = Math.min(B20_SIZE, scored.size());
        double score = scored.stream()
                .limit(sample)
                .mapToDouble(Scored::rt)
                .average()
                .orElse(0);
        return new B20(score, sample, List.copyOf(scored.subList(0, sample)));
    }

    /**
     * 参与评分的单条记录。
     * <p>A single play participating in the score.</p>
     *
     * @param levelId  the level id
     * @param name     the level name
     * @param tier     the rounded GDDL tier
     * @param progress the completion percentage
     */
    record Play(long levelId, String name, int tier, int progress) {
    }

    /**
     * 带 rt 的单条记录。
     * <p>A play with its computed rt.</p>
     *
     * @param play the play
     * @param rt   the rt value
     */
    record Scored(Play play, double rt) {
    }

    /**
     * B20 计算结果。
     * <p>The B20 result.</p>
     *
     * @param score  the average of the top values
     * @param sample the number of values averaged
     * @param top    the top values, at most 20
     */
    record B20(double score, int sample, List<Scored> top) {
    }
}
