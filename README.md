# NullBot renewal

基于 Java 27 与 Spring Boot 4 的 QQ 机器人，业务与协议解耦，自带 Web 控制台与外部插件系统。

当前版本：1.1.0 · [变更日志](CHANGELOG.md)

## 特性

- **适配器解耦**：业务只依赖 `spi` 的事件模型与 `BotClient`/`BotServer` 端口，内置 Shiro（OneBot 11）、QQ 官方机器人与 stdin 三种适配器。
- **指令框架**：可配置前缀、别名、限权、限流、关键词触发、输入等待、帮助与调用统计。
- **AI 对话**：兼容 OpenAI 接口；群聊 @、私聊、戳一戳均可触发，支持群/用户级提示词、推理强度、注入防护与工具调用。
- **消息能力**：图片/视频/音频/语录收藏与随机发送、入退群通知、防撤回、聊天历史、自动聊天。
- **图片渲染**：JTE 预编译模板配合 JairoSVG 输出 PNG，内置 `Usage`、`Info`、`Help` 图片卡片；插件可复用同一渲染管线并自带字体。
- **Web 控制台**：概览、角色与群设置、关键词与指令管理、语录、文件、配置、插件、系统信息，以及 SSE 实时日志。
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

按适配器切换构建 profile：

| 适配器 | 构建 | 产物 |
| --- | --- | --- |
| Shiro（默认） | `./mvnw package` | `launcher-shiro.jar` |
| stdin | `./mvnw -P adapter-stdin package` | `launcher-stdin.jar` |
| QQ 官方 | `./mvnw -P adapter-qq package` | `launcher-qq.jar` |

