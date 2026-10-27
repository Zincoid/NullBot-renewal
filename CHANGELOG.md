# Changelog

本项目的所有重要变更都记录在此文件。

## [Unreleased]

### Added

- 插件 API 2：`Plugin.tools()` 允许外部插件注册 AI 工具，插件停用或重载时同步注销；`cmds()` 改为默认空实现，插件可只提供工具；兼容 API 1。
- 图片渲染：JTE 预编译模板填充数据，SVG 经 JairoSVG 输出 PNG；内置 `Usage`/`用量` 历史用量卡片、`Info` 系统信息和 `Help`/`帮助` 指令列表，`Help -p=<插件>` 支持查看指定插件。
- 插件 API 3：`PluginContext.renderer()` 统一提供原始 SVG、预编译 JTE 模板与字体注册；插件 JAR 内 `fonts/` 下的 TTF/OTF 在加载时自动注册，`PluginRenderer.font` 支持动态注册；兼容 API 1 与 2。
- 新增 `say-plugin`：`Say`/`说` 指令将随机语录渲染为图片，左侧作者头像渐隐，右侧带引号展示语录；作者右对齐、字号略小于语录，支持通过 `@用户` 或用户 ID 指定作者。
- 新增 `gd-plugin`：`GD`/`几何冲刺` 指令以图片卡片输出关卡搜索列表、关卡详情与 GDDL B20 评分；关卡详情内嵌社区缩略图（WebP 自动转为 PNG），数据通过 JDash 5.1.0 与 GDDL API 获取。
- GD 插件按 `rt = tier^E × (进度/100)^1.5` 取最高的 20 条计算 B20，`E` 默认 1.5，可配置为 2；仅统计 GDDL 提交记录（`beaten`/`beating`）。
- `gd-plugin` 的 GDDL/缩略图请求支持配置连接超时、请求超时、重试次数与 HTTP 代理，并缓存成功获取的缩略图。
- `gd-plugin` 新增 `config.properties.example` 配置模板，列出代理、超时、缓存与 B20 配置项。
- `say-plugin` 新增 Noto Serif CJK SC 与 Noto Emoji（均为 OFL）字体资源，用于 `Say` 语录卡片渲染。
- `Say`/`说` 支持 `--id=<语录ID>` / `-i=<语录ID>` 获取指定语录。
- `hello-plugin` 的 `Hello` 指令新增 `--image` 图片卡片输出示例。
- 控制台插件页展示插件注册的 AI 工具。
- 控制台概览新增用户调用排行。
- `MsgContent` 新增公共 `BASE64_PREFIX`，`MsgContent.Builder` 新增 `imageBase64` 重载，封装 base64 图片 URL 的编码与拼接。

### Changed

