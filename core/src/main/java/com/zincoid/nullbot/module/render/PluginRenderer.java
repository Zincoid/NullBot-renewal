package com.zincoid.nullbot.module.render;

import java.util.Map;

/**
 * 插件图片渲染器：支持原始 SVG 和插件预编译 JTE 模板。
 * <p>Plugin image renderer supporting raw SVG and precompiled plugin JTE templates.</p>
 */
public interface PluginRenderer {

    /**
     * 把 SVG 渲染为 PNG。
     * <p>Renders SVG into PNG.</p>
     *
     * @param svg the SVG document
     * @return the PNG image bytes
     */
    byte[] svg(String svg);

    /**
     * 用插件预编译模板和参数渲染 PNG。
     * <p>Renders PNG with a precompiled plugin template and parameters.</p>
     *
     * @param template the template name, for example {@code card.jte}
     * @param model    the template parameters keyed by parameter name, may be null
     * @return the PNG image bytes
     */
    byte[] template(String template, Map<String, Object> model);

    /**
     * 注册 TTF/OTF 字体到 Java2D 全局环境。
     * <p>Registers a TTF/OTF font with the global Java2D environment.</p>
     *
     * @param font the font bytes
     * @return the result of Java2D font registration
     */
    boolean font(byte[] font);
}
