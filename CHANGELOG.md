# Changelog

本项目的所有重要变更都记录在此文件，格式参考 Keep a Changelog。

## [Unreleased]

### Added

- 新增 `adapter-qq`：QQ 官方机器人 WebSocket 适配器，支持 QQ 群与 C2C 私聊、主动文本与富媒体发送（不携带 `msg_id`，避免被动回复自动 @）、token 自动刷新、断线 Resume 与重连；构建产物为 `launcher-qq.jar`。
- 适配器使用平台消息 ID 的稳定短 ID 映射，兼容现有 `message.id` 字段长度；支持群信息/成员白名单接口、禁言与撤回，并在无权限时回退。

### Changed

- `#Usage` 在缺少用户头像时改用占位头像渲染，不再直接失败；QQ 官方机器人等无法获取头像的平台也可正常使用。
- 群聊消息同时 @机器人 并携带指令时优先执行指令，未匹配指令才进入 AI 对话；开头的提及段与引用段不再阻止指令解析。
- `adapter-qq` 的配置项 `nullbot.qq.api-base` 更名为 `nullbot.qq.api-url`，旧键不再读取。
- `adapter-qq` 拆分 HTTP/token、OpenAPI、媒体、入站路由与客户端工厂，统一会话目标 `QqTarget`；公开行为不变。

## [1.1.0] - 2026-10-27

### Added

**插件与 API**

- 插件 API 2：`Plugin.tools()` 允许外部插件注册 AI 工具，插件停用或重载时同步注销；`cmds()` 改为默认空实现；兼容 API 1。
- 插件 API 3：`PluginContext.renderer()` 统一提供原始 SVG、预编译 JTE 模板与字体注册；插件 JAR 内 `fonts/` 下的 TTF/OTF 自动注册；兼容 API 1/2。
- 新增 `say-plugin`：`Say`/`说` 渲染随机语录卡片，作者头像渐隐、右对齐；支持 `@用户`、用户 ID 与 `--id=<语录ID>`，随插件分发 Noto Serif CJK SC 字体。
- 新增 `gd-plugin`：`GD`/`几何冲刺` 输出关卡搜索、详情与 GDDL B20 图片卡片；B20 按 `rt = tier^E × (进度/100)^1.5` 取最高 20 条平均，`E` 默认 1.5、可配 2，仅统计 GDDL 提交记录；支持超时、重试、HTTP 代理、缓存与 `config.properties.example`。
- `hello-plugin` 的 `Hello` 新增 `--image` 图片卡片示例。
- 控制台插件页展示插件注册的 AI 工具；概览新增用户调用排行。
- `MsgContent` 新增公共 `BASE64_PREFIX`，`Builder.imageBase64` 重载封装 base64 图片 URL。

**图片渲染**

- 新增 JTE 预编译模板 + JairoSVG 图片渲染管线，宿主与插件共用 `JteSvgRenderer`；内置 `Usage`、`Info`、`Help` 图片卡片，`Help -p=<插件>` 支持查看指定插件。

**文档**

- 新增 `PLUGIN.md` 插件开发指南；README 重新整理，补充插件结构、配置示例与文档入口。

### Changed

**渲染与字体**

- 内置插件版本统一为 `1.1.0`，插件 `version()` 与 Maven 项目版本保持一致。
- 核心渲染字体统一为 JetBrains Maple Mono NF（OFL），移除 JetBrains Mono 与 Noto Sans Mono CJK SC；粗体通过 `font-weight` 合成；字体常量改为 `FontRegistry.JETBRAINS_MAPLE_MONO`，外部插件引用旧常量需同步调整。
- 新增 core SVG 文本与表格排版工具 `SvgText`/`SvgLine`/`SvgPositionedLine`、`SvgTable`/`SvgColumn`/`SvgCell`，以及图片工具 `ImageData`（data URI、WebP/PNG 解码、裁剪缩放、PNG 编码）；GD、Usage、Say 与 AI `view` 复用。
- emoji 字体分段抽取到 core `SvgText.fontSegments`，Noto Emoji 移入 core 字体注册；`Say` 模板改用 `SvgText.Segment`。
- 渲染卡片统一深色/中性配色：移除左侧色条和蓝色 glow，提高正文/标签对比；`GD`、`Info`、`Help`、`Usage`、`Hello`、`Say` 同步，`Usage` 计数使用浅蓝数据色。
- `Usage` 卡片显示昵称与用户 ID，计数补零改为 7 位；`Info` 统一左右字号为 14px 并调整底部留白；`Help` 在指令数量行提示 `指令名 -H`；提高渲染模板 SVG 输出尺寸。

**GD 插件**

- 搜索与 B20 卡片新增表头并使用固定列对齐；详情页缩略图改为圆角、右侧信息垂直居中，下载/点赞/长度/物体改用 Nerd Font 图标；歌曲名右侧显示歌曲 ID，标题/meta/GDDL/歌曲/描述按宽度截断；整体字号提高并收紧底部留白。
- GD 搜索支持回复 `next` 翻页；B20 卡片右上角显示 GDDL 头像并新增 `gddl.avatar-base` 配置；`rank` 子命令更名为 `gddl`，新增 `--bind`/`--unbind` 用户绑定。
- GDDL 与缩略图请求改用 HTTP/1.1，支持连接/请求超时、重试与 HTTP 代理，关卡详情并行拉取 GDDL 与缩略图；排除未使用的 Netty native QUIC 依赖并合并 shade 元数据。

**插件与工程**

- Maven 模块版本改为由根 `pom.xml` 的 `revision` 属性统一控制，发布时只需修改一处。
- 插件 `version()` 默认读取 JAR manifest 的 `Implementation-Version`，内置插件版本自动跟随 Maven `revision`。
- 统一插件代码结构：根包保留入口、文案与异常，指令移入 `cmd`，配置/服务/模型/渲染按需分层；指令通过构造器注入具体依赖，插件访问器收回包内可见，生命周期与 `stop()` 统一。
- 统一插件双语文案：`hello`、`gallery`、`game`、`pubg`、`query`、`say`、`sb6657`、`symmetry` 新增 `XxxText`；`game` 的问答、猜图与老婆文案迁入 `GameText`；`sb6657-plugin` 使用 `SB6657Exception` 统一接口与解析错误。
- `Say`/`说` 从 `game-plugin` 拆分到独立 `say-plugin`，模板与字体随插件分发；语录卡片改用 Noto Serif CJK SC 斜体，作者行使用双倍宽破折号并加粗，收紧换行宽度。
- 控制台群设置与用户设置弹窗按基础、通知、限速与 AI 对话分组；`Echo` 使用唯一名称，移除 `Reply`/`回复` 别名。
- 插件加载与注册流程统一回滚，插件状态新增工具列表；启动时清理异常退出遗留的 `.nullbot-plugin-*.jar.tmp`。

### Fixed

- AI 最终回复和 `send` 工具消息发送前移除代码块外的连续空行。
- SVG 栅格化启用 fractional metrics，修复等宽字体逐字取整导致的列错位。
- 修复日志 SSE 长连接未关闭导致优雅停机卡住，停机时主动关闭日志流。
- 修复控制台状态标签在窄列换行，并将插件 ID 列居中。
- 修复 `Say` 语录卡片 emoji 显示为方框，以及换行宽度过大导致文字贴近右边缘的问题。

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
