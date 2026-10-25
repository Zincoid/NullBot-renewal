package com.zincoid.nullbot.module.render;

import java.util.Map;

/**
 * 宿主图片渲染服务：用 JTE 模板替换数据并输出 PNG。
 * <p>Host image render service: fills JTE templates with data and produces PNG images.</p>
 */
public interface RenderService {

    // ════ Usage ═══════════════════

    /**
     * 渲染用户用量卡片。
     * <p>Renders the user usage card.</p>
     *
     * @param avatar the avatar image bytes
     * @param times  the total command usage count
     * @return the PNG image bytes
     */
    byte[] usage(byte[] avatar, long times);

    /**
     * 用指定模板和参数渲染 PNG。
     * <p>Renders a PNG image with the given template and parameters.</p>
     *
     * @param template the template name, for example {@code info.jte}
     * @param model    the template parameters keyed by parameter name, may be null
     * @return the PNG image bytes
     */
    byte[] template(String template, Map<String, Object> model);
}
