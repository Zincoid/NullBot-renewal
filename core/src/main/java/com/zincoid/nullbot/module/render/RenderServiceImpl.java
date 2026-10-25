package com.zincoid.nullbot.module.render;

import com.zincoid.nullbot.platform.util.MimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Locale;
import java.util.Map;

/**
 * {@link RenderService} 的默认实现。
 * <p>Default implementation of {@link RenderService}.</p>
 */
@Service
@RequiredArgsConstructor
public class RenderServiceImpl implements RenderService {

    private final JteSvgRenderer renderer;

    // ════ Usage ═══════════════════

    @Override
    public byte[] usage(byte[] avatar, long times) {
        if (avatar == null || avatar.length == 0) throw new RenderException("Missing key: avatar");
        if (times < 0) throw new RenderException("Invalid usage count: " + times);
        String mime = MimeUtils.mime(avatar);
        if (mime == null || !mime.startsWith("image/"))
            throw new RenderException("Unsupported avatar image");
        String dataUri = "data:" + mime + ";base64,"
                + Base64.getEncoder().encodeToString(avatar);
        return renderer.template("usage.jte", Map.of(
                "avatar", dataUri,
                "times", String.format(Locale.ROOT, "%08d", times)
        ));
    }

    // ════ Template ════════════════

    @Override
    public byte[] template(String template, Map<String, Object> model) {
        return renderer.template(template, model);
    }
}
