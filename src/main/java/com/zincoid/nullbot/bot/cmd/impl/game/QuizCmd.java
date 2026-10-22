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
import com.zincoid.nullbot.module.system.InputManager;
import com.zincoid.nullbot.module.system.InputMode;
import com.zincoid.nullbot.module.system.InputResult;
import com.zincoid.nullbot.platform.exception.PlatformException;
import com.zincoid.nullbot.platform.persistence.enums.Effort;
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
    private static final String DEFAULT_TOPIC = "二次元";

    private static final String PROMPT = """
            Create a multiple-choice question with exactly one correct answer. Topic: %s.
            Reply strictly in the following JSON format and nothing else:
            ```
            {
              "answer": "A",
              "timeout": 30,
              "question": "Question text?\\nA. option one\\nB. option two"
            }
            ```
            Notes:
            1. Set timeout by difficulty: easy 15-30s, medium 45-60s, hard 90-120s.
            2. Do not use LaTeX for formulas.
            3. Write the question and options in the same language as the topic.
            4. Do not generate questions about Chinese domestic political events or figures.""";

    private final Model model;
    private final InputManager inputManager;
    private final Set<String> inGame = ConcurrentHashMap.newKeySet();

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        String userId = event.userId();
        if (!inGame.add(userId))
            throw new CmdInfoException("You are already in a quiz");
        try {
            String topic = args.rest(DEFAULT_TOPIC).trim();
            ctx.send("⏳ Generating question...");
            QuizData data = generate(topic);
            ctx.send(MsgContent.builder()
                    .at(userId)
                    .text("""
                            Answer the question!!!
                            %s
                            
                            Send the option letter.
                            Time limit: %ss.""".formatted(data.question(), data.timeout()))
                    .build());
            List<InputResult> inputs = inputManager.request(
                    InputMode.PS, userId, "[A-Za-z]", data.timeout());
            if (inputs.isEmpty()) {
                ctx.send("%s timed out! The answer is %s."
                        .formatted(event.nickname(), data.answer()));
            } else if (data.answer().equalsIgnoreCase(inputs.getFirst().message())) {
                ctx.send("%s answered correctly!".formatted(event.nickname()));
            } else {
                ctx.send("%s answered wrong! The answer is %s."
                        .formatted(event.nickname(), data.answer()));
            }
        } finally {
            inGame.remove(userId);
        }
    }

    // ════ Helpers ═════════════════

    private QuizData generate(String topic) {
        ModelRes res;
        try {
            res = model.invoke(ModelReq.of(
                    List.of(AiMessage.system(PROMPT.formatted(topic))),
                    Effort.HIGH,
                    MAX_TOKENS));
        } catch (RuntimeException e) {
            log.warn("Quiz generation failed: {}", e.getMessage());
            throw new CmdWarnException("Failed to generate question");
        }
        String raw = res.getContent();
        if (raw == null || raw.isBlank())
            throw new CmdWarnException("Empty response");
        try {
            QuizData data = JsonUtils.parse(json(raw), QuizData.class);
            String answer = data.answer() == null
                    ? ""
                    : data.answer().trim().toUpperCase(Locale.ROOT);
            String question = data.question() == null ? "" : data.question().trim();
            if (!answer.matches("[A-Z]") || question.isEmpty())
                throw new CmdWarnException("Malformed response");
            int timeout = Math.clamp(data.timeout(), MIN_TIMEOUT, MAX_TIMEOUT);
            return new QuizData(answer, timeout, question);
        } catch (PlatformException e) {
            throw new CmdWarnException("Malformed response");
        }
    }

    private static String json(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start)
            throw new CmdWarnException("Malformed response");
        return raw.substring(start, end + 1);
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp() {
        return """
                Quiz
                Starts a single-player quiz.
                Usage: Quiz [topic]
                Aliases: 问答
                Access: >=0(normal)""";
    }

    private record QuizData(String answer, int timeout, String question) {
    }
}
