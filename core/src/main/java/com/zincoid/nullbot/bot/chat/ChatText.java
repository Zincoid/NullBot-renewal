package com.zincoid.nullbot.bot.chat;

/**
 * 聊天文本整理：移除代码块外的空行，保留单个换行。
 * <p>Chat text cleanup: removes blank lines outside fenced code blocks while keeping single line breaks.</p>
 */
final class ChatText {

    private ChatText() {
    }

    /**
     * 移除代码块外的连续空行。
     * <p>Removes blank lines outside fenced code blocks.</p>
     *
     * @param text the raw text
     * @return the compacted text
     */
    static String compact(String text) {
        if (text == null || text.isEmpty()) return "";
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder out = new StringBuilder();
        boolean fenced = false;
        for (String line : normalized.split("\n", -1)) {
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
