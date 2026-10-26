package com.zincoid.symmetry;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import com.zincoid.nullbot.bot.spi.model.UserInfo;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * 对称指令：把图片的一半镜像到另一侧。
 * <p>Symmetry command: mirrors one half of an image onto the other side.</p>
 */
@Slf4j
@CmdMapping({"Symmetry", "图像对称", "对称"})
public class SymmetryCmd implements Cmd {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_IMAGE_BYTES = 32 * 1024 * 1024;
    private static final String USER_AGENT = "NullBot-Symmetry/1.0";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, event.content(), args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, event.content(), args);
    }

    private void handle(CmdContext ctx, MsgContent content, CmdArgs args) {
        Mode mode = Mode.LEFT;
        if (args.hasNext()) {
            Mode parsed = Mode.parse(args.peek());
            if (parsed != null) {
                mode = parsed;
                args.next();
            }
        }
        String replyId = content.replyId();
        List<String> sources = new ArrayList<>();
        if (replyId != null) sources.addAll(replyImages(ctx, replyId));
        for (MsgSegment segment : content.segments()) {
            if (segment instanceof MsgSegment.Image image
                    && image.url() != null && !image.url().isBlank())
                sources.add(image.url());
        }
        if (args.hasNext()) {
            long userId = args.nextLong();
            sources.add(avatar(ctx, Long.toString(userId)));
        } else {
            for (String userId : content.atIds()) {
                if (userId != null && !userId.isBlank())
                    sources.add(avatar(ctx, userId));
            }
        }
        if (sources.isEmpty()) {
            throw new CmdWarnException((replyId != null
                    ? SymmetryText.NO_IMAGE_IN_REPLY
                    : SymmetryText.MISSING_SOURCE).of(ctx.language()));
        }
        int sent = 0;
        for (String source : sources) {
            try {
                ctx.send(MsgContent.builder()
                        .imageBase64(render(source, mode))
                        .build());
                sent++;
            } catch (SymmetryException e) {
                log.warn("Symmetry image failed: {}", e.getMessage());
            }
        }
        if (sent == 0)
            throw new CmdWarnException(SymmetryText.IMAGE_FAILED.of(ctx.language()));
        log.info("Symmetry sent: mode={}, images={}", mode, sent);
    }

    // ════ Helpers ═════════════════

    private static List<String> replyImages(CmdContext ctx, String replyId) {
        MsgDetail detail;
        try {
            detail = ctx.client().getMsg(replyId);
        } catch (RuntimeException e) {
            log.warn("Cannot read replied message: {}", replyId, e);
            throw new CmdWarnException(SymmetryText.READ_FAILED.of(ctx.language()));
        }
        return detail.content().segments().stream()
                .filter(MsgSegment.Image.class::isInstance)
                .map(MsgSegment.Image.class::cast)
                .map(MsgSegment.Image::url)
                .filter(url -> url != null && !url.isBlank())
                .toList();
    }

    private static String avatar(CmdContext ctx, String userId) {
        UserInfo info;
        try {
            info = ctx.client().getUserInfo(userId);
        } catch (RuntimeException e) {
            log.warn("Cannot fetch avatar: {}", userId, e);
            throw new CmdWarnException(SymmetryText.AVATAR_FAILED.of(ctx.language()));
        }
        if (info == null || info.avatar() == null || info.avatar().isBlank())
            throw new CmdWarnException(SymmetryText.NO_AVATAR.of(ctx.language()));
        return info.avatar();
    }

    private static String render(String source, Mode mode) {
        BufferedImage sourceImage = decode(fetch(source));
        BufferedImage result = symmetry(sourceImage, mode);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(result, "png", out))
                throw new SymmetryException("No PNG writer");
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new SymmetryException("PNG encoding failed", e);
        }
    }

    private static BufferedImage decode(byte[] bytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new SymmetryException("Unsupported image format");
            return image;
        } catch (IOException e) {
            throw new SymmetryException("Image decoding failed", e);
        }
    }

    private static byte[] fetch(String source) {
        try {
            if (source.startsWith(MsgContent.BASE64_PREFIX))
                return decodeBase64(source.substring(MsgContent.BASE64_PREFIX.length()));
            if (source.startsWith("data:")) {
                int comma = source.indexOf(',');
                if (comma < 0) throw new SymmetryException("Malformed data URI");
                return decodeBase64(source.substring(comma + 1));
            }
            if (source.startsWith("http://") || source.startsWith("https://")) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(source))
                        .timeout(REQUEST_TIMEOUT)
                        .header("User-Agent", USER_AGENT)
                        .GET()
                        .build();
                HttpResponse<byte[]> response = CLIENT.send(request,
                        HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200)
                    throw new SymmetryException("HTTP " + response.statusCode());
                byte[] body = response.body();
                if (body.length > MAX_IMAGE_BYTES)
                    throw new SymmetryException("Image too large");
                return body;
            }
            if (source.startsWith("file:"))
                return Files.readAllBytes(Path.of(URI.create(source)));
            return Files.readAllBytes(Path.of(source));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SymmetryException("Image download interrupted", e);
        } catch (IOException e) {
            throw new SymmetryException("Image download failed", e);
        } catch (SymmetryException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SymmetryException("Malformed image source", e);
        }
    }

    private static byte[] decodeBase64(String value) {
        String payload = value.replaceAll("\\s+", "");
        try {
            return Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            return Base64.getUrlDecoder().decode(payload);
        }
    }

    static BufferedImage symmetry(BufferedImage source, Mode mode) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            switch (mode) {
                case LEFT -> {
                    int half = (width + 1) / 2;
                    graphics.setClip(half, 0, width - half, height);
                    graphics.drawImage(source, flipX(width), null);
                    graphics.setClip(null);
                    graphics.drawImage(source, 0, 0, half, height, 0, 0, half, height, null);
                }
                case RIGHT -> {
                    int start = width / 2;
                    graphics.setClip(0, 0, start, height);
                    graphics.drawImage(source, flipX(width), null);
                    graphics.setClip(null);
                    graphics.drawImage(source, start, 0, width, height, start, 0, width, height, null);
                }
                case TOP -> {
                    int half = (height + 1) / 2;
                    graphics.setClip(0, half, width, height - half);
                    graphics.drawImage(source, flipY(height), null);
                    graphics.setClip(null);
                    graphics.drawImage(source, 0, 0, width, half, 0, 0, width, half, null);
                }
                case BOTTOM -> {
                    int start = height / 2;
                    graphics.setClip(0, 0, width, start);
                    graphics.drawImage(source, flipY(height), null);
                    graphics.setClip(null);
                    graphics.drawImage(source, 0, start, width, height, 0, start, width, height, null);
                }
            }
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static AffineTransform flipX(int width) {
        return new AffineTransform(-1, 0, 0, 1, width, 0);
    }

    private static AffineTransform flipY(int height) {
        return new AffineTransform(1, 0, 0, -1, 0, height);
    }

    enum Mode {
        LEFT, RIGHT, TOP, BOTTOM;

        static Mode parse(String value) {
            if (value == null) return null;
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "左", "left" -> LEFT;
                case "右", "right" -> RIGHT;
                case "上", "top" -> TOP;
                case "下", "bottom" -> BOTTOM;
                default -> null;
            };
        }
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return SymmetryText.HELP.of(language);
    }
}
