package com.zincoid.game.cmd;

import com.zincoid.game.GameText;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdInfoException;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.cmd.input.InputMode;
import com.zincoid.nullbot.bot.cmd.input.InputResult;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.model.ModelReq;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import com.zincoid.nullbot.platform.exception.PlatformException;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 问答指令：生成一道单选题，等待指定用户作答。
 * <p>Quiz command: generates a multiple-choice question and waits for the target user's answer.</p>
 */
@Slf4j
@CmdMapping({"Quiz", "问答"})
@RequiredArgsConstructor
public class QuizCmd implements Cmd {

    private static final int MAX_TOKENS = 2048;
    private static final int MIN_TIMEOUT = 5;
    private static final int MAX_TIMEOUT = 120;

    private static final String PROMPT = """
            Create a multiple-choice question with exactly one correct answer.
            Topic: %s.
            Reply strictly in the following JSON format and nothing else:
            ```
            {
              "answer": "A",
              "timeout": 30,
              "question": "Question text..."
            }
            ```
            Notes:
            1. Set timeout by difficulty: easy 30s, medium 60s, hard 120s.
            2. Do not use LaTeX for formulas.
            3. Write the question and options in %s.""";

    private final PluginContext context;

    private final Set<String> inGame = ConcurrentHashMap.newKeySet();

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        String userId = event.userId();
        if (!inGame.add(userId))
            throw new CmdInfoException(GameText.QUIZ_ALREADY_IN_GAME.of(ctx.language()));
        try {
            String topic = args.rest(GameText.QUIZ_DEFAULT_TOPIC.of(ctx.language())).trim();
            ctx.send(GameText.QUIZ_GENERATING.of(ctx.language()));
            QuizData data = generate(ctx, topic);
            String question = GameText.QUIZ_QUESTION.of(
                    ctx.language(), data.question(), data.timeout());
            ctx.send(MsgContent.builder().at(userId).text(question).build());
            List<InputResult> inputs = context.inputs().request(
                    InputMode.PS, userId, "[A-Za-z]", data.timeout());
            if (inputs.isEmpty()) {
                ctx.send(GameText.QUIZ_TIMEOUT.of(
                        ctx.language(), event.nickname(), data.answer()));
                return;
            }
            if (data.answer().equalsIgnoreCase(inputs.getFirst().message()))
                ctx.send(GameText.QUIZ_CORRECT.of(ctx.language(), event.nickname()));
            else ctx.send(GameText.QUIZ_WRONG.of(
                    ctx.language(), event.nickname(), data.answer()));
        } finally {
            inGame.remove(userId);
        }
    }

    // ════ Helpers ═════════════════

    private QuizData generate(CmdContext ctx, String topic) {
        String language = GameText.QUIZ_LANGUAGE_NAME.of(ctx.language());
        ModelRes res;
        try {
            res = context.model().invoke(ModelReq.of(
                    List.of(AiMessage.system(PROMPT.formatted(topic, language))),
                    Effort.HIGH,
                    MAX_TOKENS));
        } catch (RuntimeException e) {
            log.warn("Quiz generation failed: {}", e.getMessage());
            throw new CmdWarnException(GameText.QUIZ_GENERATE_FAILED.of(ctx.language()));
        }
        String raw = res.getContent();
        if (raw == null || raw.isBlank())
            throw new CmdWarnException(GameText.QUIZ_EMPTY_RESPONSE.of(ctx.language()));
        try {
            QuizData data = JsonUtils.parse(extractJson(ctx, raw), QuizData.class);
            String answer = data.answer() == null
                    ? "" : data.answer().trim().toUpperCase(Locale.ROOT);
            String question = data.question() == null ? "" : data.question().trim();
            if (!answer.matches("[A-Z]") || question.isEmpty())
                throw new CmdWarnException(GameText.QUIZ_MALFORMED_RESPONSE.of(ctx.language()));
            int timeout = Math.clamp(data.timeout(), MIN_TIMEOUT, MAX_TIMEOUT);
            return new QuizData(answer, timeout, question);
        } catch (PlatformException e) {
            throw new CmdWarnException(GameText.QUIZ_MALFORMED_RESPONSE.of(ctx.language()));
        }
    }

    private static String extractJson(CmdContext ctx, String raw) {
        int start = raw.indexOf('{');
        if (start < 0) throw new CmdWarnException(GameText.QUIZ_MALFORMED_RESPONSE.of(ctx.language()));
        return raw.substring(start);
    }

    private record QuizData(String answer, int timeout, String question) {
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return GameText.QUIZ_HELP.of(language);
    }
}
