package com.zincoid.nullbot.bot.plugin;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.module.ai.tool.Tool;

import java.util.List;

/**
 * 外部插件入口：一个 JAR 提供一个实现，通过 ServiceLoader 加载。
 * <p>External plugin entry point: one implementation per JAR, loaded through ServiceLoader.</p>
 */
public interface Plugin {

    /**
     * 插件唯一标识，用于日志和插件数据目录。
     * <p>Unique plugin id, used for logs and the plugin data directory.</p>
     *
     * @return the plugin id
     */
    String id();

    /**
     * 插件版本，默认读取插件 JAR manifest 的 {@code Implementation-Version}。
     * <p>Plugin version; by default reads {@code Implementation-Version} from the plugin JAR manifest.</p>
     * <p>从 IDE 或 classpath 运行时可能取不到版本，此时返回 {@code dev}；插件也可以覆写此方法。</p>
     * <p>Returns {@code dev} when the version is unavailable on the classpath; plugins may override it.</p>
     *
     * @return the plugin version
     */
    default String version() {
        Package type = getClass().getPackage();
        String version = type == null ? null : type.getImplementationVersion();
        return version == null || version.isBlank() ? "dev" : version;
    }

    /**
     * 插件适配的宿主 API 版本，当前支持 1 至 3；提供 AI 工具时至少为 2，
     * 使用图片渲染或插件模板时为 3。
     * <p>Host API version the plugin targets; 1 through 3 are supported.
     * At least 2 is required for AI tools; 3 is required for image rendering or plugin templates.</p>
     *
     * @return the API version
     */
    default int apiVersion() {
        return 1;
    }

    /**
     * 插件提供的指令，在 {@link #start(PluginContext)} 之后调用。
     * <p>Commands provided by the plugin, requested after {@link #start(PluginContext)}.</p>
     * <p>不需要指令时返回空列表，插件可以只提供 AI 工具。</p>
     * <p>Return an empty list when the plugin has no commands; a plugin may provide only tools.</p>
     *
     * @return the commands, may be empty
     */
    default List<Cmd> cmds() {
        return List.of();
    }

    /**
     * 插件提供的 AI 工具，在 {@link #start(PluginContext)} 之后调用。
     * <p>AI tools provided by the plugin, requested after {@link #start(PluginContext)}.</p>
     * <p>工具名必须全局唯一，建议使用“插件 ID_工具名”的格式；提供工具时
     * {@link #apiVersion()} 至少为 2。</p>
     * <p>Tool names must be globally unique, preferably prefixed with the plugin id;
     * {@link #apiVersion()} must be at least 2 when tools are provided.</p>
     *
     * @return the tools, may be empty
     */
    default List<Tool> tools() {
        return List.of();
    }

    /**
     * 插件启动，在获取和注册指令前调用。
     * <p>Starts the plugin before its commands are requested and registered.</p>
     *
     * @param context the host services exposed to the plugin
     */
    default void start(PluginContext context) {
    }

    /**
     * 插件停止，在卸载时调用。
     * <p>Stops the plugin when it is unloaded.</p>
     */
    default void stop() {
    }
}
