package com.zincoid.nullbot.module.render;

import com.zincoid.nullbot.module.render.image.ImageData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        String dataUri = ImageData.imageDataUri(avatar);
        if (dataUri.isBlank())
            throw new RenderException("Unsupported avatar image");
        return renderer.template("usage.jte", Map.of(
                "avatar", dataUri,
                "times", String.format(Locale.ROOT, "%07d", times)
        ));
    }

    // ════ Template ════════════════

    @Override
    public byte[] template(String template, Map<String, Object> model) {
        return renderer.template(template, model);
    }
}
