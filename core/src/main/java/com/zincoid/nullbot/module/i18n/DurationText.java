package com.zincoid.nullbot.module.i18n;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.time.Duration;

/**
 * 时长文案工具：把时长格式化为中英文单位。
 * <p>Duration text utility: formats a duration with Chinese or English units.</p>
 */
public final class DurationText {

    private DurationText() {
    }

    /**
     * 格式化时长，向上取整到分钟。
     * <p>Formats a duration, rounding up to the minute.</p>
     *
     * @param language  the reply language
     * @param remaining the remaining duration
     * @return the formatted duration
     */
    public static String format(Language language, Duration remaining) {
        long seconds = Math.max(0, remaining.getSeconds());
        long minutes = (seconds + 59) / 60;
        if (minutes < 1) return Text.DURATION_LESS_MINUTE.of(language);
        if (minutes < 60) return Text.DURATION_MINUTES.of(language, minutes);
        long hours = minutes / 60;
        long restMinutes = minutes % 60;
        if (hours < 24) {
            return restMinutes == 0
                    ? Text.DURATION_HOURS.of(language, hours)
                    : Text.DURATION_HOURS_MINUTES.of(language, hours, restMinutes);
        }
        long days = hours / 24;
        long restHours = hours % 24;
        return restHours == 0
                ? Text.DURATION_DAYS.of(language, days)
                : Text.DURATION_DAYS_HOURS.of(language, days, restHours);
    }
}
