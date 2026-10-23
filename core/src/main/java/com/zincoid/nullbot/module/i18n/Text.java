package com.zincoid.nullbot.module.i18n;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * 用户可见文案的中英文目录。
 * <p>Bilingual catalog of user-visible text.</p>
 */
public enum Text {

    // ════ Common ═════════════════

    MISSING_REPLY("缺少回复", "Missing reply"),
    MALFORMED_OPTION_ID("选项格式错误：id", "Malformed option: id"),
    UNSUPPORTED_CONTENT("不支持的内容", "Unsupported content"),
    NO_TEXT_CONTENT("没有文本内容", "No text content"),
    FILE_NOT_FOUND("文件不存在：%d", "File not found: %d"),
    NOT_IMAGE("不是图片：%d", "Not an image: %d"),
    NOT_VIDEO("不是视频：%d", "Not a video: %d"),
    NO_IMAGES("没有图片", "No images"),
    NO_VIDEOS("没有视频", "No videos"),
    NO_IMAGES_IN_REPLY("回复中没有图片", "No images in reply"),
    NO_VIDEOS_IN_REPLY("回复中没有视频", "No videos in reply"),
    QUOTE_NOT_FOUND("语录不存在：%d", "Quote not found: %d"),
    NO_QUOTES("没有语录", "No quotes"),
    QUOTE_ALREADY_SAVED("语录已保存", "Quote already saved"),
    AI_GUARD_REFUSED("⚠️ 对话被拒绝", "⚠️ Conversation refused"),

    // ════ Arguments ══════════════

    ARG_MISSING("缺少参数", "Missing argument"),
    ARG_MALFORMED("参数格式错误", "Malformed argument"),
    ARG_MISSING_OPTION("缺少选项：--%s / -%s", "Missing option: --%s / -%s"),
    ARG_MISSING_OPTION_ALIAS("缺少选项：--%s", "Missing option: --%s"),

    // ════ Handlers ═══════════════

    ACCESS_DENIED("🚫 权限不足：需要 %s，当前 %s", "🚫 Access denied: required %s, yours %s"),
    TOO_MANY_REQUESTS("⚠️ 请求过于频繁", "⚠️ Too many requests"),
    HELP_PRIVATE_UNAVAILABLE("⚠️ 私聊中无法查看帮助", "⚠️ Help is unavailable in private chat"),
    HELP_UNAVAILABLE("暂无帮助", "No help available"),
    CMD_WARN("⚠️ 警告：%s", "⚠️ Warn: %s"),
    CMD_ERROR("❌ 错误：%s", "❌ Error: %s"),

    // ════ Unsupported events ═════

    CMD_UNSUPPORTED_GROUP_MSG("指令不支持群消息事件", "Command does not support group message events"),
    CMD_UNSUPPORTED_PRIVATE_MSG("指令不支持私聊消息事件", "Command does not support private message events"),
    CMD_UNSUPPORTED_GROUP_POKE("指令不支持群戳一戳事件", "Command does not support group poke events"),
    CMD_UNSUPPORTED_PRIVATE_POKE("指令不支持私聊戳一戳事件", "Command does not support private poke events"),
    CMD_UNSUPPORTED_GROUP_RECALL("指令不支持群撤回事件", "Command does not support group recall events"),
    CMD_UNSUPPORTED_PRIVATE_RECALL("指令不支持私聊撤回事件", "Command does not support private recall events"),
    CMD_UNSUPPORTED_JOIN("指令不支持入群事件", "Command does not support group join events"),
    CMD_UNSUPPORTED_LEAVE("指令不支持离群事件", "Command does not support group leave events"),

    // ════ Quiz ═══════════════════

    QUIZ_ALREADY_IN_GAME("你已经在问答中了", "You are already in a quiz"),
    QUIZ_GENERATING("⏳ 正在生成问题...", "⏳ Generating question..."),
    QUIZ_QUESTION("""
            请回答问题：
            %s

            发送选项字母。
            时限：%d 秒。""", """
            Answer the question:
            %s

            Send the option letter.
            Time limit: %ds."""),
    QUIZ_TIMEOUT("%s 超时了！答案是 %s。", "%s timed out! The answer is %s."),
    QUIZ_CORRECT("%s 回答正确！", "%s answered correctly!"),
    QUIZ_WRONG("%s 回答错误！答案是 %s。", "%s answered wrong! The answer is %s."),
    QUIZ_GENERATE_FAILED("生成问题失败", "Failed to generate question"),
    QUIZ_EMPTY_RESPONSE("AI 返回为空", "Empty response"),
    QUIZ_MALFORMED_RESPONSE("AI 返回格式错误", "Malformed response"),
    QUIZ_DEFAULT_TOPIC("二次元", "anime"),

