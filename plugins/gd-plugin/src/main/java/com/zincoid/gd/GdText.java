package com.zincoid.gd;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * GD 插件文案：中文与英文成对保存。
 * <p>GD plugin texts: Chinese and English stored in pairs.</p>
 */
public enum GdText {

    HELP("""
            GD
            几何冲刺关卡查询与 GDDL B20 评分。

            用法：
              GD search <关键词> [选项]
              GD level <关卡ID>
              GD rank <GDDL用户名|ID> [--all]

            选项：
              --diff, -d      难度：easy,normal,hard,harder,insane,demon（逗号分隔）
              --demon         恶魔难度：easy,medium,hard,insane,extreme（自动按 demon 难度搜索）
              --length, -l    长度：tiny,short,medium,long,xl,platformer（逗号分隔）
              --filter        标签：star,no-star,featured,original,two-player,coins,epic,custom-song
              --page, -p      页码，从 1 开始
              --all, -a       显示完整 B20 列表

            别名：几何冲刺
            权限：>=0(normal)
            说明：B20 = 取 rt = tier^E × (进度/100)^1.5 最高的 20 条求平均；
                  E 默认 1.5，可在插件配置中将 score.tier-exponent 设为 2 使用平方。
                  用户评分仅统计 GDDL 提交记录（beaten/beating），未提交的关卡不计入。""",
            """
            GD
            Geometry Dash level lookup and GDDL B20 rating.

            Usage:
              GD search <keyword> [options]
              GD level <level id>
              GD rank <GDDL username|id> [--all]

            Options:
              --diff, -d      difficulties: easy,normal,hard,harder,insane,demon (comma separated)
              --demon         demon difficulty: easy,medium,hard,insane,extreme (implies --diff=demon)
              --length, -l    lengths: tiny,short,medium,long,xl,platformer (comma separated)
              --filter        tags: star,no-star,featured,original,two-player,coins,epic,custom-song
              --page, -p      page number, starting at 1
              --all, -a       show the full B20 list

            Aliases: 几何冲刺
            Access: >=0(normal)
            Notes: B20 = average of the best 20 rt values, where
                   rt = tier^E × (progress/100)^1.5 and E defaults to 1.5.
                   Set score.tier-exponent=2 in the plugin config to use squared tiers.
                   User scores only count GDDL submissions (beaten/beating)."""),

    UNKNOWN_SUBCOMMAND("未知子命令：%s", "Unknown subcommand: %s"),
    MISSING_KEYWORD("缺少搜索关键词", "Missing search keyword"),
    PAGE_FROM_ONE("页码必须从 1 开始", "Page must start at 1"),
    SEARCH_FAILED("几何冲刺服务器查询失败，请稍后重试",
            "Geometry Dash request failed; try again later"),
    NO_LEVELS("未找到关卡", "No levels found"),
    WAIT_CONFLICT("当前会话已有等待中的操作，请稍后再试",
            "Another input wait is active in this chat; try again later"),
    SELECT_TIMEOUT("选择超时", "Selection timed out"),
    INVOKER_ONLY("只有发起者可以选择", "Only the command invoker can choose"),
    CANCELLED("已取消", "Cancelled"),
    INDEX_OUT_OF_RANGE("序号超出范围", "Index out of range"),
    MISSING_LEVEL_ID("缺少关卡 ID", "Missing level id"),
    INVALID_LEVEL_ID("无效的关卡 ID：%s", "Invalid level id: %s"),
    LEVEL_NOT_FOUND("关卡不存在或已删除", "Level not found or deleted"),
    MISSING_USER("缺少 GDDL 用户名或 ID", "Missing GDDL username or id"),
    GDDL_FAILED("GDDL 查询失败，请稍后重试", "GDDL request failed; try again later"),
    NO_RECORDS("该用户没有 GDDL 通关记录", "This user has no GDDL completion records"),
    LIST_SEPARATOR("、", ", "),
    GDDL_USER_NOT_FOUND("未找到 GDDL 用户：%s", "GDDL user not found: %s"),
    MULTIPLE_USERS("找到多个 GDDL 用户：%s。请使用用户 ID 查询",
            "Multiple GDDL users found: %s. Use the user id instead"),
    INVALID_DIFFICULTY("无效的难度：%s", "Invalid difficulty: %s"),
    INVALID_DEMON("无效的恶魔难度：%s", "Invalid demon difficulty: %s"),
    INVALID_LENGTH("无效的长度：%s", "Invalid length: %s"),
    INVALID_FILTER("无效的标签：%s", "Invalid filter: %s"),
    DEMON_COMBINATION("--demon 只能与 --diff=demon 一起使用",
            "--demon can only be combined with --diff=demon"),
    AUTO_UNSUPPORTED("GD 搜索接口不支持 auto 难度筛选",
            "GD search does not support filtering by auto difficulty"),

    SEARCH_TITLE("搜索 \"%s\" · 第 %d 页", "Search \"%s\" · page %d"),
    SEARCH_PROMPT("回复序号查看详情，end 取消（%d 秒）",
            "Reply with a number to view details, or end to cancel (%d s)"),
    AUTHOR("作者", "Author"),
    DIFFICULTY("难度", "Difficulty"),
    DOWNLOADS("下载", "Downloads"),
    LIKES("点赞", "Likes"),
    LENGTH("长度", "Length"),
    OBJECTS("物体", "Objects"),
    SONG("歌曲", "Song"),
    DESCRIPTION("描述", "Description"),
    STARS("星", "Stars"),
    TIER("Tier", "Tier"),
    RATING("Rating", "Rating"),
    ENJOYMENT("Enjoyment", "Enjoyment"),
    RATINGS("个评分", "ratings"),
    GDDL_NOT_RATED("未收录", "not rated"),
    GDDL_QUERY_FAILED("查询失败", "query failed"),
    NO_THUMBNAIL("暂无缩略图", "No thumbnail"),
    COMPLETED("通关", "Completed"),
    IN_PROGRESS("进行中", "In progress"),
    B20("几何冲刺 B20", "GD B20"),
    HIGHEST_TIER("最高 tier", "Highest tier"),
    SAMPLE("样本", "Sample"),
    FEWER_THAN_20("不足 20 条", "fewer than 20 records"),
    TOP_N("前 %d 条", "Top %d"),
    RANK_NOTE("仅统计 GDDL 提交记录（beaten/beating）。",
            "Only GDDL submissions (beaten/beating) are counted.");

    private final String zh;
    private final String en;

    GdText(String zh, String en) {
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
