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
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.module.setting.UserSettingService;

import java.nio.file.Path;

/**
 * {@link PluginContext} 的默认实现，按插件绑定独立数据目录。
 * <p>Default {@link PluginContext} implementation binding one data directory per plugin.</p>
 */
record DefaultPluginContext(
        FileService files,
        ConfigService config,
        InputManager inputs,
        Model model,
        UserService users,
        GroupService groups,
        GroupSettingService groupSettings,
        UserSettingService userSettings,
        QuoteService quotes,
        LanguageService languages,
        Path dataDir
) implements PluginContext {
}
