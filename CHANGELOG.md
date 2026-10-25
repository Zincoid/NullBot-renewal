# Changelog

本项目的所有重要变更都记录在此文件。

## [Unreleased]

### Added

- 图片渲染：JTE 模板填充数据，SVG 经 JairoSVG 输出 PNG；`Usage`/`用量` 渲染历史用量卡片，`Info` 渲染系统信息，`Help`/`帮助` 渲染指令与插件列表（含 `Help -p=<插件>` 提示）。
- 插件 API 3：`PluginContext.renderer()` 统一提供原始 SVG、预编译 JTE 模板和字体注册；插件 JAR 内 `fonts/` 下的 TTF/OTF 加载时自动注册，`PluginRenderer.font` 支持动态注册；兼容 API 1 与 2。
- `hello-plugin` 的 `Hello` 指令新增 `--image` 图片卡片输出示例。
- `MsgContent` 新增公共 `BASE64_PREFIX`，`MsgContent.Builder` 新增 `imageBase64` 重载，封装 base64 图片 URL 的编码与拼接。
- 插件 API 2：`Plugin.tools()` 允许外部插件注册 AI 工具，插件停用或重载时同步注销；`cmds()` 改为默认空实现，插件可只提供工具；兼容 API 1。
- 控制台插件页展示插件注册的 AI 工具。
- 控制台概览新增用户调用排行。

### Changed

- 渲染字体：内置 JetBrains Mono 与 Noto Sans Mono CJK SC（均为 OFL），移除 Monorama；`Usage`、`Info`、`Help` 统一为深色卡片风格，值文本支持中英混排。
- 优化 `Usage` 用量卡片的布局与视觉样式。
- 合并插件渲染入口为 `PluginContext.renderer()`，宿主与插件共用 `JteSvgRenderer` 渲染管线，字体重复注册与模板类读取错误处理更明确。
- 插件加载器启动时清理上次异常退出遗留的 `.nullbot-plugin-*.jar.tmp` 副本。
- 重构插件加载与注册流程：指令与工具注册统一回滚，插件状态新增工具列表。
- `Echo` 指令使用 `Echo` 作为唯一名称，不再保留 `Reply` / `回复` 别名。

## [1.0.0] - 2026-10-25

首个正式版本。

### Added

- 多模块架构：`spi`、`core`、`adapters`、`launcher`、`plugins`。
- 适配器 SPI：Shiro（OneBot 11 正/反向 WS）与 stdin 适配器。
- 指令框架：前缀与别名、限权、限流、关键词触发、输入等待、帮助、调用统计。
- 消息总线：聊天记录、入退群通知、防撤回、自动聊天。
- AI 对话：OpenAI 兼容接口，群聊 @/私聊/戳一戳触发，按群与用户配置提示词、推理强度、注入防护；工具调用（call/read/view/send/search/web/mute/recall）。
- 媒体与语录：图片、视频、音频、语录的保存、获取、随机发送与删除。
- Web 控制台：概览、角色与群设置、关键词、语录、文件、配置、插件、系统信息，以及 SSE 实时日志。
- 插件系统：JAR 热插拔，上传、启停、重载、删除、独立数据目录，ServiceLoader 加载。
- 内置插件：hello、gallery、game、pubg、query、sb6657、symmetry。
- 存储：SQLite 自动建表；MySQL 提供 `schema-mysql.sql`。
- 配置：`nullbot.*` 支持文件、环境变量与启动参数，控制台令牌缺省随机生成。
