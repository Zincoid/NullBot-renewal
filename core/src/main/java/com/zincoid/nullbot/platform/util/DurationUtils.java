package com.zincoid.nullbot.platform.util;

import java.time.Duration;
import java.util.Locale;

/**
 * 时长解析工具：支持 {@code 30m}、{@code 2h}、{@code 7d}。
 * <p>Duration parsing utility: supports {@code 30m}, {@code 2h}, and {@code 7d}.</p>
 */
public final class DurationUtils {

    private DurationUtils() {
    }

    /**
     * 解析带单位的时长，格式错误时返回 null。
     * <p>Parses a duration with a unit, returning null when malformed.</p>
     *
     * @param raw the raw duration
     * @return the duration, or null
     */
    public static Duration parse(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.length() < 2) return null;
        char unit = value.charAt(value.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(value.substring(0, value.length() - 1));
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0) return null;
        try {
            return switch (unit) {
                case 'm' -> Duration.ofMinutes(amount);
                case 'h' -> Duration.ofHours(amount);
                case 'd' -> Duration.ofDays(amount);
                default -> null;
            };
        } catch (ArithmeticException e) {
            return null;
        }
    }
}
