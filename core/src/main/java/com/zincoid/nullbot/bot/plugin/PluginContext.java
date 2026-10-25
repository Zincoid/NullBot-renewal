package com.zincoid.nullbot.bot.plugin;

import com.zincoid.nullbot.bot.cmd.input.InputManager;
import com.zincoid.nullbot.module.ai.model.Model;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.i18n.LanguageService;
import com.zincoid.nullbot.module.render.PluginRenderer;
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
     * 配置存储服务。
     * <p>Configuration store service.</p>
     *
     * @return the config service
     */
    ConfigService config();

    /**
     * 输入等待管理器。
     * <p>Input wait manager.</p>
     *
     * @return the input manager
     */
    InputManager inputs();

    /**
     * 模型调用端口。
     * <p>Model invocation port.</p>
     *
     * @return the model
     */
    Model model();

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
     * 插件图片渲染器，可渲染原始 SVG 或预编译 JTE 模板。
     * <p>Plugin image renderer that renders raw SVG or precompiled JTE templates.</p>
     *
     * @return the plugin renderer
     */
    PluginRenderer renderer();

    /**
     * 插件专用数据目录，由宿主创建。
     * <p>Plugin-specific data directory, created by the host.</p>
     *
     * @return the plugin data directory
     */
    Path dataDir();
}
