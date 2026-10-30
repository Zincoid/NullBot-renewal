# Changelog

本项目的所有重要变更都记录在此文件，格式参考 Keep a Changelog。

## [Unreleased]

### Added

**指令**

- 新增指令禁用：直接使用 `#指令 -B` 可在当前群或私聊禁用该指令，`#指令 -U` 解除；群聊支持整群或 `@用户`/`--user=<ID>`，私聊必须指定 `--user`。
- 指令禁用支持 `--temp=30m` / `2h` / `7d` 限时规则，到期自动恢复；被禁用时回复剩余时间，永久规则显示“永久”。
- 群聊规则与私聊规则相互隔离，群规则不影响私聊，私聊规则不影响群规则。
- MySQL 部署需重新执行 `schema-mysql.sql` 创建 `group_cmd_disable` / `user_cmd_disable` 表；SQLite 启动时自动创建。

**控制台**

- 角色页的群设置与用户设置新增“指令管理”页：列出并管理整群、群内指定用户与私聊的指令禁用规则，支持永久或按分钟、小时、天手动填写时长，可直接解除。

**插件与 API**

- 插件 API 5：新增 `PluginContext.http()` 与 `HttpService`，统一提供 GET、字节 POST、单文件 multipart 上传，以及超时、代理、重试和响应大小限制。

**GD 插件**

- `gd-plugin` 新增本地进度：`#GD register`、`#GD update <关卡ID> <进度>`、`#GD remove`、`#GD unregister`；进度保存在插件数据目录的 SQLite `gd.db`，更新时通过 JDash 验证关卡并显示名称/作者，进度范围 1–100。
- `#GD b20` 使用本地进度与 GDDL 关卡评级计算本地 B20，新增 `local-b20.jte` 卡片、机器人用户头像与未评级提示；GDDL 评级按已评级 24 小时、未评级 1 小时缓存，可通过 `gddl.tier-cache-ttl` / `gddl.unrated-cache-ttl` 调整。
- `gd-plugin` 与 `query-plugin` 首次加载时自动从 JAR 内默认配置创建数据目录的 `config.properties`，已有配置不会覆盖。
- 控制台“插件数据”弹窗新增“配置”页，可直接编辑并保存 `config.properties`，支持保存后重载插件；原文件管理保留在“文件”页。
- 插件配置页的“保存并重载”改为带重载图标的按钮，`config.properties` 改为带文件图标的纯文本标签，并与工具栏按钮对齐；配置状态提示移到按钮行左侧。
- 插件列表中的 JAR 文件名改为普通文本显示。
- `hello-plugin` 新增测试配置：首次加载自动创建 `config.properties`，`Hello` 指令的 `Data:` 输出改为配置项 `hello.data=<value>`（默认 `hello-plugin-test`）。
- 控制台插件数据文件列表为每个条目新增独立删除按钮，文件夹支持逐项删除。

### Fixed

- 修复控制台实时日志连接静默断开后仍显示“已连接”、且不会自动重连的问题；前端现在检测 45 秒无数据（含 SSE 心跳）会中止连接并触发重连。
- 宿主 HTTP 服务的请求失败消息不再拼接完整 URL，避免查询参数中的 API key 等敏感信息进入日志。
- 文件下载与聊天附件保存失败时不再拼接完整 URL，避免带签名参数的媒体地址进入日志。

### Changed

- `gd-plugin` 拆分数据访问：`GdLevelService` 负责 JDash 关卡与缩略图，`GddlService` 负责 GDDL 关卡/用户/提交记录与 B20，`GdProgressService` 负责本地进度；`GdStore` 更名为 `GdProgressStore`，`ScoreResult` 更名为 `GddlB20Result`，GDDL B20 视图/模板统一为 `GddlB20View` / `gddl-b20.jte`。
- B20 计算改用 GDDL 原始小数 `rating`，不再使用四舍五入后的 tier；结果表格的 tier 列显示为 `25(25.33)` 形式。
- `gd_level_cache` 只保留原始 `rating`，tier 由 rating 派生；数据库 schema 为 v1，首次启动直接创建不含 `tier` 列的表。
- `GDDL` 改为子指令模式：`GDDL bind <用户名|ID>`、`GDDL unbind`、`GDDL b20 [用户名|ID] [--all]`；原 `--bind` / `--unbind` 和直接用户名形式不再支持。
- `#GD search` 搜索结果卡片新增关卡 ID 列，ID 左对齐并左移列位置；同步调整关卡名、难度与作者列宽以适配。
- `#GD search` 搜索卡片的页码改为右上角右对齐显示。
- `#GDDL b20` 卡片用户名前不再显示 `GDDL:` 前缀。
- GDDL 与本地 B20 卡片的用户 ID 改为 `#id` 形式。
- B20 卡片用户名加大到 32px 并加粗，用户 ID 使用 20px 小字号；关卡详情卡片的关卡名改为加粗显示。
- 插件配置改由宿主在插件启动前自动解压，并通过 `PluginContext.properties()` 提供只读配置快照；插件只保留类型化映射，GD/Query 配置移除环境变量回退，插件 API 升至 4。
- `GdHttp`、Query、SB6657、Symmetry、Say 改用宿主 HTTP 服务，移除各自的 JDK `HttpClient` 或 `HttpUtils` 导入；插件 API 升至 5。
- 插件 HTTP 请求统一由各自的 `XxxHttp` 适配器承载（`GdHttp`、`QueryHttp`、`SB6657Http`、`SymmetryHttp`、`SayHttp`），命令不再直接构造 `HttpService.Options`；`HttpService.post` 与 `Options.withHeaders` 作为通用能力保留。
- `UsageCmd` 的头像获取改用宿主 `HttpService`，移除仅剩此一处使用的 `HttpUtils`。
- 关卡详情卡片的 `Rt.` / `Enj.` 标签加粗并使用次级色，评分值改为高亮斜体，`Enj.` 数值带 `/10` 后缀；同时拉开两项间距。
- 关卡详情卡片描述区右下角增加两个浅色平行四边形 `//` 背景装饰，底部与 GDDL 面板对齐。
- 关卡详情卡片统计数值按类型着色：下载绿色、点赞红色、长度蓝色、物体黄色；GDDL 有数据时长度显示具体时长，如 `Medium · 1:52`。
- B20 表格整行文字颜色随 tier 变化，沿用绿到蓝再到红的渐变。
- 缩短插件加载与上传的临时文件名，并在启动时清理遗留临时文件。
- `CmdArgs` 的长选项名必须至少两个字符、短选项别名必须是单个字母；`--B` / `-ban` 这类写法不再解析为选项，`hasOpt` / `getOpt` 传入不合法的 name / alias 会抛出 `IllegalArgumentException`。

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
