package com.zincoid.nullbot.bot.plugin;

import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.LanguageService;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.module.setting.UserSettingService;

import java.nio.file.Path;

/**
 * 插件可用的宿主服务白名单。
 * <p>Whitelist of host services available to a plugin.</p>
 */
public interface PluginContext {

    /**
     * 文件记录与磁盘存储服务。
     * <p>File record and disk storage service.</p>
     *
     * @return the file service
     */
    FileService files();

    /**
     * 用户记录服务。
     * <p>User record service.</p>
     *
     * @return the user service
     */
    UserService users();

    /**
     * 群记录服务。
     * <p>Group record service.</p>
     *
     * @return the group service
     */
    GroupService groups();

    /**
     * 群设置服务。
     * <p>Group setting service.</p>
     *
     * @return the group setting service
     */
    GroupSettingService groupSettings();

    /**
     * 用户设置服务。
     * <p>User setting service.</p>
     *
     * @return the user setting service
     */
    UserSettingService userSettings();

    /**
     * 语录服务。
     * <p>Quote service.</p>
     *
     * @return the quote service
     */
    QuoteService quotes();

    /**
     * 回复语言服务。
     * <p>Reply language service.</p>
     *
     * @return the language service
     */
    LanguageService languages();

    /**
     * 插件专用数据目录，由宿主创建。
     * <p>Plugin-specific data directory, created by the host.</p>
     *
     * @return the plugin data directory
     */
    Path dataDir();
}
