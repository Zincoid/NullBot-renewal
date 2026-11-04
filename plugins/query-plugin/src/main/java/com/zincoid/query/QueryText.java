package com.zincoid.query;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * Query 插件文案：中文与英文成对保存。
 * <p>Query plugin texts: Chinese and English stored in pairs.</p>
 */
public enum QueryText {

    // ════ AA ══════════════════════

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

    // ════ PRTS ════════════════════

    PRTS_MISSING_KEYWORD("缺少查询关键词", "Missing query keyword"),
    PRTS_QUERY_FAILED("PRTS 查询失败，请检查关键词或网络",
            "PRTS query failed; check the keyword or network"),
    PRTS_SECTION_NOT_FOUND("未找到栏目：%s", "Section not found: %s"),
    PRTS_TRAITS("【特性】", "[Traits]"),
    PRTS_STATS("【属性】", "[Stats]"),
    PRTS_TALENTS("【天赋】", "[Talents]"),
    PRTS_TRUNCATED("\n…（内容过长已截断）", "\n… (truncated)"),

    // ════ Trace ═══════════════════

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
