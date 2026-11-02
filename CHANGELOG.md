# Changelog

本项目的所有重要变更都记录在此文件，格式参考 Keep a Changelog。

## [Unreleased]

### Fixed

- AI `send` 工具发送的中间消息现在写入会话记录并带上所发文件 ID，可在 `#History` 与后续 AI 上下文中看到。
- 消息窗口裁剪只删除会话自己保存的聊天附件，且不再被其他消息引用时才删除；机器人发送的图库等文件不会被连带删除。

## [1.2.0] - 2026-11-02

本版本重构 AI 触发链路与用量统计，新增指令禁用和群 / 用户功能权限，插件 API 升级至 5。从 1.1.0 升级时请先阅读“数据结构与迁移”和“AI 触发”。

### Added

**AI 与统计**

- 新增 `ai_usage` 按日汇总表，以 `time + chat_type + chat_id + user_id + source` 为唯一键，累计 AI 回合数与输入/输出词元；来源区分群聊、群自动聊天、群戳一戳、私聊与私聊戳一戳。
- 控制台概览新增 AI 子页：展示按天调用与词元趋势、输入/输出词元汇总、活跃用户 / 群、触发来源以及群 / 用户排行。
- 控制台概览新增“累计”子页，分别汇总不限时间的调用数与词元数。

**指令与权限**

- 新增指令禁用：`#指令 -B` / `-U` 可在当前会话禁用或解除，支持整群、`@用户`、`--user=<ID>` 与私聊规则，支持 `--temp=30m|2h|7d` 限时，到期自动恢复；被禁用时提示剩余时间，永久规则显示“永久”，群聊与私聊规则相互隔离。
- 控制台角色页的群设置与用户设置新增“指令管理”页，可查看、添加和解除禁用规则。
- 控制台角色页的群 / 用户表新增“指令 / AI 对话”开关：群默认开启，用户私聊默认关闭，可独立授权。

**插件与 API**

- 插件 API 5：新增 `PluginContext.http()`，统一提供 GET、字节 POST 与单文件 multipart 上传，支持超时、代理、重试与响应大小限制。
- 插件 API 4：新增 `PluginContext.properties()`；宿主在插件启动前把 JAR 内的 `config.properties` 复制到数据目录，已有文件不会覆盖。
- 控制台插件数据弹窗新增“配置”页，可在线编辑并保存 `config.properties`、保存后重载插件；文件页支持逐项删除。
- `hello-plugin` 新增 `hello.data` 配置示例，首次加载自动生成 `config.properties`。

**GD 插件**

- 新增本地进度：`#GD register`、`#GD update <关卡ID> <进度>`、`#GD remove`、`#GD unregister`，以及 `#GD b20 [--all]`；进度保存在插件数据目录的 SQLite `gd.db` 中，更新时通过 JDash 验证关卡并显示名称 / 作者，进度范围 1–100。
- `#GD b20` 使用本地进度与 GDDL 评级计算本地 B20，新增 `local-b20.jte` 卡片；GDDL 评级按已评级 24 小时、未评级 1 小时缓存。
- `#GDDL` 改为子指令模式：`GDDL bind <用户名|ID>`、`GDDL unbind`、`GDDL b20 [用户名|ID] [--all]`；B20 改用 GDDL 原始小数评分计算，tier 由 rating 派生，表格显示为 `25(25.33)`。
- `#GD search` 结果新增关卡 ID 列，页码移到右上角；关卡详情与 B20 卡片调整评分、颜色、用户标识与文字样式。

### Changed

**数据结构与迁移**

- `usage` 表更名为 `cmd_usage`：`group_id` 改为 `chat_id`，新增 `chat_type`（0=群聊，1=私聊），唯一键改为 `time + chat_type + chat_id + user_id + cmd`；控制台接口路径保持 `/console/usage`。
- 新增 `cmd_disable` 单表，使用 `chat_type` / `chat_id` 区分群聊与私聊规则。
- 群记录新增 `cmd_enabled` / `ai_chat_enabled`（默认 true）；用户记录新增 `private_cmd_enabled` / `private_ai_chat_enabled`（默认 false），删除 `private_message_allowed`。升级后现有用户的两个私聊权限均为 false，需要重新授权。
- `UserSetting` 的 `aiPrompt` / `aiEffort` / `aiInjectionGuard` 更名为 `privateAiPrompt` / `privateAiEffort` / `privateAiInjectionGuard`，数据库列同步改名。
- SQLite 新增表会在启动时自动创建；MySQL 需重新执行 `schema-mysql.sql`。已有数据库的表名、列与索引需手动迁移；若使用过旧版开发构建的逐条 `ai_usage` 记录表，需要删除重建。

