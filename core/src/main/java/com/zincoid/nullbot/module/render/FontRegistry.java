package com.zincoid.nullbot.module.render;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 渲染字体注册：把随包字体注册到 Java2D。
 * <p>Render font registration: registers bundled fonts with Java2D.</p>
 */
@Slf4j
@Component
public class FontRegistry {

    /**
     * 渲染字体族。
     * <p>The font families used for rendering.</p>
     */
    public static final String JETBRAINS_MAPLE_MONO = "JetBrains Maple Mono";

    private static final Map<String, String> FONTS = Map.of(
            JETBRAINS_MAPLE_MONO, "fonts/JetBrainsMapleMonoNF-Regular.ttf"
    );

    @PostConstruct
    public void register() {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        FONTS.forEach((name, resource) -> register(env, name, resource));
    }

    /**
     * 把 TTF/OTF 字体字节注册到 Java2D 全局环境。
     * <p>Registers TTF/OTF font bytes with the global Java2D environment.</p>
     *
     * @param font the font bytes
     * @return the result of Java2D font registration
     */
    public static boolean register(byte[] font) {
        if (font == null || font.length == 0) throw new RenderException("Missing key: font");
        try {
            Font awtFont = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(font));
            return register(GraphicsEnvironment.getLocalGraphicsEnvironment(),
                    awtFont, awtFont.getFamily(), null);
        } catch (IOException | FontFormatException e) {
            throw new RenderException("Failed to register font", e);
        }
    }

    private static void register(GraphicsEnvironment env, String name, String resource) {
        try (InputStream in = FontRegistry.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) throw new RenderException("Font not found: " + resource);
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            register(env, font, name, resource);
        } catch (IOException | FontFormatException e) {
            throw new RenderException("Failed to register font: " + resource, e);
        }
    }

    private static boolean register(GraphicsEnvironment env, Font font, String name, String resource) {
        boolean added = env.registerFont(font);
        if (!added) {
            log.debug("Font already registered: {}", name);
            return false;
        }
        if (resource == null) log.info("Font registered: {}", name);
        else log.info("Font registered: {} ({})", name, resource);
        return true;
    }
}
