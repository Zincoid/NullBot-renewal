package com.zincoid.nullbot.bot.plugin.loader;

import com.zincoid.nullbot.bot.cmd.input.InputManager;
import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.module.ai.model.Model;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.LanguageService;
import com.zincoid.nullbot.module.render.JteSvgRenderer;
import com.zincoid.nullbot.module.render.PluginRenderer;
import com.zincoid.nullbot.module.render.svg.SvgRasterizer;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.module.setting.UserSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 插件上下文工厂：把宿主服务与插件数据目录组装为 {@link PluginContext}。
 * <p>Plugin context factory assembling host services and a plugin data directory into a {@link PluginContext}.</p>
 */
@Component
@RequiredArgsConstructor
public class PluginContextFactory {

    private final FileService files;
    private final ConfigService config;
    private final InputManager inputs;
    private final Model model;
    private final UserService users;
    private final GroupService groups;
    private final GroupSettingService groupSettings;
    private final UserSettingService userSettings;
    private final QuoteService quotes;
    private final LanguageService languages;
    private final SvgRasterizer rasterizer;

    /**
     * 为指定插件创建上下文。
     * <p>Creates the context for the given plugin.</p>
     *
     * @param dataDir the plugin data directory
     * @param loader  the plugin classloader
     * @param jar     the plugin jar containing precompiled templates
     * @return the plugin context
     */
    PluginContext create(Path dataDir, ClassLoader loader, Path jar) {
        PluginRenderer renderer = JteSvgRenderer.forPlugin(loader, jar, rasterizer);
        return new DefaultPluginContext(
                files, config, inputs, model, users, groups,
                groupSettings, userSettings,
                quotes,
                languages,
                renderer,
                dataDir
        );
    }
}
