package com.zincoid.nullbot.bot.cmd.impl.game;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdInfoException;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.module.ai.message.AiMessage;
import com.zincoid.nullbot.module.ai.model.Model;
import com.zincoid.nullbot.module.ai.model.ModelReq;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.bot.input.InputManager;
import com.zincoid.nullbot.bot.input.InputMode;
import com.zincoid.nullbot.bot.input.InputResult;
import com.zincoid.nullbot.platform.exception.PlatformException;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
@Component
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

    private final Model model;
    private final InputManager inputManager;
    private final Set<String> inGame = ConcurrentHashMap.newKeySet();

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        String userId = event.userId();
        if (!inGame.add(userId))
            throw new CmdInfoException(ctx.render(Text.QUIZ_ALREADY_IN_GAME));
        try {
            String topic = args.rest(ctx.render(Text.QUIZ_DEFAULT_TOPIC)).trim();
            ctx.send(Text.QUIZ_GENERATING);
            QuizData data = generate(ctx, topic);
            String question = ctx.render(Text.QUIZ_QUESTION, data.question(), data.timeout());
            ctx.send(MsgContent.builder().at(userId).text(question).build());
            List<InputResult> inputs = inputManager.request(
                    InputMode.PS, userId, "[A-Za-z]", data.timeout());
            if (inputs.isEmpty()) {
                ctx.send(Text.QUIZ_TIMEOUT, event.nickname(), data.answer());
                return;
            }
            if (data.answer().equalsIgnoreCase(inputs.getFirst().message()))
                ctx.send(Text.QUIZ_CORRECT, event.nickname());
            else ctx.send(Text.QUIZ_WRONG, event.nickname(), data.answer());
        } finally {
            inGame.remove(userId);
        }
    }

    // ════ Helpers ═════════════════

    private QuizData generate(CmdContext ctx, String topic) {
        String language = ctx.language() == Language.EN ? "English" : "Chinese";
        ModelRes res;
        try {
            res = model.invoke(ModelReq.of(
                    List.of(AiMessage.system(PROMPT.formatted(topic, language))),
                    Effort.HIGH,
                    MAX_TOKENS));
        } catch (RuntimeException e) {
            log.warn("Quiz generation failed: {}", e.getMessage());
            throw new CmdWarnException(ctx.render(Text.QUIZ_GENERATE_FAILED));
        }
        String raw = res.getContent();
        if (raw == null || raw.isBlank())
            throw new CmdWarnException(ctx.render(Text.QUIZ_EMPTY_RESPONSE));
        try {
            QuizData data = JsonUtils.parse(extractJson(ctx, raw), QuizData.class);
            String answer = data.answer() == null
                    ? "" : data.answer().trim().toUpperCase(Locale.ROOT);
            String question = data.question() == null ? "" : data.question().trim();
            if (!answer.matches("[A-Z]") || question.isEmpty())
                throw new CmdWarnException(ctx.render(Text.QUIZ_MALFORMED_RESPONSE));
            int timeout = Math.clamp(data.timeout(), MIN_TIMEOUT, MAX_TIMEOUT);
            return new QuizData(answer, timeout, question);
        } catch (PlatformException e) {
            throw new CmdWarnException(ctx.render(Text.QUIZ_MALFORMED_RESPONSE));
        }
    }

    private static String extractJson(CmdContext ctx, String raw) {
        int start = raw.indexOf('{');
        if (start < 0) throw new CmdWarnException(ctx.render(Text.QUIZ_MALFORMED_RESPONSE));
        return raw.substring(start);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                Quiz
                Starts a single-player quiz.
                Usage: Quiz [topic]
                Aliases: 问答
                Access: >=0(normal)""" : """
                Quiz
                开始一轮单人问答。
                用法：Quiz [topic]
                别名：问答
                权限：>=0(normal)""";
    }

    private record QuizData(String answer, int timeout, String question) {
    }
}
