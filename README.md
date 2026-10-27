# NullBot renewal

基于 Java 27 与 Spring Boot 4 的 QQ 机器人，业务与协议解耦，自带 Web 控制台与外部插件系统。

当前版本：1.1.0 · [变更日志](CHANGELOG.md)

## 特性

- **适配器解耦**：业务只依赖 `spi` 的事件模型与 `BotClient`/`BotServer` 端口，内置 Shiro（OneBot 11）、QQ 官方机器人与 stdin 三种适配器。
- **指令框架**：可配置前缀、别名、限权、限流、关键词触发、输入等待、帮助与调用统计。
- **AI 对话**：兼容 OpenAI 接口；群聊 @、私聊、戳一戳均可触发，支持群/用户级提示词、推理强度、注入防护与工具调用。
- **消息能力**：图片/视频/音频/语录收藏与随机发送、入退群通知、防撤回、聊天历史、自动聊天。
- **图片渲染**：JTE 预编译模板配合 JairoSVG 输出 PNG，内置 `Usage`、`Info`、`Help` 图片卡片；插件可复用同一渲染管线并自带字体。
- **Web 控制台**：概览、角色与群设置、关键词、语录、文件、配置、插件、系统信息，以及 SSE 实时日志。
- **外部插件**：JAR 热插拔，支持上传、启停、重载、删除与独立数据目录；插件可注册指令、AI 工具与图片渲染。

## 模块

| 模块 | 说明 |
| --- | --- |
| `spi` | 事件模型与机器人端口 |
| `core` | 指令、消息总线、AI、控制台、持久化、图片渲染 |
| `adapters` | `adapter-shiro`、`adapter-qq`、`adapter-stdin` |
| `launcher` | Spring Boot 入口与打包 |
| `plugins` | 随仓库分发的外部插件 |

## 快速开始

需要 JDK 27。

```bash
./mvnw package
java -jar launcher/target/launcher-shiro.jar
```

默认使用 SQLite（首次启动自动建表），HTTP 端口随机；启动日志会打印控制台地址与访问令牌：

```text
Console UI: http://localhost:<port>/console/ui?token=<token>
```

调试时可改用 stdin 适配器：

```bash
./mvnw -P adapter-stdin package
java -jar launcher/target/launcher-stdin.jar
```

