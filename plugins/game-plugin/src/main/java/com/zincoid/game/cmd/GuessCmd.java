package com.zincoid.game.cmd;

import com.zincoid.game.GameText;
import com.zincoid.game.service.GameResources;
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
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 猜图指令：从共享资源目录按分类或角色随机抽取图片，裁剪后由群成员抢答。
 * <p>Guess command: draws a random image by category or character from the shared resource directory, crops it, and lets group members guess.</p>
 */
@Slf4j
@CmdMapping({"Guess", "猜"})
@RequiredArgsConstructor
public class GuessCmd implements Cmd {

    private static final int WAIT_TIMEOUT_SECONDS = 99;
    private static final int MAX_RETRIES = 10;
    private static final int MAX_CROP_ATTEMPTS = 100;
    private static final double CROP_RATIO = 0.1;
    private static final int CROP_PADDING = 250;
    private static final double TRANSPARENT_RATIO = 0.75;
    private static final List<String> ALIASES = List.of("Guess", "猜");

    private final PluginContext context;
    private final GameResources resources;

    private final Map<String, Guess> games = new ConcurrentHashMap<>();

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        String groupId = event.groupId();
        String prefix = context.config().getString(Conf.CMD_PREFIX);
        if (args.hasOpt("forfeit", "f")) {
            if (!games.containsKey(groupId))
                throw new CmdInfoException(GameText.GUESS_NOT_PLAYING.of(ctx.language()));
            context.inputs().cancelWait(InputMode.GS, groupId);
            log.info("Guess forfeited: group={}", groupId);
            return;
        }
        if (games.containsKey(groupId))
            throw new CmdInfoException(GameText.GUESS_ALREADY_PLAYING.of(ctx.language()));

        Guess guess = create(ctx, args);
        if (games.putIfAbsent(groupId, guess) != null)
            throw new CmdInfoException(GameText.GUESS_ALREADY_PLAYING.of(ctx.language()));
        try {
            ctx.send(MsgContent.builder()
                    .text(GameText.GUESS_START.of(ctx.language()))
                    .imageBase64(guess.cropped)
                    .text(GameText.GUESS_HINT.of(ctx.language(), prefix, prefix, prefix))
                    .build());
            log.info("Guess started: group={}, answer={}", groupId, guess.answer);
            play(ctx, groupId, guess, prefix);
        } finally {
            games.remove(groupId);
        }
    }

    // ════ Game ════════════════════

    private void play(CmdContext ctx, String groupId, Guess guess, String prefix) {
        String forfeit = prefix + prefix;
        String pattern = inputPattern(prefix);
        while (guess.times < MAX_RETRIES) {
            guess.times++;
            List<InputResult> inputs = context.inputs().request(
                    InputMode.GS, groupId, pattern, WAIT_TIMEOUT_SECONDS);
            if (inputs.isEmpty() || forfeit.equals(inputs.getFirst().message().trim())) {
                end(ctx, groupId, guess,
                        GameText.GUESS_END.of(ctx.language(), guess.answer));
                return;
            }
            InputResult input = inputs.getFirst();
            String answer = input.message().substring(prefix.length()).trim();
            if (guess.answer.equals(answer)) {
                ctx.send(MsgContent.builder()
                        .text(GameText.GUESS_CORRECT.of(ctx.language(),
                                nickname(ctx, input.userId()), guess.answer, guess.times))
                        .image(guess.resource.path().toUri().toString())
                        .build());
                log.info("Guess correct: group={}, user={}", groupId, input.userId());
                return;
            }
            ctx.send(MsgContent.builder()
                    .at(input.userId())
                    .text(GameText.GUESS_WRONG.of(ctx.language()))
                    .build());
            log.info("Guess wrong: group={}, user={}", groupId, input.userId());
        }
        end(ctx, groupId, guess,
                GameText.GUESS_FAIL.of(ctx.language(), MAX_RETRIES, guess.answer));
    }

    private void end(CmdContext ctx, String groupId, Guess guess, String text) {
        ctx.send(MsgContent.builder()
                .text(text)
                .image(guess.resource.path().toUri().toString())
                .build());
        log.info("Guess ended: group={}, answer={}", groupId, guess.answer);
    }

    // ════ Create ══════════════════

    private Guess create(CmdContext ctx, CmdArgs args) {
        String keyword = args.rest("").trim();
        GameResources.Resource resource = resources.random(keyword);
        if (resource == null)
            throw new CmdInfoException(GameText.GUESS_NO_IMAGES.of(ctx.language()));
        return new Guess(resource, resource.character(), crop(ctx, resource));
    }

    // ════ Crop ════════════════════

    private String crop(CmdContext ctx, GameResources.Resource resource) {
        BufferedImage image;
        try {
            image = ImageIO.read(resource.path().toFile());
        } catch (IOException | RuntimeException e) {
            throw new CmdWarnException(GameText.GUESS_CROP_FAILED.of(ctx.language()));
        }
        if (image == null)
            throw new CmdWarnException(GameText.GUESS_CROP_FAILED.of(ctx.language()));

        int width = image.getWidth();
        int height = image.getHeight();
        int pad = Math.min(CROP_PADDING, Math.min(width, height) / 4);
        int w = Math.clamp((int) (width * CROP_RATIO), 1, width - 2 * pad);
        int h = Math.clamp((int) (height * CROP_RATIO), 1, height - 2 * pad);
        int xMax = Math.max(pad, width - w - pad);
        int yMax = Math.max(pad, height - h - pad);

        if (!image.getColorModel().hasAlpha())
            return encode(ctx, image.getSubimage(
                    random(pad, xMax), random(pad, yMax), w, h));

        for (int attempt = 0; attempt < MAX_CROP_ATTEMPTS; attempt++) {
            BufferedImage sub = image.getSubimage(
                    random(pad, xMax), random(pad, yMax), w, h);
            if (transparentRatio(sub) <= TRANSPARENT_RATIO)
                return encode(ctx, sub);
        }
        throw new CmdWarnException(GameText.GUESS_CROP_FAILED.of(ctx.language()));
    }

    private static String encode(CmdContext ctx, BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", out))
                throw new IOException("No PNG writer");
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new CmdWarnException(GameText.GUESS_CROP_FAILED.of(ctx.language()));
        }
    }

    private static int random(int min, int max) {
        return max <= min ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static double transparentRatio(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        int transparent = 0;
        for (int pixel : pixels)
            if (((pixel >>> 24) & 0xff) == 0) transparent++;
        return (double) transparent / (width * height);
    }

    // ════ Helpers ═════════════════

    private static String inputPattern(String prefix) {
        String quoted = Pattern.quote(prefix);
        String aliases = String.join("|", ALIASES.stream().map(Pattern::quote).toList());
        String cmd = quoted + "(?i:" + aliases + ")(?:\\s|$)";
        return "(?!" + cmd + ")" + quoted + ".+";
    }

    private static String nickname(CmdContext ctx, String userId) {
        try {
            return ctx.client().getUserInfo(userId).nickname();
        } catch (RuntimeException e) {
            return userId;
        }
    }

    private static final class Guess {

        private final GameResources.Resource resource;
        private final String answer;
        private final String cropped;
        private int times;

        private Guess(GameResources.Resource resource, String answer, String cropped) {
            this.resource = resource;
            this.answer = answer;
            this.cropped = cropped;
        }
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        String prefix = context.config().getString(Conf.CMD_PREFIX);
        return GameText.GUESS_HELP.of(language, prefix, prefix, prefix);
    }
}
