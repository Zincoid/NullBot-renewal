package com.zincoid.query.prts;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.query.QueryException;
import com.zincoid.query.QueryPlugin;
import com.zincoid.query.QueryText;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * PRTS 指令：查询 PRTS 干员页面并输出指定栏目。
 * <p>PRTS command: queries a PRTS operator page and outputs the selected section.</p>
 */
@Slf4j
@CmdMapping({"PRTS", "PRTS查询"})
public class PrtsCmd implements Cmd {

    private static final int MAX_TEXT = 1800;

    private final QueryPlugin plugin;

    public PrtsCmd(QueryPlugin plugin) {
        this.plugin = plugin;
    }

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    private void handle(CmdContext ctx, CmdArgs args) {
        String first = args.next("");
        if (first.isBlank())
            throw new CmdWarnException(QueryText.PRTS_MISSING_KEYWORD.of(ctx.language()));
        Option option = Option.from(first);
        String keyword;
        if (option != null) {
            keyword = args.rest("").trim();
        } else {
            option = Option.SUMMARY;
            keyword = (first + " " + args.rest("")).trim();
        }
        if (keyword.isEmpty())
            throw new CmdWarnException(QueryText.PRTS_MISSING_KEYWORD.of(ctx.language()));

        Document document;
        try {
            document = plugin.prts().document(keyword);
        } catch (QueryException e) {
            log.warn("PRTS query failed: {}", keyword, e);
            throw new CmdWarnException(QueryText.PRTS_QUERY_FAILED.of(ctx.language()));
        }

        String body = switch (option) {
            case VOICE -> voiceText(document);
            case SUMMARY -> summaryText(ctx, document);
            case ARCHIVE -> sectionText(document, "干员档案");
            case RECORD -> sectionText(document, "干员密录");
            case PARADOX -> sectionText(document, "悖论模拟");
        };
        if (body == null || body.isBlank())
            throw new CmdWarnException(QueryText.PRTS_SECTION_NOT_FOUND.of(
                    ctx.language(), option.label(ctx.language())));

        String header = "PRTS " + keyword;
        if (option != Option.SUMMARY) header += " [" + option.label(ctx.language()) + "]";
        ctx.send(header + "\n" + truncate(ctx, body));
        log.info("PRTS sent: keyword={}, option={}", keyword, option.name());
    }

    // ════ Extract ═════════════════

    private static String summaryText(CmdContext ctx, Document document) {
        Language language = ctx.language();
        StringBuilder summary = new StringBuilder();
        append(summary, QueryText.PRTS_TRAITS.of(language), sectionText(document, "特性"));
        append(summary, QueryText.PRTS_STATS.of(language), sectionText(document, "属性"));
        append(summary, QueryText.PRTS_TALENTS.of(language), sectionText(document, "天赋"));
        if (!summary.isEmpty()) return summary.toString();
        return sectionText(document, "干员信息", true);
    }

    private static void append(StringBuilder text, String header, String body) {
        if (body == null || body.isBlank()) return;
        if (!text.isEmpty()) text.append('\n');
        text.append(header).append('\n').append(body);
    }

    private static String voiceText(Document document) {
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        for (Element item : document.select("div.voice-data-item")) {
            Element zh = item.selectFirst("div.voice-item-detail[data-kind-name=中文]");
            if (zh == null || zh.text().isBlank()) continue;
            String title = item.attr("data-title").trim();
            lines.add(clean(title.isEmpty() ? zh.text() : title + "：" + zh.text()));
        }
        if (lines.isEmpty()) return sectionText(document, "语音记录");
        return String.join("\n", lines);
    }

    private static String sectionText(Document document, String id) {
        return sectionText(document, id, false);
    }

    private static String sectionText(Document document, String id, boolean includeNested) {
        Element heading = document.select("h2").stream()
                .filter(element -> id.equals(element.id()))
                .findFirst()
                .orElse(null);
        if (heading == null) return null;
        Element anchor = heading.parent();
        if (anchor == null || !anchor.hasClass("mw-heading")) anchor = heading;
        Element section = document.createElement("div");
        for (Element sibling : anchor.nextElementSiblings()) {
            if (isHeading(sibling)) break;
            section.appendChild(sibling.clone());
        }
        section.select(".mw-editsection, style, script, sup.reference, .navbox, .toc").remove();

        LinkedHashSet<String> lines = new LinkedHashSet<>();
        for (Element table : section.select("table")) {
            if (!includeNested
                    && table.parents().stream().anyMatch(parent -> "table".equals(parent.tagName())))
                continue;
            for (Element row : table.select("tr")) {
                List<String> cells = new ArrayList<>();
                for (Element cell : row.children()) {
                    if (!"th".equals(cell.tagName()) && !"td".equals(cell.tagName())) continue;
                    String value = clean(cell.text());
                    if (!value.isBlank()) cells.add(value);
                }
                if (!cells.isEmpty()) lines.add(String.join(" | ", cells));
            }
        }
        for (Element paragraph : section.select("p")) {
            if (paragraph.parents().stream().anyMatch(parent -> "table".equals(parent.tagName())))
                continue;
            String value = clean(paragraph.text());
            if (!value.isBlank()) lines.add(value);
        }
        if (lines.isEmpty()) return clean(section.text());
        return String.join("\n", lines);
    }

    private static boolean isHeading(Element element) {
        if ("h2".equals(element.tagName())) return true;
        return element.hasClass("mw-heading") && element.selectFirst("h2") != null;
    }

    private static String clean(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ")
                .replaceAll("\\[(编辑|展开|收起|显示|隐藏)]", "")
                .trim();
    }

    private static String truncate(CmdContext ctx, String text) {
        if (text.length() <= MAX_TEXT) return text;
        return text.substring(0, MAX_TEXT) + QueryText.PRTS_TRUNCATED.of(ctx.language());
    }

    // ════ Options ═════════════════

    private enum Option {
        SUMMARY(null, "Summary"),
        VOICE("语音", "Voice"),
        ARCHIVE("档案", "Archive"),
        RECORD("密录", "Record"),
        PARADOX("悖论", "Paradox");

        private final String zh;
        private final String en;

        Option(String zh, String en) {
            this.zh = zh;
            this.en = en;
        }

        static Option from(String raw) {
            if (raw == null || raw.isBlank()) return null;
            for (Option option : values()) {
                if (option == SUMMARY) continue;
                if (option.zh.equals(raw) || option.en.equalsIgnoreCase(raw)
                        || option.name().equalsIgnoreCase(raw))
                    return option;
            }
            return null;
        }

        String label(Language language) {
            return language == Language.EN ? en : zh;
        }
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return QueryText.PRTS_HELP.of(language);
    }
}
