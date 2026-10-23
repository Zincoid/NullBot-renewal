package com.zincoid.nullbot.bot.plugin.loader;

import com.zincoid.nullbot.bot.plugin.PluginContext;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.LanguageService;
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
    private final UserService users;
    private final GroupService groups;
    private final GroupSettingService groupSettings;
    private final UserSettingService userSettings;
    private final QuoteService quotes;
    private final LanguageService languages;

    /**
     * 为指定插件创建上下文。
     * <p>Creates the context for the given plugin.</p>
     *
     * @param dataDir the plugin data directory
     * @return the plugin context
     */
    PluginContext create(Path dataDir) {
        return new DefaultPluginContext(
                files, users, groups,
                groupSettings, userSettings,
                quotes,
                languages,
                dataDir
        );
    }
}
