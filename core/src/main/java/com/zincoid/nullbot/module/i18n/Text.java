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
    UNSUPPORTED_CONTENT("不支持的内容", "Unsupported content"),
    NO_TEXT_CONTENT("没有文本内容", "No text content"),
    FILE_NOT_FOUND("文件不存在：%d", "File not found: %d"),
    UNKNOWN_TYPE("未知类型：%s（可选 img|aud|vid|quot）",
            "Unknown type: %s (expected img|aud|vid|quot)"),
    TYPE_MISMATCH("类型不匹配：%d", "Type mismatch: %d"),
    NO_IMAGES("没有图片", "No images"),
    NO_VIDEOS("没有视频", "No videos"),
    NO_AUDIOS("没有音频", "No audio"),
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
    GROUP_ACCESS_DENIED("🚫 群权限不足：需要 %s，当前 %s", "🚫 Group access denied: required %s, current %s"),
    TOO_MANY_REQUESTS("⚠️ 请求过于频繁", "⚠️ Too many requests"),
    HELP_PRIVATE_UNAVAILABLE("⚠️ 私聊中无法查看帮助", "⚠️ Help is unavailable in private chat"),
    HELP_UNAVAILABLE("暂无帮助", "No help available"),
    HELP_PLUGIN_NOT_FOUND("插件不存在：%s", "Plugin not found: %s"),
    HELP_PLUGIN_MISSING("缺少插件名", "Missing plugin name"),
    HELP_PLUGIN_HINT("查看插件指令：Help -p=插件名",
            "View plugin commands: Help -p=name"),
    HELP_NONE("无", "None"),
    HELP_DETAIL("指令名 -H 查看详情", "Cmd -H for details"),
    CMD_WARN("⚠️ 警告：%s", "⚠️ Warn: %s"),
    CMD_ERROR("❌ 错误：%s", "❌ Error: %s"),

    // ════ Unsupported events ═════

    CMD_UNSUPPORTED_GROUP_MSG("指令不支持群消息事件", "Command does not support group message events"),
    CMD_UNSUPPORTED_PRIVATE_MSG("指令不支持私聊消息事件", "Command does not support private message events"),

    // ════ Media ══════════════════

    IMAGE_SAVED_ONE("💾 图片已保存：id=%d", "💾 Image saved: id=%d"),
    IMAGE_SAVED_MANY("💾 已保存 %d 张图片", "💾 Images saved: %d"),
    IMAGE_DELETED("⚠️ 图片已删除：%d", "⚠️ Image deleted: %d"),
    VIDEO_SAVED_ONE("🎥 视频已保存：id=%d", "🎥 Video saved: id=%d"),
    VIDEO_SAVED_MANY("🎥 已保存 %d 个视频", "🎥 Videos saved: %d"),
    VIDEO_DELETED("⚠️ 视频已删除：%d", "⚠️ Video deleted: %d"),
    AUDIO_SAVED_ONE("🎵 音频已保存：id=%d", "🎵 Audio saved: id=%d"),
    AUDIO_SAVED_MANY("🎵 已保存 %d 个音频", "🎵 Audio saved: %d"),
    AUDIO_DELETED("⚠️ 音频已删除：%d", "⚠️ Audio deleted: %d"),
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

    // ════ Anti-Recall ════════════

    ANTI_RECALL_DETECTED("""
            检测到撤回消息！
            用户: %s(%s)
            内容: %s""", """
            Message recalled!
            User: %s(%s)
            Content: %s"""),

    // ════ History ════════════════

    HISTORY_TITLE("聊天历史", "Chat history"),
    HISTORY_EMPTY("暂无聊天历史", "No chat history"),
    HISTORY_ASSISTANT("AI", "AI"),
    HISTORY_ATTACHMENTS("附件：%s", "Attachments: %s"),
    HISTORY_CLEARED("♻️ 聊天历史已清空（%d 条）", "♻️ Chat history cleared (%d messages)"),
    HISTORY_UNKNOWN_OP("未知历史参数：%s", "Unknown history argument: %s"),

    // ════ Prompt ═════════════════

    PROMPT_VIEW("当前提示词：%s", "Current prompt: %s"),
    PROMPT_MISSING("缺少提示词", "Missing prompt"),
    PROMPT_GROUP_HINT("该指令用于管理私聊提示词，群提示词请通过群设置修改", "This command manages private-chat prompts; change group prompts through group settings"),
    PROMPT_UPDATED("提示词已更新", "Prompt updated"),
    PROMPT_RESET("提示词已重置", "Prompt reset"),
    PROMPT_UNKNOWN_OP("未知操作", "Unknown operation"),

    // ════ Paging ═════════════════

    PAGE_HEADER("[%s] 🔍 共 %d 个结果", "[%s] 🔍 %d results"),
    PAGE_FOOTER("""
            [第 %d/%d 页，每页 %d 条]
            操作：Up / Down / End
            选择：发送序号""", """
            [Page %d/%d, %d per page]
            Controls: Up / Down / End
            Select: send the number"""),
    PAGE_TOP("💦 已经到顶了", "💦 Already at the top"),
    PAGE_BOTTOM("💦 已经到底了", "💦 Already at the bottom"),
    PAGE_END("⛔️ 查询结束", "⛔️ Query ended"),
    PAGE_TIMEOUT("⏳ 操作超时，查询结束", "⏳ Timed out; query ended"),
    PAGE_INVALID("❌ 无效序号：%s", "❌ Invalid index: %s"),

    // ════ Durations ═══════════════

    DURATION_LESS_MINUTE("1 分钟以内", "under 1 min"),
    DURATION_MINUTES("%d 分钟", "%d min"),
    DURATION_HOURS("%d 小时", "%d hr"),
    DURATION_HOURS_MINUTES("%d 小时 %d 分钟", "%d hr %d min"),
    DURATION_DAYS("%d 天", "%d d"),
    DURATION_DAYS_HOURS("%d 天 %d 小时", "%d d %d hr"),

    // ════ Cmd Disable ═════════════

    DISABLE_MISSING_USER("私聊中必须指定 --user", "Private chat requires --user"),
    DISABLE_EMPTY_USER("用户 ID 不能为空", "User ID must not be blank"),
    DISABLE_MULTIPLE_TARGETS("只能指定一个目标用户", "Only one target user is allowed"),
    DISABLE_CONFLICT("不能同时使用 -B 和 -U", "Use either -B or -U, not both"),
    DISABLE_UNEXPECTED_ARG("不支持的参数：%s", "Unsupported argument: %s"),
    DISABLE_MALFORMED_TEMP("临时时长格式错误：%s（示例：30m、2h、7d）",
            "Malformed duration: %s (examples: 30m, 2h, 7d)"),
    DISABLE_TEMP_WITH_UNBAN("解除时不能指定 --temp", "Cannot use --temp with -U"),
    DISABLE_TARGET_GROUP("整群", "whole group"),
    DISABLE_TARGET_USER("用户 %s", "user %s"),
    DISABLE_PERMANENT("永久", "permanent"),
    DISABLE_REMAINING("剩余 %s", "%s remaining"),
    DISABLE_ADDED("✅ 已禁用 %s（%s，%s）", "✅ Disabled %s (%s, %s)"),
    DISABLE_REMOVED("✅ 已启用 %s（%s）", "✅ Enabled %s (%s)"),
    DISABLE_NOT_FOUND("没有找到对应的禁用规则", "No matching disable rule"),
    DISABLE_NOTICE_GROUP("🚫 该指令在本群已被禁用（%s）",
            "🚫 This command is disabled in this group (%s)"),
    DISABLE_NOTICE_USER("🚫 你已被禁止使用该指令（%s）",
            "🚫 You are blocked from using this command (%s)"),

    // ════ Setting ════════════════

    SETTING_UNKNOWN_OPTION("未知选项", "Unknown option"),
    SETTING_UNKNOWN_LIMIT("未知限速项：%s", "Unknown limit setting: %s"),
    SETTING_UNKNOWN_AI("未知 AI 项：%s", "Unknown AI setting: %s"),
    SETTING_MALFORMED_BOOLEAN("布尔值格式错误：%s", "Malformed boolean: %s"),
    SETTING_POSITIVE_VALUE("值必须为正数：%d", "Value must be positive: %d"),
    SETTING_FREQUENCY_RANGE("频率必须在 0~1 之间：%s", "Frequency must be between 0 and 1: %s"),
    SETTING_UNKNOWN_SCOPE("未知范围：%s", "Unknown scope: %s"),
    SETTING_UNKNOWN_EFFORT("未知强度：%s", "Unknown effort: %s"),
    SETTING_UNKNOWN_LANGUAGE("未知语言：%s（可选 auto|zh|en）", "Unknown language: %s (expected auto|zh|en)"),
    SETTING_UPDATED("✅ 设置已更新：%s", "✅ Setting updated: %s"),
    SETTING_LANGUAGE("language = %s", "language = %s"),
    SETTING_NOTICE_JOIN("入群通知 = %s", "Join notice = %s"),
    SETTING_NOTICE_LEAVE("退群通知 = %s", "Leave notice = %s"),
    SETTING_UNKNOWN_NOTICE("未知通知项：%s", "Unknown notice setting: %s"),
    SETTING_ANTI_RECALL("防撤回 = %s", "Anti-recall = %s"),
    SETTING_LIMIT_ENABLED("限速启用 = %s", "limit enabled = %s"),
    SETTING_LIMIT_SCOPE("限速范围 = %s", "limit scope = %s"),
    SETTING_LIMIT_CAPACITY("限速容量 = %s", "limit capacity = %s"),
    SETTING_LIMIT_REFILL("限速补充 = %s", "limit refill = %s"),
    SETTING_LIMIT_INTERVAL("限速间隔 = %s", "limit interval = %s"),
    SETTING_AI_EFFORT("AI 强度 = %s", "AI effort = %s"),
    SETTING_AI_PROMPT_MISSING("缺少提示词", "Missing prompt"),
    SETTING_AI_PROMPT_RESET("AI 提示词已重置为默认", "AI prompt reset to default"),
    SETTING_AI_PROMPT_UPDATED("AI 提示词已更新", "AI prompt updated"),
    SETTING_AI_GUARD("AI 防注入 = %s", "AI injection guard = %s"),
    SETTING_AI_AUTO_CHAT("AI 自动回复 = %s", "AI auto reply = %s"),
    SETTING_AI_CHAT_FREQUENCY("AI 自动回复频率 = %s", "AI auto reply frequency = %s"),
    SETTING_VIEW("""
            【设置：%s】
            语言：%s
            通知：入群=%s, 退群=%s
            防撤回：%s
            限速：enabled=%s, scope=%s, capacity=%s, refill=%s, interval=%s
            AI：effort=%s, guard=%s, auto=%s, frequency=%s
            提示词：%s""", """
            [Settings: %s]
            Language: %s
            Notices: join=%s, leave=%s
            Anti-recall: %s
            Limit: enabled=%s, scope=%s, capacity=%s, refill=%s, interval=%s
            AI: effort=%s, guard=%s, auto=%s, frequency=%s
            Prompt: %s"""),
    SETTING_LANG_AUTO("auto（全局：%s）", "auto (global: %s)"),
    SETTING_DEFAULT("（默认）", "(default)");

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
