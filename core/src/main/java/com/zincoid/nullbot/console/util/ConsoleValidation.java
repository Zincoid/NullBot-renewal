package com.zincoid.nullbot.console.util;

import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.platform.util.PageUtils;
import org.springframework.http.HttpStatus;

/**
 * 控制台请求参数的通用校验。
 * <p>Common validation for console request parameters.</p>
 */
public final class ConsoleValidation {

    private ConsoleValidation() {
    }

    /**
     * 去掉首尾空白并校验长度，空串返回 null。
     * <p>Trims the value and checks its length, returning null for a blank value.</p>
     *
     * @param raw   the raw value, nullable
     * @param max   the maximum length
     * @param field the field name used in the error message
     * @return the trimmed value, or null when blank
     */
    public static String text(String raw, int max, String field) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.length() > max)
            throw new ConsoleException(HttpStatus.BAD_REQUEST, field + " too long");
        return value.isEmpty() ? null : value;
    }

    /**
     * 校验必填文本并返回去空白后的值。
     * <p>Validates a required text and returns its trimmed value.</p>
     *
     * @param raw   the raw value, nullable
     * @param max   the maximum length
     * @param field the field name used in the error message
     * @return the trimmed value
     */
    public static String required(String raw, int max, String field) {
        String value = text(raw, max, field);
        if (value == null)
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Missing " + field);
        return value;
    }

    /**
     * 把页码收敛到从 1 开始。
     * <p>Clamps the page number to start from one.</p>
     *
     * @param page the raw page number
     * @return the clamped page number
     */
    public static long page(long page) {
        return PageUtils.page(page);
    }

    /**
     * 把页大小收敛到 1..100。
     * <p>Clamps the page size to 1..100.</p>
     *
     * @param size the raw page size
     * @return the clamped page size
     */
    public static long size(long size) {
        return PageUtils.size(size);
    }
}