    // ════ Wife ═══════════════════

    WIFE_MEMBER_ALREADY("\n今天已经选过了哦💦...\n你的群友老婆是\n%s(%s)",
            "\nAlready picked today💦...\nYour group wife is\n%s(%s)"),
    WIFE_MEMBER_PICKED("\n你的今日群友老婆是✨\n%s(%s)",
            "\nYour group wife of the day is✨\n%s(%s)"),
    WIFE_ANIME_ALREADY("\n今天已经选过了哦💦...\n你的二次元老婆是\n%s",
            "\nAlready picked today💦...\nYour anime wife is\n%s"),
    WIFE_ANIME_PICKED("\n你的今日二次元老婆是✨\n%s",
            "\nYour anime wife of the day is✨\n%s"),
    WIFE_NO_RESULT("暂无角色", "No character found"),
    WIFE_NO_MEMBERS("群成员不足", "Not enough group members"),
    WIFE_NOT_ALLOWED("你不能用。", "You cannot use this option"),

    // ════ Guess ══════════════════

    GUESS_START("✨猜图题目如下！", "✨Guess the image!"),
    GUESS_HINT("注: 请发送\"%s答案\"，发送\"%s%s\"放弃",
            "Note: send \"%sanswer\"; send \"%s%s\" to give up."),
    GUESS_END("游戏结束啦💦\n答案是...%s！", "Game over💦\nThe answer is... %s!"),
    GUESS_FAIL("已经错%d次啦💦\n答案是...%s！", "Wrong %d times💦\nThe answer is... %s!"),
    GUESS_CORRECT("%s 猜对啦✨\n答案是...%s！\n一共猜了%s次",
            "%s guessed right✨\nThe answer is... %s!\nGuessed %s times"),
    GUESS_WRONG(" 猜错啦！", " Wrong guess!"),
    GUESS_ALREADY_PLAYING("已在游戏中", "Game already in progress"),
    GUESS_NOT_PLAYING("未在游戏中", "Not in a game"),
    GUESS_NO_IMAGES("暂无可用图片", "No images available"),
    GUESS_CROP_FAILED("图片裁剪失败", "Failed to crop image"),

    // ════ Media ══════════════════

    IMAGE_SAVED_ONE("💾 图片已保存：id=%d", "💾 Image saved: id=%d"),
    IMAGE_SAVED_MANY("💾 已保存 %d 张图片", "💾 Images saved: %d"),
    IMAGE_DELETED("⚠️ 图片已删除：%d", "⚠️ Image deleted: %d"),
    VIDEO_SAVED_ONE("🎥 视频已保存：id=%d", "🎥 Video saved: id=%d"),
    VIDEO_SAVED_MANY("🎥 已保存 %d 个视频", "🎥 Videos saved: %d"),
    VIDEO_DELETED("⚠️ 视频已删除：%d", "⚠️ Video deleted: %d"),
    QUOTE_SAVED("💾 语录已保存：no.%d", "💾 Quote saved: no.%d"),
    QUOTE_DELETED("⚠️ 语录 No.%d 已删除", "⚠️ Quote No.%d deleted"),

    // ════ Config ═════════════════

    CONFIG_HEADER("配置：", "Config:"),
    CONFIG_EMPTY("（空）", "(empty)"),
    CONFIG_RESET("✅ 配置已重置：%s", "✅ Config reset: %s"),
    CONFIG_UPDATED("✅ 配置已更新：%s = %s", "✅ Config updated: %s = %s"),
    CONFIG_UNKNOWN_KEY("未知配置项：%s", "Unknown config key: %s"),
    CONFIG_INVALID_LANGUAGE("语言只能是 zh 或 en：%s", "Language must be zh or en: %s"),

    // ════ Member ═══════════════

    MEMBER_JOINED("欢迎新成员 %s(%s) 加入！", "Welcome new member %s(%s)!"),
    MEMBER_LEFT("%s(%s) 离开了我们...", "%s(%s) left the group..."),
    MEMBER_KICKED("%s(%s) 被驱逐了...", "%s(%s) was kicked..."),

    // ════ History ════════════════