QQ 官方机器人需要先在 [QQ 开放平台](https://bot.q.qq.com/) 创建机器人并取得 AppID/AppSecret：

```bash
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
| `nullbot.plugin.enabled` / `dir` | `true` / `./plugins` | 外部插件开关与目录 |
| `nullbot.shiro.token` | 空 | Shiro 适配器访问令牌 |
| `nullbot.shiro.server.enable` / `url` | `true` / `/ws` | 反向 WS |
| `nullbot.shiro.client.enable` / `url` | `false` / 空 | 正向 WS，启用时需关闭反向 |
| `nullbot.qq.app-id` / `app-secret` | 空 | QQ 官方机器人 AppID 与 AppSecret |
| `nullbot.qq.api-url` | `https://api.bot.qq.com` | QQ 官方 OpenAPI 地址 |
| `nullbot.qq.token-url` | `https://api.bot.qq.com/app/getAppAccessToken` | Access token 获取地址 |
| `nullbot.qq.intents` | `33554432` | 事件订阅位掩码（群聊与 C2C） |
| `nullbot.qq.shard` / `shard-count` | `0` / `1` | WebSocket 分片编号与总数 |
| `nullbot.qq.text-limit` | `1800` | 单条文本的最大 Unicode code point 数 |

MySQL 需手动执行 `schema-mysql.sql`。

## 适配器

- `adapter-shiro`（默认）：OneBot 11 实现（如 NapCat）通过反向 WS 连接 `/ws`。
- `adapter-qq`：QQ 官方机器人（`bot.q.qq.com`）WebSocket 接入，支持 QQ 群与 C2C 私聊。
- `adapter-stdin`：从标准输入读取事件，便于调试。

QQ 官方机器人统一以主动消息发送（不携带 `msg_id`），不会触发被动回复的自动 @；额度受开放平台认证、权限与频控限制。群内默认只有 @机器人 时才会收到消息，全量群消息需在开放平台和群设置中授权。其他限制：

- 群信息、群成员列表为白名单/内邀能力，成员没有头像；`#Usage`、`#Say` 等卡片使用占位头像或省略头像，`#Wife` 成员模式可能没有可选成员。
- 没有群消息历史；引用回复、`#Save`/`#Recall` 等依赖被引用消息的场景受限。适配器会缓存收到的消息，供 `#Save --id=<消息ID>` 使用。
- 不处理入退群、戳一戳、防撤回等通知类事件。
- 图片支持 png/jpg，视频 mp4，语音 silk/mp3/wav；本地文件大于 5 MB 首版不支持，WebP/GIF 可能上传失败。
- 群与用户 ID 使用平台 openid 体系，不是 QQ 号，不同机器人的 openid 不通用；文本中的 `@用户` 以 `@<openid>` 显示。
- 文本中包含 URL 时需要在开放平台配置消息 URL 白名单；当前统一使用 `api.bot.qq.com`，测试用户、测试群和 IP 白名单在开放平台后台配置，`api-url` 仅用于特殊网络/旧域名兼容。

## 指令

默认前缀为 `#`，可通过 `#Config cmd_prefix <值>` 或控制台修改。`#Help`（别名 `#帮助`）以图片列出全部内置指令与已加载插件；`#Help -p=<插件>` 查看指定插件；任意指令后跟 `-H`（如 `#Say -H`）查看该指令详情。

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

插件需启用并成功加载后才会注册指令；各插件的详细用法与配置见插件目录下的 README：

| 插件 | 指令 |
| --- | --- |
| [`hello`](plugins/hello-plugin/README.md) | `#Hello`（别名 `#你好`） |
| [`gallery`](plugins/gallery-plugin/README.md) | `#Anime`（别名 `#二次元`）、`#Zincoid`（别名 `#zn`、`#锌`） |
| [`game`](plugins/game-plugin/README.md) | `#Wife`（别名 `#今日老婆`）、`#Guess`（别名 `#猜`）、`#Quiz`（别名 `#问答`） |
| [`say`](plugins/say-plugin/README.md) | `#Say`（别名 `#说`；支持 `@用户`、用户 ID、`--id=<语录ID>`） |
| [`gd`](plugins/gd-plugin/README.md) | `#GD`（别名 `#几何冲刺`）、`#GDDL` |
| [`pubg`](plugins/pubg-plugin/README.md) | `#PUBG`（别名 `#吃鸡地图`） |
| [`query`](plugins/query-plugin/README.md) | `#AA`（别名 `#模型查询`）、`#PRTS`（别名 `#PRTS查询`）、`#Trace`（别名 `#图溯源`、`#溯源`） |
| [`sb6657`](plugins/sb6657-plugin/README.md) | `#SB6657`（别名 `#6657`、`#玩机器`、`#玩机器烂梗`） |
| [`symmetry`](plugins/symmetry-plugin/README.md) | `#Symmetry`（别名 `#图像对称`、`#对称`） |

### 指令禁用

直接使用 `#指令 -B` 可在当前会话禁用该指令，`#指令 -U` 解除：

- 群聊：`#Guess -B` 禁用整群，`#Guess -B @用户` 只禁用该用户；
- 私聊：`#Guess -B --user=<用户ID>` 禁用指定用户，必须指定 `--user`（管理员，且已开启私聊）；
- 限时：`#Guess -B --temp=30m` 或 `#Guess -B @用户 --temp=30m`，支持 `m`、`h`、`d`；不填 `--temp` 为永久；
- 解除：`#Guess -U` 解除整群规则，`#Guess -U @用户` 或 `#Guess -U --user=<用户ID>` 解除个人规则；
- 被禁用时会提示剩余时间；群规则不影响私聊，私聊规则不影响群规则。

控制台“角色”页的群设置与用户设置也可以查看、添加和解除这些规则。

`-B` / `-U` 是宿主保留参数，插件指令不要使用。

## 插件

插件实现 `com.zincoid.nullbot.bot.plugin.Plugin`，一个 JAR 提供一个实现，并在 `META-INF/services/com.zincoid.nullbot.bot.plugin.Plugin` 中注册；可注册指令、AI 工具与图片渲染，支持 JAR 热插拔、独立数据目录和运行时启停。

- 开发指南：[PLUGIN.md](PLUGIN.md)
- 入门示例：[`plugins/hello-plugin`](plugins/hello-plugin)
- 完整示例：[`plugins/gd-plugin`](plugins/gd-plugin)、[`plugins/query-plugin`](plugins/query-plugin)
- 各插件的详细用法与配置：见上方插件表或插件目录下的 README

插件默认启用；将 JAR 放入 `nullbot.plugin.dir`（默认 `./plugins`），或在控制台“插件”页上传，成功加载后才会注册指令。执行 `./mvnw package` 后，声明了复制步骤的插件 JAR 会进入 `workspace/plugins/`。控制台“插件数据”弹窗分为“配置”和“文件”两页：配置页编辑 `config.properties` 并可保存后重载插件，文件页管理插件数据文件。

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
