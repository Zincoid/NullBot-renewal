package com.zincoid.query;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * Query 插件文案：中文与英文成对保存。
 * <p>Query plugin texts: Chinese and English stored in pairs.</p>
 */
public enum QueryText {

    // ════ AA ═════════════════════

    AA_HELP("""
            AA
            查看 Artificial Analysis 模型排行。
            用法：AA [查询项] [数量]
            查询项：智能/历史/性价比/速度/编程/成本/定价
            数量：1~25，默认 10
            别名：模型查询
            权限：>=0(normal)
            说明：需要在 query 插件配置 aa.api-key。""", """
            AA
            Shows Artificial Analysis model rankings.
            Usage: AA [metric] [count]
            Metrics: 智能/历史/性价比/速度/编程/成本/定价
            Count: 1..25, default 10
            Aliases: 模型查询
            Access: >=0(normal)
            Note: requires aa.api-key in the query plugin config."""),
    AA_UNKNOWN_METRIC("未知查询项：%s（可选：智能/历史/性价比/速度/编程/成本/定价）",
            "Unknown metric: %s (expected Intelligence/History/Value/Speed/Coding/Cost/Pricing)"),
    AA_NO_API_KEY("未配置 AA API Key", "AA API key is not configured"),
    AA_QUERY_FAILED("AA 查询失败，请检查 API Key 或网络",
            "AA query failed; check the API key or network"),
    AA_NO_DATA("没有可用数据", "No data available"),
    AA_MALFORMED_COUNT("数量格式错误：%s", "Malformed count: %s"),
    AA_INTELLIGENCE("智能", "Intelligence"),
    AA_COST("成本", "Cost"),
    AA_CODING("编程", "Coding"),
    AA_INPUT("输入", "Input"),
    AA_OUTPUT("输出", "Output"),
    AA_PER_TASK("/任务", "/task"),

    // ════ PRTS ═══════════════════

    PRTS_HELP("""
            PRTS
            查询 PRTS 干员页面。
            用法：PRTS [栏目] <关键词>
            栏目：语音/档案/密录/悖论，默认概要
            示例：PRTS 能天使
                  PRTS 档案 能天使
            别名：PRTS查询
            权限：>=0(normal)
            说明：输出为纯文本，过长会截断。""", """
            PRTS
            Queries a PRTS operator page.
            Usage: PRTS [section] <keyword>
            Sections: 语音/档案/密录/悖论, default summary
            Example: PRTS 能天使
                     PRTS 档案 能天使
            Aliases: PRTS查询
            Access: >=0(normal)
            Note: output is plain text and may be truncated."""),
    PRTS_MISSING_KEYWORD("缺少查询关键词", "Missing query keyword"),
    PRTS_QUERY_FAILED("PRTS 查询失败，请检查关键词或网络",
            "PRTS query failed; check the keyword or network"),
    PRTS_SECTION_NOT_FOUND("未找到栏目：%s", "Section not found: %s"),
    PRTS_TRAITS("【特性】", "[Traits]"),
    PRTS_STATS("【属性】", "[Stats]"),
    PRTS_TALENTS("【天赋】", "[Talents]"),
    PRTS_TRUNCATED("\n…（内容过长已截断）", "\n… (truncated)"),

    // ════ Trace ══════════════════

    TRACE_HELP("""
            Trace
            通过 SauceNAO 查询引用图片的出处。
            用法：[引用图片] Trace [数量]
            数量：1~5，默认 1
            别名：图溯源/溯源
            权限：>=0(normal)
            说明：配置 saucenao.api-key 可提高额度。""", """
            Trace
            Finds the source of a replied image through SauceNAO.
            Usage: [reply image] Trace [count]
            Count: 1..5, default 1
            Aliases: 图溯源/溯源
            Access: >=0(normal)
            Note: set saucenao.api-key for higher quota."""),
    TRACE_MISSING_IMAGE("缺少图片引用", "Missing image reply"),
    TRACE_NO_API_KEY("未配置 SauceNAO API Key", "SauceNAO API key is not configured"),
    TRACE_READ_FAILED("无法读取引用消息", "Cannot read the replied message"),
    TRACE_NO_IMAGE("引用中没有图片", "No image in the reply"),
    TRACE_FAILED("图片溯源失败，请稍后重试", "Image trace failed; try again later"),
    TRACE_NO_MATCHES("未找到匹配结果", "No matches found"),
    TRACE_MALFORMED_COUNT("数量格式错误：%s", "Malformed count: %s"),
    TRACE_SIMILARITY("相似度", "Similarity"),
    TRACE_SOURCE("来源", "Source"),
    TRACE_TITLE("标题", "Title"),
    TRACE_AUTHOR("作者", "Author"),
    TRACE_LINK("链接", "Link");

    private final String zh;
    private final String en;

    QueryText(String zh, String en) {
        this.zh = zh;
        this.en = en;
    }

    /**
     * 按语言选择文案。
     * <p>Selects the text for the given language.</p>
     *
     * @param language the reply language
     * @return the localized text
     */
    public String of(Language language) {
        return language == Language.EN ? en : zh;
    }

    /**
     * 按语言选择文案并填充参数。
     * <p>Selects the text for the given language and fills its arguments.</p>
     *
     * @param language the reply language
     * @param args     the format arguments
     * @return the localized text
     */
    public String of(Language language, Object... args) {
        return String.format(Locale.ROOT, of(language), args);
    }
}