    HISTORY_HEADER("聊天历史（最近 %d 条）：", "Chat history (latest %d):"),
    HISTORY_EMPTY("暂无聊天历史", "No chat history"),
    HISTORY_ASSISTANT("AI", "AI"),
    HISTORY_CLEARED("♻️ 聊天历史已清空（%d 条）", "♻️ Chat history cleared (%d messages)"),
    HISTORY_UNKNOWN_OP("未知历史参数：%s", "Unknown history argument: %s"),

    // ════ GroupSet ═══════════════

    GROUPSET_UNKNOWN_OPTION("未知选项", "Unknown option"),
    GROUPSET_UNKNOWN_LIMIT("未知限速项：%s", "Unknown limit setting: %s"),
    GROUPSET_UNKNOWN_AI("未知 AI 项：%s", "Unknown AI setting: %s"),
    GROUPSET_MISSING_PROMPT("缺少提示词", "Missing prompt"),
    GROUPSET_MALFORMED_BOOLEAN("布尔值格式错误：%s", "Malformed boolean: %s"),
    GROUPSET_VALUE_POSITIVE("值必须为正数：%d", "Value must be positive: %d"),
    GROUPSET_VALUE_FREQUENCY("频率必须在 0~1 之间：%s", "Frequency must be between 0 and 1: %s"),
    GROUPSET_UNKNOWN_SCOPE("未知范围：%s", "Unknown scope: %s"),
    GROUPSET_UNKNOWN_EFFORT("未知强度：%s", "Unknown effort: %s"),
    GROUPSET_UNKNOWN_LANGUAGE("未知语言：%s（可选 auto|zh|en）", "Unknown language: %s (expected auto|zh|en)"),
    GROUPSET_UPDATED("✅ 群设置已更新：%s", "✅ Group setting updated: %s"),
    GROUPSET_LANGUAGE("language = %s", "language = %s"),
    GROUPSET_NOTICE_JOIN("入群通知 = %s", "Join notice = %s"),
    GROUPSET_NOTICE_LEAVE("退群通知 = %s", "Leave notice = %s"),
    GROUPSET_UNKNOWN_NOTICE("未知通知项：%s", "Unknown notice setting: %s"),
    GROUPSET_LIMIT_ENABLED("限速启用 = %s", "limit enabled = %s"),
    GROUPSET_LIMIT_SCOPE("限速范围 = %s", "limit scope = %s"),
    GROUPSET_LIMIT_CAPACITY("限速容量 = %s", "limit capacity = %s"),
    GROUPSET_LIMIT_REFILL("限速补充 = %s", "limit refill = %s"),
    GROUPSET_LIMIT_INTERVAL("限速间隔 = %s", "limit interval = %s"),
    GROUPSET_AI_EFFORT("AI 强度 = %s", "AI effort = %s"),
    GROUPSET_AI_PROMPT_RESET("AI 提示词已重置为默认", "AI prompt reset to default"),
    GROUPSET_AI_PROMPT_UPDATED("AI 提示词已更新", "AI prompt updated"),
    GROUPSET_AI_GUARD("AI 防注入 = %s", "AI injection guard = %s"),
    GROUPSET_AI_AUTO_CHAT("AI 自动回复 = %s", "AI auto reply = %s"),
    GROUPSET_AI_CHAT_FREQUENCY("AI 自动回复频率 = %s", "AI auto reply frequency = %s"),
    GROUPSET_VIEW("""
            群设置：%s
            语言：%s
            通知：入群=%s, 退群=%s
            限速：enabled=%s, scope=%s, capacity=%s, refill=%s, interval=%s
            AI：effort=%s, guard=%s, auto=%s, frequency=%s
            提示词：%s""", """
            Group settings: %s
            Language: %s
            Notices: join=%s, leave=%s
            Limit: enabled=%s, scope=%s, capacity=%s, refill=%s, interval=%s
            AI: effort=%s, guard=%s, auto=%s, frequency=%s
            Prompt: %s"""),
    GROUPSET_LANG_AUTO("auto（全局：%s）", "auto (global: %s)"),
    GROUPSET_DEFAULT("（默认）", "(default)");

    private final String zh;
    private final String en;

    Text(String zh, String en) {
        this.zh = zh;
        this.en = en;
    }

    /**
     * 按语言渲染文案并填充参数。
     * <p>Renders the text in the given language and fills the arguments.</p>
     *
     * @param language the reply language, AUTO is treated as Chinese
     * @param args     the format arguments
     * @return the rendered text
     */
    public String of(Language language, Object... args) {
        String template = language == Language.EN ? en : zh;
        return args == null || args.length == 0
                ? template
                : String.format(Locale.ROOT, template, args);
    }
}
