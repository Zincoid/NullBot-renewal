# Changelog

本项目的所有重要变更都记录在此文件。

## [Unreleased]

暂无。

## [1.3.0] - 2026-11-16

重构聊天附件与 AI 记忆，新增控制台消息页、记忆页，统一媒体指令，插件 API 升至 6。

### Added

- AI 情景记忆与长期记忆，群 / 用户设置开关默认关闭；关闭后停止注入与压缩，已有记忆保留。
- 控制台新增消息页与记忆页。
- 新增 `#Get` / `#Delete` 与 `#Image` / `#Audio` / `#Video` / `#Quote`，AI `send` 支持 `last`。
- 插件 API 6 文件发送、`UrlDownloader.download`、GD `#GD song`。
- QQ 官方适配器默认订阅并支持群成员加入 / 退出事件；无相关权限时可把 `nullbot.qq.intents` 改回 `33554432`。
- QQ 官方适配器支持通过 `message_reference` 引用回复文本 / Markdown / 媒体消息，并记录发送响应的 `ext_info.ref_idx`，可引用机器人自己发送的消息。

### Changed

- 聊天附件拆分为 `attachment` 表，新增 `memory` 表与消息状态；AI `read` / `view` / `search` 与 `#Save` 适配附件，`#GroupSet` 更名为 `#Setting`。
- 情景压缩改为后台任务，摘要上限 1024 字符，压缩输出默认 2048。
- 控制台导航与表格调整，`BizType` 简化，GD 默认只搜有星关卡。
- QQ 官方适配器群聊 @ 用户改用 Markdown 提及，C2C 保持纯文本。

### Removed

- `BizType.CHAT`、`FileService.imageUri`、`#Ping` / `#Mute` 与旧媒体指令。

### Fixed

- 限速、QQ 引用消息解析与头像获取、自身消息回显、`send` 记录与输出清理、聊天附件清理。
- QQ 官方适配器引用解析失败不再丢弃整条消息，群 @ 事件会移除机器人自身的提及标记，消息事件的 `reply` 标记按引用是否存在生成。
- QQ 群成员加入 / 退出事件模型与官方字段对齐，不再依赖事件中不存在的昵称和操作人字段。
- QQ 网关遇到 4013 / 4014 时自动退回群聊与 C2C intents 重连。
- QQ 卡片消息只有 `ark_data` 时不再被丢弃，并行消息 / 聊天记录（`message_type` 101/102）从 `msg_elements` 提取文本。
- QQ 引用回复在 `Reply` 段位于消息末尾时也能正确带上引用；解除禁言按官方要求传空 `mute_expire_at`。

## [1.2.0] - 2026-11-02

重构 AI 触发链路与用量统计，新增指令禁用和群 / 用户功能权限，插件 API 升至 5。

### Added

- 新增 `ai_usage` 按日汇总表；控制台概览新增 AI 子页（调用 / 词元趋势、来源与排行）和“累计”子页。
- `#指令 -B` / `-U` 支持整群、用户、私聊与 `--temp` 限时禁用；角色页新增指令 / AI 权限开关和指令管理。
- API 4 新增 `properties()` 与插件配置页；API 5 新增 `http()`，支持超时、代理、重试与响应上限。
- GD 插件新增本地进度与本地 B20；`#GDDL` 改为子指令并改用原始小数评分；搜索结果显示关卡 ID。

### Changed

- AI 触发改由 `AiChatMiddleware` 直接调用 `ChatPipeline`，移除 `#Chat` / `#Poked`；AI 用量改记入 `ai_usage`。
- `usage` 更名为 `cmd_usage`，新增 `cmd_disable`；群记录新增指令 / AI 开关，用户记录新增私聊开关。
- 插件 HTTP 请求统一走宿主服务；控制台表格、配置页与插件列表重排。

### Fixed

- 修复 AI 看不到发送者昵称、控制台日志断线不重连，以及失败日志中的敏感 URL。

## [1.1.0] - 2026-10-27

新增 QQ 官方机器人适配器、插件 API 2/3 与 JTE / JairoSVG 渲染管线。

### Added

- 新增 `adapter-qq` WebSocket 适配器，支持群 / C2C 私聊、主动消息、token 刷新与断线重连，构建产物为 `launcher-qq.jar`。
- API 2 支持插件注册 AI 工具；API 3 提供 SVG / JTE 渲染与字体注册；新增 `say-plugin` 与 `gd-plugin`。
- 新增 JTE + JairoSVG 渲染管线，内置 `Usage` / `Info` / `Help` 卡片；新增 `SvgText` / `SvgTable` / `ImageData` 等工具。

### Changed

- `adapter-qq` 配置键 `api-base` 更名为 `api-url`；内置插件版本跟随 Maven `revision`。
- 统一核心字体与插件结构；GD / GDDL 卡片和指令重做。
- 控制台群设置与用户设置弹窗分组，插件页展示 AI 工具。

### Fixed

- 头像占位、代码块外空行、SVG 列错位、日志 SSE 停机与 `Say` emoji 显示。

## [1.0.0] - 2026-10-25

首个正式版本，搭建完整的机器人框架、Web 控制台与插件体系。

### Added

- 多模块架构：`spi`、`core`、`adapters`、`launcher`、`plugins`。
- 适配器：Shiro（OneBot 11 正 / 反向 WS）与 stdin。
- 指令框架：前缀、别名、限权、限流、关键词触发、输入等待、帮助、调用统计。
- 消息总线：聊天记录、入退群通知、防撤回、自动聊天。
- AI 对话：OpenAI 兼容接口，群聊 @ / 私聊 / 戳一戳触发，支持提示词、推理强度、注入防护与工具调用。
- 媒体与语录：图片、视频、音频、语录的保存、获取、随机发送与删除。
- Web 控制台：概览、角色与群设置、关键词、语录、文件、配置、插件、系统信息与 SSE 日志。
- 插件系统：JAR 热插拔、上传 / 启停 / 重载 / 删除、独立数据目录。
- 内置插件：hello、gallery、game、pubg、query、sb6657、symmetry。
- 存储与配置：SQLite 自动建表、MySQL schema、`nullbot.*` 配置。

[Unreleased]: https://github.com/Zincoid/NullBot-renewal/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/Zincoid/NullBot-renewal/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/Zincoid/NullBot-renewal/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/Zincoid/NullBot-renewal/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/Zincoid/NullBot-renewal/releases/tag/v1.0.0
