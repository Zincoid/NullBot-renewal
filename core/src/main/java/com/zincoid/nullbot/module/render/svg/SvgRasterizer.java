package com.zincoid.nullbot.module.render.svg;

/**
 * SVG 栅格化端口：把 SVG 文本转换为 PNG。
 * <p>SVG rasterization port: converts SVG text into PNG.</p>
 */
public interface SvgRasterizer {

    /**
     * 把 SVG 文本渲染为 PNG。
     * <p>Renders SVG text into PNG.</p>
     *
     * @param svg the SVG document
     * @return the PNG image bytes
     */
    byte[] png(String svg);
}
