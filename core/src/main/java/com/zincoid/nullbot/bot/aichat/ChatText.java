package com.zincoid.nullbot.bot.aichat;

import java.util.regex.Pattern;

/**
 * 聊天文本整理：移除模型复读的内部媒体注释与代码块外的空行，保留单个换行。
 * <p>Chat text cleanup: removes internal media notes echoed by the model and blank lines outside fenced code blocks while keeping single line breaks.</p>
 */
final class ChatText {

    private static final Pattern SENT_FILE_PATTERN = Pattern.compile("\\(sent [^()]*file id=\\d+[^()]*\\)");

    private ChatText() {
    }

    /**
     * 整理聊天文本：移除内部媒体注释与代码块外的连续空行。
     * <p>Cleans the chat text: removes internal media notes and blank lines outside fenced code blocks.</p>
     *
     * @param text the raw text
     * @return the cleaned text
     */
    static String clean(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        boolean fenced = false;
        for (String line : SENT_FILE_PATTERN.matcher(text).replaceAll("").lines().toList()) {
            String trimmed = line.stripLeading();
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~"))
                fenced = !fenced;
            if (!fenced && line.isBlank()) continue;
            if (!out.isEmpty()) out.append('\n');
            out.append(line);
        }
        return out.toString().strip();
    }
}
