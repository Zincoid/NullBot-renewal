package com.zincoid.nullbot.adapter.qq;

/**
 * QQ 适配器字符串工具。
 * <p>QQ adapter string helpers.</p>
 */
final class QqText {

    private QqText() {
    }

    /**
     * 判断字符串是否为 null 或空白。
     * <p>Checks whether a string is null or blank.</p>
     *
     * @param value the string
     * @return true when null or blank
     */
    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 返回第一个非空白字符串。
     * <p>Returns the first non-blank string.</p>
     *
     * @param first  the first candidate
     * @param second the fallback
     * @return the first non-blank value, or the fallback
     */
    static String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }
}
