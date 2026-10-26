# NullBot renewal

基于 Java 27 与 Spring Boot 4 的 QQ 机器人，业务与协议解耦，自带 Web 控制台与外部插件系统。

## 特性

- **适配器解耦**：业务只依赖 `spi` 的事件模型与 `BotClient`/`BotServer` 端口，内置 Shiro（OneBot 11）与 stdin 两种适配器。
- **指令框架**：可配置前缀、别名、限权（banned/restricted/normal/manager/admin）、限流、关键词触发、输入等待、帮助与调用统计。
- **AI 对话**：兼容 OpenAI 接口；群聊 @、私聊、戳一戳均可触发，支持按群/用户设置提示词、推理强度与注入防护，并提供工具调用（读消息、看图、搜索收藏、发送、网页、撤回、禁言、调用指令）。
- **消息能力**：图片/视频/音频/语录收藏与随机发送、入退群通知、防撤回、聊天历史、自动聊天。
- **图片渲染**：JTE 预编译模板配合 JairoSVG 输出 PNG，内置 `Usage`、`Info`、`Help` 图片卡片；插件可复用同一渲染管线并自带字体。
- **Web 控制台**：概览、角色与群设置、关键词、语录、文件、配置、插件管理、系统信息，以及常驻实时日志（SSE）。
- **外部插件**：JAR 热插拔，支持上传、启停、重载、删除与独立数据目录；插件可注册指令、AI 工具和图片渲染。

## 模块

| 模块 | 说明 |
| --- | --- |
| `spi` | 事件模型与机器人端口 |
| `core` | 指令、消息总线、AI、控制台、持久化、图片渲染 |
| `adapters` | `adapter-shiro`、`adapter-stdin` |
| `launcher` | Spring Boot 入口与打包 |
| `plugins` | 随仓库分发的外部插件 |

## 快速开始

需要 JDK 27。

```bash
./mvnw package
java -jar launcher/target/launcher-shiro.jar
```

默认使用 SQLite（首次启动自动建表），HTTP 端口随机。启动日志会打印控制台地址与访问令牌：

```text
Console UI: http://localhost:<port>/console/ui?token=<token>
```

## 配置

启动时默认激活 `prod` profile。复制 `application.yaml.template` 为运行目录下的 `application-prod.yaml` 后修改；也可直接使用环境变量（如 `NULLBOT_PORT`、`NULLBOT_CONSOLE_TOKEN`）或 `--nullbot.port=8080` 形式的启动参数。

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
| `nullbot.plugin.enabled` / `dir` | `false` / `./plugins` | 外部插件开关与目录 |

MySQL 需手动执行 `schema-mysql.sql`。

### 适配器

- `adapter-shiro`（默认）：OneBot 11 实现（如 NapCat）通过反向 WS 连接 `/ws`。
- `adapter-stdin`：从标准输入读取事件，便于调试。

```bash
./mvnw -P adapter-stdin package
java -jar launcher/target/launcher-stdin.jar
```

## 插件

### 开发

插件实现 `com.zincoid.nullbot.bot.plugin.Plugin`，一个 JAR 提供一个实现，并在 `META-INF/services/com.zincoid.nullbot.bot.plugin.Plugin` 中注册。`cmds()` 返回的 `Cmd` 使用 `@CmdMapping` 声明别名；不需要指令时可省略。插件还可通过 `tools()` 提供 AI 工具（`Tool`），工具名需全局唯一，建议使用 `<插件 ID>_<工具名>`。

| `apiVersion()` | 能力 |
| --- | --- |
| `1`（默认） | 指令 |
| `2` | 指令与 AI 工具 |
| `3` | 指令、AI 工具、图片渲染与字体 |

插件声明的 `apiVersion()` 必须在宿主支持的范围内（当前 `1`–`3`），否则加载失败。开发时依赖 `core`（`provided`）。完整示例见 `plugins/hello-plugin`。

### 插件渲染

`PluginContext.renderer()` 提供图片渲染能力：

- `svg(String)`：原始 SVG → PNG
- `template(String, Map)`：预编译 JTE 模板 → PNG
- `font(byte[])`：注册 TTF/OTF 字体

模板用 `jte-maven-plugin`（配置可参考 `core/pom.xml`，JTE 版本与宿主一致）从 `src/main/resources/templates/*.jte` 预编译进插件 JAR；模板参数名与 `.jte` 的 `@param` 一致。结果通常以 `base64://` 图片发送。使用渲染能力时 `apiVersion()` 返回 `3`。

