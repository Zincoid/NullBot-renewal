package com.zincoid.query.aa;

import com.zincoid.nullbot.platform.persistence.enums.Language;

/**
 * AA 查询项。
 * <p>AA query metric.</p>
 */
public enum AaMetric {

    INTELLIGENCE("智能", "Intelligence"),
    HISTORY("历史", "History"),
    VALUE("性价比", "Value"),
    SPEED("速度", "Speed"),
    CODING("编程", "Coding"),
    COST("成本", "Cost"),
    PRICING("定价", "Pricing");

    private final String zh;
    private final String en;

    AaMetric(String zh, String en) {
        this.zh = zh;
        this.en = en;
    }

    /**
     * 解析查询项，支持中文、英文和枚举名。
     * <p>Parses a metric from Chinese, English, or the enum name.</p>
     *
     * @param raw user input
     * @return the metric, or null when unknown
     */
    public static AaMetric from(String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (AaMetric metric : values()) {
            if (metric.zh.equals(raw)
                    || metric.en.equalsIgnoreCase(raw)
                    || metric.name().equalsIgnoreCase(raw))
                return metric;
        }
        return null;
    }

    /**
     * 返回当前语言的查询项名称。
     * <p>Returns the metric label in the given language.</p>
     *
     * @param language reply language
     * @return the label
     */
    public String label(Language language) {
        return language == Language.EN ? en : zh;
    }
}
