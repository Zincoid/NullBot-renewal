package com.zincoid.game;

import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.Locale;

/**
 * Game 插件文案：中文与英文成对保存。
 * <p>Game plugin texts: Chinese and English stored in pairs.</p>
 */
public enum GameText {

    // ════ Guess ═════════════════

    GUESS_HELP("""
            Guess
            多人猜图。
            用法：Guess [关键词]
            选项：-f/--forfeit  放弃游戏
            说明：按分类或角色名精确随机抽取图片并发送裁剪图；回答格式为 %s答案，发送 %s%s 放弃。
            别名：猜
            权限：>=0(normal)""", """
            Guess
            Plays a multiplayer image-guessing game.
            Usage: Guess [keyword]
            Options: -f/--forfeit  forfeit the game
            Notes: picks a random image by exact category or character name and sends a cropped part; answer with "%sanswer" or give up with "%s%s".
            Aliases: 猜
            Access: >=0(normal)"""),
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

    // ════ Quiz ══════════════════

    QUIZ_HELP("""
            Quiz
            开始一轮单人问答。
            用法：Quiz [topic]
            别名：问答
            权限：>=0(normal)""", """
            Quiz
            Starts a single-player quiz.
            Usage: Quiz [topic]
            Aliases: 问答
            Access: >=0(normal)"""),
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
    QUIZ_LANGUAGE_NAME("Chinese", "English"),

    // ════ Wife ══════════════════

    WIFE_HELP("""
            Wife
            今日老婆。
            用法：Wife [关键词]
            选项：-m/--member=<用户ID>  指定群友老婆（需 manager）
                  -a/--anime=<关键词>  指定二次元老婆（需 manager）
            说明：无参数随机群友；带关键词按分类或角色名精确匹配。
            别名：今日老婆
            权限：>=0(normal)""", """
            Wife
            Picks today's wife.
            Usage: Wife [keyword]
            Options: -m/--member=<user id> picks a group member (manager)
                     -a/--anime=<keyword> picks an anime wife (manager)
            Notes: no argument picks a random group member; a keyword exactly matches a category or character name.
            Aliases: 今日老婆
            Access: >=0(normal)"""),
    WIFE_MEMBER_ALREADY("\n今天已经选过了哦💦...\n你的群友老婆是\n%s(%s)",
            "\nAlready picked today💦...\nYour group wife is\n%s(%s)"),
    WIFE_MEMBER_PICKED("\n你的今日群友老婆是✨\n%s(%s)",
            "\nYour group wife of the day is✨\n%s(%s)"),
    WIFE_ANIME_ALREADY("\n今天已经选过了哦💦...\n你的二次元老婆是\n%s",
            "\nAlready picked today💦...\nYour anime wife is\n%s"),
    WIFE_ANIME_PICKED("\n你的今日二次元老婆是✨\n%s - %s",
            "\nYour anime wife of the day is✨\n%s - %s"),
    WIFE_NO_RESULT("暂无角色", "No character found"),
    WIFE_NO_MEMBERS("群成员不足", "Not enough group members"),
    WIFE_NOT_ALLOWED("你不能用。", "You cannot use this option");

    private final String zh;
    private final String en;

    GameText(String zh, String en) {
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