QQ 官方机器人使用 WebSocket 接入，需要先在 [QQ 开放平台](https://bot.q.qq.com/) 创建机器人并取得 AppID/AppSecret：

```bash
./mvnw -P adapter-qq package
java -jar launcher/target/launcher-qq.jar \
  --nullbot.qq.app-id=<AppID> \
  --nullbot.qq.app-secret=<AppSecret>
```

生产环境建议使用 systemd 或 screen 托管进程，并配置数据库、日志与控制台令牌。

## 配置

默认激活 `prod` profile。参考 `application.yaml.template` 在运行目录创建 `application-prod.yaml`，也可使用环境变量（如 `NULLBOT_PORT`、`NULLBOT_CONSOLE_TOKEN`）或 `--nullbot.port=8080` 形式覆盖。

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `nullbot.port` | `0` | HTTP 端口，`0` 表示随机 |
| `nullbot.console.token` | 空 | 控制台令牌，空则随机生成并打印 |
| `nullbot.docs.enabled` | `false` | OpenAPI 文档开关，开启后访问 `/swagger-ui.html` |
| `nullbot.log.path` / `nullbot.log.level` | `./logs/output.log` / `INFO` | 日志文件与业务日志级别 |
| `nullbot.db.type` | `sqlite` | `sqlite` 或 `mysql` |
| `nullbot.db.sqlite.path` | `./data/sqlite.db` | SQLite 文件 |
| `nullbot.db.mysql.*` | - | MySQL 主机、端口、库名、账号、密码 |
| `nullbot.file.root` | `./files` | 文件存储根目录 |
| `nullbot.shiro.token` | 空 | Shiro 适配器访问令牌 |
| `nullbot.shiro.server.enable` / `url` | `true` / `/ws` | 反向 WS |
| `nullbot.shiro.client.enable` / `url` | `false` / 空 | 正向 WS，启用时需关闭反向 |
| `nullbot.qq.app-id` / `app-secret` | 空 | QQ 官方机器人 AppID 与 AppSecret |
| `nullbot.qq.api-url` | `https://api.bot.qq.com` | QQ 官方 OpenAPI 地址 |
| `nullbot.qq.token-url` | `https://api.bot.qq.com/app/getAppAccessToken` | Access token 获取地址 |
| `nullbot.qq.intents` | `33554432` | 事件订阅位掩码（群聊与 C2C） |
| `nullbot.qq.shard` / `shard-count` | `0` / `1` | WebSocket 分片编号与总数，通常无需修改 |
| `nullbot.qq.text-limit` | `1800` | 单条文本的最大 Unicode code point 数 |
| `nullbot.plugin.enabled` / `dir` | `true` / `./plugins` | 外部插件开关与目录 |

MySQL 需手动执行 `schema-mysql.sql`。

### 适配器

- `adapter-shiro`（默认）：OneBot 11 实现（如 NapCat）通过反向 WS 连接 `/ws`。
- `adapter-qq`：QQ 官方机器人（`bot.q.qq.com`）WebSocket 接入，支持 QQ 群与 C2C 私聊；产物为 `launcher-qq.jar`。
- `adapter-stdin`：从标准输入读取事件，便于调试。

QQ 官方机器人适配器的回复统一以主动消息发送（不携带 `msg_id`），因此不会触发被动回复的自动 @；代价是受开放平台认证、权限与频控限制，额度以平台后台为准，没有主动消息权限时发送会失败。群内消息默认只有 @机器人 时才会收到；全量群消息需要在开放平台和群设置中授权。

受官方接口限制：

- 群信息、群成员列表为白名单/内邀能力，普通机器人通常无法获取；成员没有头像，`#Usage`、`#Say` 等卡片会使用占位头像或省略头像，`#Wife` 成员模式可能没有可选成员。
- 没有群消息历史；引用回复、`#Save`/`#Recall` 依赖被引用消息的场景受限。适配器会缓存收到的消息，供 `#Save --id=<消息ID>` 等使用。
- 不处理入退群、戳一戳、防撤回等通知类事件。
- 图片上传支持 png/jpg，视频 mp4，语音 silk/mp3/wav；本地文件大于 5 MB 首版不支持；WebP/GIF 可能上传失败。
- 文本中的 `@用户` 不会高亮，适配器以 `@<openid>` 形式显示；真正的群内 @ 需要 Markdown，首版不支持。
- 文本中包含 URL 时需要在开放平台配置消息 URL 白名单，否则平台会拒绝发送。
- 群与用户 ID 使用平台 openid 体系，不是 QQ 号；不同机器人的 openid 不通用。
- 当前 v2 使用统一 `api.bot.qq.com`，不再提供旧版 sandbox 域名；测试用户、测试群和 IP 白名单请在开放平台后台配置。`api-url` 仅用于特殊网络/旧域名兼容。

## 插件

插件实现 `com.zincoid.nullbot.bot.plugin.Plugin`，一个 JAR 提供一个实现，并在 `META-INF/services/com.zincoid.nullbot.bot.plugin.Plugin` 中注册；可注册指令、AI 工具与图片渲染，支持 JAR 热插拔、独立数据目录和运行时启停。

- 开发指南：[PLUGIN.md](PLUGIN.md)
- 入门示例：[plugins/hello-plugin](plugins/hello-plugin)
- 完整示例：[plugins/gd-plugin](plugins/gd-plugin)、[plugins/query-plugin](plugins/query-plugin)

插件默认启用；将 JAR 放入 `nullbot.plugin.dir`（默认 `./plugins`），或在控制台“插件”页上传，成功加载后才会注册指令。执行 `./mvnw package` 后，声明了复制步骤的插件 JAR 会进入 `workspace/plugins/`。控制台“插件数据”弹窗分为“配置”和“文件”两页：配置页编辑 `config.properties` 并可保存后重载插件，文件页管理插件数据文件。

## 指令

默认前缀为 `#`，可通过 `#Config cmd_prefix <值>` 或控制台修改。`#Help`（别名 `#帮助`）以图片列出全部内置指令与已加载插件；`#Help -p=<插件>` 查看指定插件的指令；任意指令后跟 `-H`（如 `#Say -H`）查看该指令详情。

群聊中 @机器人 时优先匹配指令，未命中指令才进入 AI 对话；私聊消息与戳一戳（适配器支持时）也会进入 AI 对话。

### 内置指令

| 类别 | 指令 |
| --- | --- |
| 系统 | `#Ping`、`#Echo`、`#Help`、`#Usage`、`#Info` |
| 媒体 | `#Save`，以及 `#ImageSave`/`#ImageGet`/`#ImageRand`/`#ImageDel` 与对应的视频、音频系列 |
| 语录 | `#QuoteSave`、`#QuoteGet`、`#QuoteRand`、`#QuoteDel` |
| 管理 | `#Config`、`#GroupSet`、`#History`、`#Mute`、`#Recall` |
| AI | `#Chat`、`#Prompt`（私聊） |

`#Usage`、`#Info`、`#Help` 与 `#Say` 输出图片卡片。完整指令、别名与限权请发送 `#Help` 查看。

### 插件指令

插件需启用并成功加载后才会注册指令：

| 插件 | 指令 |
| --- | --- |
| `hello` | `#Hello`（别名 `#你好`） |
| `gallery` | `#Anime`（别名 `#二次元`）、`#Zincoid`（别名 `#zn`、`#锌`） |
| `game` | `#Wife`（别名 `#今日老婆`）、`#Guess`（别名 `#猜`）、`#Quiz`（别名 `#问答`） |
| `say` | `#Say`（别名 `#说`；支持 `@用户`、用户 ID、`--id=<语录ID>`） |
| `gd` | `#GD`（别名 `#几何冲刺`）、`#GDDL` |
| `pubg` | `#PUBG`（别名 `#吃鸡地图`） |
| `query` | `#AA`（别名 `#模型查询`）、`#PRTS`（别名 `#PRTS查询`）、`#Trace`（别名 `#图溯源`、`#溯源`） |
| `sb6657` | `#SB6657`（别名 `#6657`、`#玩机器`、`#玩机器烂梗`） |
| `symmetry` | `#Symmetry`（别名 `#图像对称`、`#对称`） |

`query` 插件需在数据目录的 `config.properties` 中配置 `aa.api-key` 与 `saucenao.api-key`；首次加载会自动生成默认配置，模板见 `plugins/query-plugin/config.properties.example`。

### GD 指令

`#GD`/`#几何冲刺` 提供 Geometry Dash 关卡搜索、详情与本地进度，`#GDDL` 提供 GDDL B20 评分；搜索结果、关卡详情、本地 B20 与 GDDL B20 均以图片卡片输出：

- `#GD search <关键词> [--diff=...] [--demon=...] [--length=...] [--filter=...] [--page=N]`：搜索关卡，回复序号查看详情，回复 `next` 翻到下一页。
- `#GD level <关卡ID>`：按 ID 查看关卡详情；卡片内嵌社区缩略图（WebP 自动转为 PNG）。
- `#GD register`：注册本地进度用户；`update`、`remove`、`b20` 需要先注册。
- `#GD update <关卡ID> <进度>`：通过 JDash 验证关卡存在后，记录或更新 1–100 的进度；回复会显示关卡名和作者。
- `#GD remove <关卡ID>`：删除本地进度记录。
- `#GD b20 [--all]`：用本地记录和 GDDL 关卡评级计算本地 B20；默认显示前 5 条，`--all` 显示完整 20 条。
- `#GD unregister`：注销本地用户并删除全部进度；有记录时会先要求确认。
- `#GDDL b20 [GDDL用户名|ID] [--all]`：查询指定 GDDL 用户的 B20；不带用户名时查询当前绑定账号。默认显示前 5 条，`--all` 显示完整 20 条。
- `#GDDL bind <GDDL用户名|ID>`：绑定当前聊天用户的 GDDL 账号；已有绑定不会覆盖。
- `#GDDL unbind`：解除当前聊天用户的 GDDL 绑定。

本地进度保存在插件数据目录的 `gd.db`（SQLite）；注册用户、关卡名/作者快照与进度记录都在其中。本地 B20 使用自报进度，关卡评级来自 GDDL，**不是 GDDL 官方记录**；该关卡在 GDDL 未评级时会被跳过并提示数量。GDDL 评级缓存默认已评级 24 小时、未评级 1 小时，可通过 `gddl.tier-cache-ttl` / `gddl.unrated-cache-ttl` 调整。首次加载会自动在数据目录生成默认 `config.properties`，已有文件不会覆盖。

B20 单关 `rt = rating^E × (进度/100)^1.5`，其中 `rating` 使用 GDDL 原始小数评分，取最高的 20 条求平均；`E` 默认 1.5，可在插件数据目录的 `config.properties` 中设置 `score.tier-exponent=2` 使用平方。B20 列表的 tier 列显示四舍五入后的整数和原始评分，例如 `25(25.33)`。GDDL 用户评分仅统计 GDDL 已提交的记录（`beaten`/`beating`），未提交到 GDDL 的关卡不计入。GDDL 绑定记录保存在插件数据目录的 `gddl-bindings.properties`，每个聊天用户只能绑定一个账号，更换前需先解绑。

GDDL 与缩略图请求支持可选配置：`http.connect-timeout`（秒，默认 8）、`http.request-timeout`（秒，默认 15）、`http.retries`（默认 1）、`http.proxy`（`host:port`，可选 HTTP 代理，暂不支持认证）、`cache-ttl`（秒，默认 600）、`gddl.tier-cache-ttl`（秒，默认 86400）、`gddl.unrated-cache-ttl`（秒，默认 3600）、`gddl.api-base`、`gddl.avatar-base`、`thumbnail.base`、`http.user-agent`。服务器网络较慢时可使用代理或镜像地址，并适当增大超时；缩略图、GDDL 头像和本地 B20 头像成功获取后会缓存。配置模板见 `plugins/gd-plugin/config.properties.example`。

## 字体

核心字体及其 OFL 许可证位于 `core/src/main/resources/fonts/`：

| 字体 | 用途 |
| --- | --- |
| JetBrains Maple Mono NF（OFL） | 核心渲染管线的默认字体，中英文混排；粗体通过 `font-weight="bold"` 合成 |
| Noto Emoji（OFL） | 渲染文本中的 emoji 回退，按文本段自动切换 |

JetBrains Maple Mono NF 由 JetBrains Mono 与 Maple Mono 合成，并包含 Nerd Font 图标。

插件字体会随插件 JAR 放在各自插件的 `fonts/` 下，并在加载时自动注册：

| 字体 | 插件 | 用途 |
| --- | --- | --- |
| Noto Serif CJK SC（OFL） | `say-plugin` | `Say` 语录卡片，斜体中英混排 |
| Bernard MT Condensed | `hello-plugin` | `Hello --image` 标题示例 |

字体注册是 JVM 全局的，插件卸载后不会撤销。`Bernard MT Condensed` 为商业字体，分发前请确认授权。

## 文档

- [插件开发指南](PLUGIN.md)
- [变更日志](CHANGELOG.md)

## 许可

[AGPL-3.0](LICENSE) © Zincoid