**AI 触发**

- AI 触发从 `Chat` / `Poked` 指令迁移到 `AiChatMiddleware`：群 @、私聊、戳一戳与自动聊天直接调用 `ChatPipeline`，移除 `#Chat` / `#Poked`；关键词规则中引用 `Chat` / `Poked` 的配置失效。
- 群 AI 复用群功能限速（键 `AI`），私聊 AI 暂不限速；AI 用量改由 `ai_usage` 记录，不再计入 `cmd_usage`。
- 戳一戳触发 AI 后调用指令时，宿主合成对应的消息事件；`Cmd` 移除戳一戳的 `run` 重载，外部插件需改用消息入口。
- `RegisterHandler` 由 `BotRegistrar` 替代，通过 `BotClient` 查询群名 / 昵称并懒注册；`GroupService` / `UserService` 不再依赖 `BotClient`，适配器不支持信息查询时回退平台 ID。
- `BANNED` 用户的所有事件在 `BotDispatcher` 入口直接丢弃。
- `GroupCmdRateLimiter` 更名为 `GroupRateLimiter`，限速维度 `LimitScope.CMD` 改为 `FUNC`（控制台显示“按功能”）；已有数据库的 `limit_scope = 2` 无需修改。

**插件与工程**

- 插件配置改由宿主统一解压，并通过 `properties()` 提供只读快照；GD / Query 配置移除环境变量回退。
- `GdHttp`、Query、SB6657、Symmetry、Say 与 `UsageCmd` 改用宿主 HTTP 服务，移除各自的 JDK `HttpClient` / `HttpUtils`；插件 HTTP 请求统一由各自的 `XxxHttp` 适配器承载。
- 插件加载与上传的临时文件名缩短，启动时清理遗留临时文件。
- `CmdArgs` 的长选项名至少两个字符、短选项必须是单个字母；`hasOpt` / `getOpt` 传入非法 name / alias 会抛 `IllegalArgumentException`。

**控制台**

- 角色、语录、文件与插件表统一为固定列宽，长 ID / 内容省略并在悬浮时显示完整值；语录内容新增复制按钮。
- 插件列表移除 JAR 列，JAR 文件名可在插件数据窗口查看；指令列改用 `Cmd.name()` 返回的规范名称。
- 配置页重做排版；概览“累计”拆分为“调用累计”和“词元累计”，AI 排行中的词元使用 k/M/B 缩写。

### Fixed

- 修复 AI 看不到群聊 / 私聊发送者昵称的问题：历史消息与触发说明改为在正文中携带“昵称(用户 ID)”，不再依赖部分 OpenAI 兼容接口会忽略的 `name` 字段。
- 修复控制台实时日志连接静默断开后仍显示“已连接”且不会自动重连的问题；前端检测 45 秒无数据（含 SSE 心跳）会中止连接并触发重连。
- 宿主 HTTP 服务的请求失败消息不再拼接完整 URL，避免查询参数中的 API key 等敏感信息进入日志。
- 文件下载与聊天附件保存失败时不再拼接完整 URL，避免带签名参数的媒体地址进入日志。

## [1.1.0] - 2026-10-27

### Added

**适配器**

- 新增 `adapter-qq`：QQ 官方机器人 WebSocket 适配器，支持 QQ 群与 C2C 私聊、主动文本与富媒体发送（不携带 `msg_id`，避免被动回复自动 @）、token 自动刷新、断线 Resume 与重连；构建产物为 `launcher-qq.jar`。
- `adapter-qq` 使用平台消息 ID 的稳定短 ID 映射，兼容现有 `message.id` 字段长度；支持群信息/成员白名单接口、禁言与撤回，并在无权限时回退。

**插件与 API**