- GD 关卡详情卡片的缩略图改为圆角，右侧信息按缩略图垂直居中。
- 核心渲染字体统一为 JetBrains Maple Mono NF（OFL），移除 JetBrains Mono 与 Noto Sans Mono CJK SC；粗体改为通过 `font-weight` 合成。
- 核心字体常量改为 `FontRegistry.JETBRAINS_MAPLE_MONO`，外部插件若引用旧字体常量需同步调整。
- GD 卡片使用 core 的 JetBrains Maple Mono 与 Nerd Font 星形图标，移除中英文逐字排版逻辑。
- GD 搜索结果卡片新增表头（序号、关卡、难度、作者、星）。
- GD B20 卡片新增表头（序号、关卡、ID、Tier、进度、rt），并将行改为固定列对齐。
- GD 关卡详情页的下载、点赞、长度、物体统计改用 Nerd Font 图标，并调整统计列间距。
- GD 关卡详情页歌曲名右侧显示歌曲 ID；标题、meta、GDDL、歌曲与描述改为按可用宽度截断，修复长描述溢出。
- GD 插件将 `rank` 子命令改名为 `gddl`，新增 `--bind`/`--unbind` 用户绑定功能；`#GD gddl` 不带用户名时查询绑定账号，绑定保存在插件数据目录的 `gddl-bindings.properties`，已有绑定不会被覆盖。
- GD 卡片试点中性配色：移除左侧色条和蓝色 glow，提高正文/标签对比，蓝色仅保留给 B20 分数等核心数据。
- Info、Help、Usage、Hello 卡片同步中性配色：移除左侧色条和蓝色 glow；Usage 计数使用浅蓝数据色。
- 增加 `Info` 卡片底部留白，画布高度从 400 调整为 410。
- 帮助卡片的插件提示文案移入 `Text`，移除未使用的 `HELP_LIST`、`HELP_PLUGIN`。
- `Say` 卡片同步中性配色并移除左侧蓝色色条。
- `GD search` 支持回复 `next` 翻到下一页，最后一页会提示。
- 提高 GD 搜索、关卡详情与 B20 卡片的整体字号，改善可读性。
- 新增 core SVG 文本排版工具 `SvgText`/`SvgLine`/`SvgPositionedLine`，GD 插件复用并移除私有的测量、截断与居中代码。
- 新增 core SVG 表格排版工具 `SvgTable`/`SvgColumn`/`SvgCell`，GD 搜索与 B20 卡片复用并移除模板中的固定列坐标。
- 新增 core 图片数据工具 `ImageData`，统一 data URI、WebP/PNG 解码、居中裁剪、头像方形缩放与 PNG 编码；GD、Usage、Say 和 AI `view` 工具复用。
- 抽取 emoji 字体分段到 core `SvgText.fontSegments`，将 Noto Emoji 移入 core 字体注册；`Say` 模板改用 `SvgText.Segment`。
- `Usage` 卡片在计数上方显示用户昵称与用户 ID，昵称过长时自动截断并保留 ID。
- 重构 `gd-plugin` 包结构：配置、数据服务、视图模型、渲染与绑定按层拆分，`GdText` 保留在根包。
- `gd-plugin` 排除未使用的 Netty native QUIC 依赖，并在 shade 中合并 LICENSE/NOTICE 与 Netty 版本元数据，消除重复资源警告。
- 对齐 GD 搜索与 B20 列表的右侧列，并收紧关卡详情卡片底部留白。
- GD B20 卡片右上角显示 GDDL 用户头像，新增 `gddl.avatar-base` 配置项。
- `Info` 卡片左侧标签与右侧数值字体统一为 14px。
- `Usage` 卡片计数补零位数从 8 位改为 7 位。
- 统一插件双语文案：`hello`、`gallery`、`game`、`pubg`、`query`、`say`、`sb6657`、`symmetry` 各新增 `XxxText` 文案枚举，帮助与提示按中英成对维护；`game` 的问答、猜图与老婆文案从核心 `Text` 迁入 `GameText`。
- 渲染卡片：`Usage`、`Info`、`Help` 统一为深色卡片风格，值文本支持中英混排。
- 将 `Say`/`说` 指令从 `game-plugin` 拆分到独立的 `say-plugin`（插件 ID `say`），模板与字体随插件分发。
- `say-plugin` 的 `Say` 语录卡片改用 Noto Serif CJK SC 斜体，移除分隔线；作者行使用双倍宽破折号（U+2E3A）并加粗，同时加大与语录的间距。
- 优化 `Usage` 用量卡片的布局与视觉样式。
- 优化控制台群设置与用户设置弹窗的排版：群设置按基础、通知、限速与 AI 对话分组，用户设置归入 AI 对话分组。
- `Help` 帮助卡片在指令数量行右侧提示通过 `指令名 -H` 查看指令详情。
- 提高 `Usage`、`Info`、`Help` 等渲染模板的 SVG 输出尺寸，提升图片清晰度。
- 合并插件渲染入口为 `PluginContext.renderer()`，宿主与插件共用 `JteSvgRenderer` 渲染管线，字体重复注册与模板类读取错误处理更明确。
- 重构插件加载与注册流程：指令与工具注册统一回滚，插件状态新增工具列表。
- 插件加载器启动时清理上次异常退出遗留的 `.nullbot-plugin-*.jar.tmp` 副本。
- `Echo` 指令使用 `Echo` 作为唯一名称，不再保留 `Reply` / `回复` 别名。

### Fixed

- AI 最终回复和 `send` 工具消息在发送前移除代码块外的连续空行，避免 QQ 中出现多余空行。
- 修复等宽字体在 SVG 图片中列错位的问题：栅格化启用 fractional metrics，避免字形宽度被逐字取整为 8/10px。
- 修复日志 SSE 长连接未关闭导致优雅停机卡住的问题；停机时主动关闭日志流。
- `gd-plugin` 的 GDDL 与缩略图请求改用 HTTP/1.1、缩短超时并增加一次重试；关卡详情并行拉取 GDDL 与缩略图，单个服务超时不再阻塞整张卡片。
- 修复控制台状态标签（如“已加载”）在窄列中换行的问题，并将插件 ID 列居中显示。
- 收紧 `Say` 语录卡片的换行宽度，避免卡片变窄后文字贴近右边缘。
- 修复 `Say` 语录卡片中的 emoji 显示为方框的问题：新增 Noto Emoji 字体，语录与作者行按文本段混排渲染。

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
