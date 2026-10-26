package com.zincoid.nullbot.module.render.svg;

import com.zincoid.nullbot.module.render.RenderException;
import io.brunoborges.jairosvg.JairoSVG;
import org.springframework.stereotype.Component;

import java.awt.RenderingHints;

/**
 * 基于 JairoSVG 的纯 Java SVG 栅格化实现；JairoSVG 内部有共享缓存，因此串行执行。
 * <p>Pure-Java SVG rasterizer backed by JairoSVG; rasterization is serialized because JairoSVG keeps shared caches.</p>
 */
@Component
public class JairoSvgRasterizer implements SvgRasterizer {

    @Override
    public synchronized byte[] png(String svg) {
        if (svg == null || svg.isBlank()) throw new RenderException("Missing key: svg");
        try {
            // Fractional metrics keeps monospaced advances consistent; integer metrics round some glyphs to 8/10px.
            return JairoSVG.builder()
                    .renderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                            RenderingHints.VALUE_FRACTIONALMETRICS_ON)
                    .fromString(svg)
                    .toPng();
        } catch (Exception e) {
            throw new RenderException("Failed to rasterize SVG", e);
        }
    }
}
