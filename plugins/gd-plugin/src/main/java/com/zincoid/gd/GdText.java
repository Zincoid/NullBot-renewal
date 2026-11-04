package com.zincoid.gd;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * GD 插件文案：中文与英文成对保存。
 * <p>GD plugin texts: Chinese and English stored in pairs.</p>
 */
public enum GdText {

    // ════ Errors ══════════════════

    UNKNOWN_SUBCOMMAND("未知子命令：%s", "Unknown subcommand: %s"),
    GDDL_SPLIT("GDDL 已拆分为独立指令，请使用 GDDL b20 [GDDL用户名|ID] 查询",
            "GDDL is now a separate command; use GDDL b20 [GDDL username|id] to query"),
    MISSING_KEYWORD("缺少搜索关键词", "Missing search keyword"),
    PAGE_FROM_ONE("页码必须从 1 开始", "Page must start at 1"),
    SEARCH_FAILED("几何冲刺服务器查询失败，请稍后重试",
            "Geometry Dash request failed; try again later"),
    NO_LEVELS("未找到关卡", "No levels found"),
    PAGE_LAST("已经是最后一页", "Already on the last page"),
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
    BIND_ALREADY("已绑定 GDDL 用户 %s (%d)，如需更换请先解绑",
            "Already bound to GDDL user %s (%d); unbind first"),
    BIND_SUCCESS("已绑定 GDDL 用户 %s (%d)", "Bound GDDL user %s (%d)"),
    UNBIND_NONE("当前聊天用户没有绑定 GDDL 账号",
            "No GDDL account is bound to this chat user"),
    UNBIND_SUCCESS("已解绑 GDDL 用户 %s (%d)", "Unbound GDDL user %s (%d)"),
    NO_BINDING("请先使用 GDDL bind <用户名|ID> 绑定 GDDL 用户",
            "Bind a GDDL user first with GDDL bind <username|id>"),
    BIND_SAVE_FAILED("保存 GDDL 绑定失败", "Failed to save GDDL binding"),
    INVALID_DIFFICULTY("无效的难度：%s", "Invalid difficulty: %s"),
    INVALID_DEMON("无效的恶魔难度：%s", "Invalid demon difficulty: %s"),
    INVALID_LENGTH("无效的长度：%s", "Invalid length: %s"),
    INVALID_FILTER("无效的标签：%s", "Invalid filter: %s"),
    DEMON_COMBINATION("--demon 只能与 --diff=demon 一起使用",
            "--demon can only be combined with --diff=demon"),
    AUTO_UNSUPPORTED("GD 搜索接口不支持 auto 难度筛选",
            "GD search does not support filtering by auto difficulty"),

    // ════ Display ═════════════════

    SEARCH_TITLE("搜索 \"%s\" · 第 %d 页", "Search \"%s\" · page %d"),
    SEARCH_HEADING("搜索 \"%s\"", "Search \"%s\""),
    SEARCH_PAGE("第 %d 页", "Page %d"),
    HEAD_LEVEL("关卡", "Level"),
    SEARCH_PROMPT("回复序号查看详情，next 下一页，end 取消（%d 秒）",
            "Reply with a number, next for the next page, or end to cancel (%d s)"),
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
    TIER_VALUE("%d(%.2f)", "%d(%.2f)"),
    PROGRESS("进度", "Progress"),
    RATING("Rt.", "Rt."),
    ENJOYMENT("Enj.", "Enj."),
    RATINGS("个评分", "ratings"),
    GDDL_NOT_RATED("未收录", "not rated"),
    GDDL_QUERY_FAILED("查询失败", "query failed"),
    NO_THUMBNAIL("暂无缩略图", "No thumbnail"),
    COMPLETED("通关", "Completed"),
    IN_PROGRESS("进行中", "In progress"),
    GDDL_B20("网络 B20", "Online B20"),
    HIGHEST_TIER("最高 tier", "Highest tier"),
    SAMPLE("样本", "Sample"),
    FEWER_THAN_20("不足 20 条", "fewer than 20 records"),
    TOP_N("前 %d 条", "Top %d"),
    GDDL_B20_NOTE("仅统计 GDDL 提交记录（beaten/beating）。",
            "Only GDDL submissions (beaten/beating) are counted."),

    // ════ Progress ════════════════

    MISSING_PROGRESS("缺少进度百分比", "Missing progress percentage"),
    INVALID_PROGRESS("进度必须是 1–100 的整数", "Progress must be an integer from 1 to 100"),
    NOT_REGISTERED("请先使用 GD register 注册", "Register first with GD register"),
    REGISTER_SUCCESS("已注册本地进度用户", "Registered local progress user"),
    REGISTER_ALREADY("已经注册过了", "Already registered"),
    STORAGE_FAILED("本地进度存储失败，请稍后重试",
            "Local progress storage failed; try again later"),
    UPDATE_SUCCESS("已记录 %s by %s (#%d) — %d%%",
            "Recorded %s by %s (#%d) — %d%%"),
    REMOVE_SUCCESS("已移除关卡 #%d 的进度记录", "Removed progress for level #%d"),
    REMOVE_NONE("没有该关卡的进度记录", "No progress record for this level"),
    UNREGISTER_CONFIRM("将删除 %d 条进度并注销，回复 yes 确认（%d 秒）",
            "This will delete %d progress records and unregister you; reply yes to confirm (%d s)"),
    UNREGISTER_CANCELLED("已取消注销", "Unregister cancelled"),
    UNREGISTER_SUCCESS("已注销并删除 %d 条进度记录",
            "Unregistered and deleted %d progress records"),
    NO_LOCAL_RECORDS("没有本地进度记录，请先使用 GD update 添加",
            "No local progress records; add one with GD update"),
    NO_RATED_RECORDS("没有可用于 B20 的 GDDL 评级记录",
            "No GDDL-rated records available for B20"),
    UNRATED_SKIPPED("%d 条无 GDDL 评级，未计入", "%d unrated entries skipped"),
    LOCAL_B20("本地 B20", "Local B20"),
    LOCAL_B20_NOTE("本地自报进度，非 GDDL 官方记录。",
            "Local self-reported progress; not an official GDDL record."),
    LOCAL_B20_FAILED("本地 B20 计算失败，请稍后重试",
            "Local B20 calculation failed; try again later");

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