- 插件 API 2：`Plugin.tools()` 允许外部插件注册 AI 工具，插件停用或重载时同步注销；`cmds()` 改为默认空实现；兼容 API 1。
- 插件 API 3：`PluginContext.renderer()` 统一提供原始 SVG、预编译 JTE 模板与字体注册；插件 JAR 内 `fonts/` 下的 TTF/OTF 自动注册；兼容 API 1/2。
- 新增 `say-plugin`：`Say`/`说` 渲染随机语录卡片，作者头像渐隐、右对齐；支持 `@用户`、用户 ID 与 `--id=<语录ID>`，随插件分发 Noto Serif CJK SC 字体。
- 新增 `gd-plugin`：`GD`/`几何冲刺` 输出关卡搜索与详情，`GDDL` 输出 GDDL B20 图片卡片；B20 按 `rt = tier^E × (进度/100)^1.5` 取最高 20 条平均，`E` 默认 1.5、可配 2，仅统计 GDDL 提交记录；支持超时、重试、HTTP 代理、缓存与 `config.properties.example`。
- `hello-plugin` 的 `Hello` 新增 `--image` 图片卡片示例。
- 控制台插件页展示插件注册的 AI 工具；概览新增用户调用排行。
- `MsgContent` 新增公共 `BASE64_PREFIX`，`Builder.imageBase64` 重载封装 base64 图片 URL。

**图片渲染**

- 新增 JTE 预编译模板 + JairoSVG 图片渲染管线，宿主与插件共用 `JteSvgRenderer`；内置 `Usage`、`Info`、`Help` 图片卡片，`Help -p=<插件>` 支持查看指定插件。

**文档**

- 新增 `PLUGIN.md` 插件开发指南；README 重新整理，补充插件结构、配置示例与文档入口。

### Changed

**适配器与指令**

- `adapter-qq` 的配置项 `nullbot.qq.api-base` 更名为 `nullbot.qq.api-url`，旧键不再读取。
- 群聊消息同时 @机器人 并携带指令时优先执行指令，未匹配指令才进入 AI 对话；开头的提及段与引用段不再阻止指令解析。
- `adapter-qq` 拆分 HTTP/token、OpenAPI、媒体、入站路由与客户端工厂，统一会话目标 `QqTarget`；公开行为不变。

**渲染与字体**

- 内置插件版本统一为 `1.1.0`，插件 `version()` 与 Maven 项目版本保持一致。
- 核心渲染字体统一为 JetBrains Maple Mono NF（OFL），移除 JetBrains Mono 与 Noto Sans Mono CJK SC；粗体通过 `font-weight` 合成；字体常量改为 `FontRegistry.JETBRAINS_MAPLE_MONO`，外部插件引用旧常量需同步调整。
- 新增 core SVG 文本与表格排版工具 `SvgText`/`SvgLine`/`SvgPositionedLine`、`SvgTable`/`SvgColumn`/`SvgCell`，以及图片工具 `ImageData`（data URI、WebP/PNG 解码、裁剪缩放、PNG 编码）；GD、Usage、Say 与 AI `view` 复用。
- emoji 字体分段抽取到 core `SvgText.fontSegments`，Noto Emoji 移入 core 字体注册；`Say` 模板改用 `SvgText.Segment`。
- 渲染卡片统一深色/中性配色：移除左侧色条和蓝色 glow，提高正文/标签对比；`GD`、`Info`、`Help`、`Usage`、`Hello`、`Say` 同步，`Usage` 计数使用浅蓝数据色。
- `Usage` 卡片显示昵称与用户 ID，计数补零改为 7 位；`Info` 统一左右字号为 14px 并调整底部留白；`Help` 在指令数量行提示 `指令名 -H`；提高渲染模板 SVG 输出尺寸。

**GD 插件**

- 将 GDDL B20 从 `#GD gddl` 拆分为独立指令 `#GDDL`；`#GD`/`#几何冲刺` 保留关卡搜索与详情。
- 重做关卡详情卡片：缩略图放大到 400×225，标题旁增加关卡 ID 胶囊；下载、点赞、长度与物体数改为带 Nerd Font 图标和标签的 2×2 统计块；GDDL 使用与缩略图同宽的独立面板，Tier 0–40 从绿到蓝再到红渐变、未评级保持灰色，并缩写为 `Rt.`/`Enj.`；歌曲与描述移至右栏，描述最多四行；标题、meta、歌曲与描述按宽度截断，整体字号提高并调整页脚上下留白。
- 搜索与 B20 列表新增表头并使用固定列对齐；加宽 B20 的 ID 列，避免长关卡 ID 被截断；B20 卡片右上角显示 GDDL 头像并新增 `gddl.avatar-base` 配置。
- GD 搜索支持回复 `next` 翻页；`rank` 子命令更名为 `gddl`，新增 `--bind`/`--unbind` 用户绑定。
- 关卡物体数达到 GD 接口上限 65535 时显示为 `65,535+`，避免误认为是精确值。
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

- `#Usage` 在缺少用户头像时改用占位头像渲染，不再直接失败；QQ 官方机器人等无法获取头像的平台也可正常使用。
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