插件 JAR 内 `fonts/` 下的 TTF/OTF 会在加载时自动注册；也可调用 `renderer().font(bytes)` 动态注册。字体注册是 JVM 全局的，插件卸载后不会撤销。`plugins/hello-plugin` 的 `Hello --image` 是完整模板示例。

### 运行

设置 `nullbot.plugin.enabled: true` 后，把插件 JAR 放入 `nullbot.plugin.dir`，或在控制台“插件”页上传；支持运行时启用、停用、重载与删除。插件数据目录由宿主创建，可通过 `PluginContext.dataDir()` 获取。

执行 `./mvnw package` 后，插件 JAR 会复制到本地 `workspace/plugins/`（该目录已被忽略），可将 `nullbot.plugin.dir` 指向它。

## 指令

默认前缀为 `#`，可通过 `#Config cmd_prefix <值>` 或控制台修改。`#Help`（别名 `#帮助`）以图片列出全部内置指令与已加载插件；`#Help -p=<插件>` 查看指定插件的指令；任意指令后跟 `-H`（如 `#Say -H`）查看该指令详情。

群聊中 @机器人、私聊直接发消息或戳一戳都会进入 AI 对话。

### 内置指令

| 类别 | 指令 |
| --- | --- |
| 系统 | `#Ping`、`#Echo`、`#Help`、`#Usage`、`#Info` |
| 媒体 | `#Save`，以及 `#ImageSave`/`#ImageGet`/`#ImageRand`/`#ImageDel` 与对应的视频、音频系列 |
| 语录 | `#QuoteSave`、`#QuoteGet`、`#QuoteRand`、`#QuoteDel` |
| 管理 | `#Config`、`#GroupSet`、`#History`、`#Mute`、`#Recall` |
| AI | `#Chat`、`#Prompt`（私聊） |

常用别名：`#帮助`、`#用量`、`#保存`、`#图片`、`#视频`、`#音频`、`#语录`、`#配置`、`#群设置`、`#历史`、`#禁言`、`#撤回`、`#对话`、`#提示词`。完整列表请发送 `#Help`。

### 插件指令

插件需启用并成功加载后才会注册指令：

| 插件 | 指令 |
| --- | --- |
| `hello` | `#Hello`（别名 `#你好`） |
| `gallery` | `#Anime`（别名 `#二次元`）、`#Zincoid`（别名 `#zn`、`#锌`） |
| `game` | `#Wife`（别名 `#今日老婆`）、`#Guess`（别名 `#猜`）、`#Quiz`（别名 `#问答`） |
| `say` | `#Say`（别名 `#说`） |
| `pubg` | `#PUBG`（别名 `#吃鸡地图`） |
| `query` | `#AA`（别名 `#模型查询`）、`#PRTS`（别名 `#PRTS查询`）、`#Trace`（别名 `#图溯源`、`#溯源`） |
| `sb6657` | `#SB6657`（别名 `#6657`、`#玩机器`、`#玩机器烂梗`） |
| `symmetry` | `#Symmetry`（别名 `#图像对称`、`#对称`） |

### 图片指令

- `#Usage` / `#用量`：当前用户的历史指令用量卡片
- `#Info`：系统信息
- `#Help` / `#帮助`：内置指令与插件列表；`#Help -p=<插件>` 查看指定插件
- `#Say` / `#说`：`say-plugin` 提供的随机语录卡片，左侧作者头像渐隐，可通过 `@用户` 或用户 ID 指定作者

## 字体

核心字体及其 OFL 许可证位于 `core/src/main/resources/fonts/`：

| 字体 | 用途 |
| --- | --- |
| JetBrains Mono | 标题、标签和数值 |
| Noto Sans Mono CJK SC | 含中文的值文本，保证中英混排 |

插件字体会随插件 JAR 放在各自插件的 `fonts/` 下，并在加载时自动注册：

| 字体 | 插件 | 用途 |
| --- | --- | --- |
| Noto Serif CJK SC（OFL） | `say-plugin` | `Say` 语录卡片，斜体中英混排 |
| Noto Emoji（OFL） | `say-plugin` | `Say` 语录卡片中的 emoji 文本 |
| Bernard MT Condensed | `hello-plugin` | `Hello --image` 标题示例 |

字体注册是 JVM 全局的，插件卸载后不会撤销。`Bernard MT Condensed` 为商业字体，分发前请确认授权。

## 许可

[AGPL-3.0](LICENSE) © Zincoid
