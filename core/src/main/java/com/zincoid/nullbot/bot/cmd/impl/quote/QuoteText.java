package com.zincoid.nullbot.bot.cmd.impl.quote;

import com.zincoid.nullbot.platform.persistence.po.Quote;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 语录的展示格式与已保存判定。
 * <p>Display format and already-saved detection for quotes.</p>
 */
final class QuoteText {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern PATTERN = Pattern.compile(
            "^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}]\\[No\\.\\d+][\\s\\S]*");

    private QuoteText() {
    }

    /**
     * 格式化一条语录。
     * <p>Formats one quote.</p>
     *
     * @param quote the quote
     * @return the formatted text
     */
    static String format(Quote quote) {
        String time = quote.getTime() == null ? "" : quote.getTime().format(FORMATTER);
        return """
                [%s][No.%d]
                %s
                \t—— %s(%s)""".formatted(
                time, quote.getId(), quote.getText(), quote.getNickname(), quote.getUserId());
    }

    /**
     * 判断文本是否已是语录格式。
     * <p>Whether the text already has the quote format.</p>
     *
     * @param text the text
     * @return true when already formatted as a quote
     */
    static boolean isFormatted(String text) {
        return PATTERN.matcher(text).matches();
    }
}
