# NullBot renewal

基于 Java 27 与 Spring Boot 4 的 QQ 机器人，业务与协议解耦，自带 Web 控制台与外部插件系统。

## 特性

- **适配器解耦**：业务只依赖 `spi` 的事件模型与 `BotClient`/`BotServer` 端口，内置 Shiro（OneBot 11）与 stdin 两种适配器。
- **指令框架**：可配置前缀、别名、限权（banned/restricted/normal/manager/admin）、限流、关键词触发、输入等待、帮助与调用统计。
- **AI 对话**：兼容 OpenAI 接口；群聊 @、私聊、戳一戳均可触发，支持按群/用户设置提示词、推理强度与注入防护，并提供工具调用（读消息、看图、搜索收藏、发送、网页、撤回、禁言、调用指令）。
- **消息能力**：图片/视频/音频/语录收藏与随机发送、入退群通知、防撤回、聊天历史、自动聊天。
- **图片渲染**：JTE 预编译模板配合 JairoSVG 输出 PNG，内置 `Usage`、`Info`、`Help` 图片卡片，插件可复用同一渲染管线。
- **Web 控制台**：概览、角色与群设置、关键词、语录、文件、配置、插件管理、系统信息，以及常驻实时日志（SSE）。
- **外部插件**：JAR 热插拔，支持上传、启停、重载、删除与独立数据目录。

## 模块

| 模块 | 说明 |
| --- | --- |
| `spi` | 事件模型与机器人端口 |
| `core` | 指令、消息总线、AI、控制台、持久化 |
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

```
Console UI: http://localhost:<port>/console/ui?token=<token>
```

## 配置

复制 `application.yaml.template` 为运行目录下的 `application-prod.yaml` 后修改；也可直接使用环境变量（如 `NULLBOT_PORT`、`NULLBOT_CONSOLE_TOKEN`）或 `--nullbot.port=8080` 形式的启动参数。

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `nullbot.port` | `0` | HTTP 端口，`0` 表示随机 |
| `nullbot.console.token` | 空 | 控制台令牌，空则随机生成并打印 |
| `nullbot.docs.enabled` | `false` | OpenAPI 文档 |
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

插件实现 `com.zincoid.nullbot.bot.plugin.Plugin`，并在 `META-INF/services/com.zincoid.nullbot.bot.plugin.Plugin` 中注册；`cmds()` 返回的 `Cmd` 使用 `@CmdMapping` 声明别名，不需要指令时可省略。插件还可通过 `tools()` 提供 AI 工具（`Tool`），工具名需全局唯一，建议使用 `<插件 ID>_<工具名>`；提供工具时 `apiVersion()` 至少为 `2`。开发时依赖 `core`（`provided`），可参考 `plugins/hello-plugin`。

### 插件渲染

`PluginContext.renderer()` 提供图片渲染能力：

- `svg(String)`：原始 SVG → PNG
- `template(String, Map)`：预编译 JTE 模板 → PNG
- `font(byte[])`：注册 TTF/OTF 字体

模板用 `jte-maven-plugin`（配置可参考 `core/pom.xml`，JTE 版本与宿主一致）从 `src/main/resources/templates/*.jte` 预编译进插件 JAR；模板参数名与 `.jte` 的 `@param` 一致。结果通常以 `base64://` 图片发送。使用渲染能力时 `apiVersion()` 返回 `3`。

插件 JAR 内 `fonts/` 下的 TTF/OTF 会在加载时自动注册；也可调用 `renderer().font(bytes)` 动态注册。字体注册是 JVM 全局的，插件卸载后不会撤销。`plugins/hello-plugin` 的 `Hello --image` 是完整模板示例。

### 插件运行

启用插件后，把 JAR 放入 `nullbot.plugin.dir`，或在控制台“插件”页上传；支持运行时启用、停用、重载与删除。插件数据目录由宿主创建，可通过 `PluginContext.dataDir()` 获取。

## 指令

默认前缀为 `#`，可通过 `#Config cmd_prefix <值>` 或控制台修改。`#Help` 以图片列出全部内置指令与已加载插件；`#Help -p=<插件>` 查看指定插件的指令。

- 群聊中 @机器人 或私聊直接发消息会进入 AI 对话；戳一戳也会触发。
- 插件示例：`#Hello`、`#Anime`、`#Guess`、`#Quiz`、`#Wife`、`#PUBG`、`#AA`、`#PRTS`、`#Trace`、`#SB6657`、`#Symmetry`。
- 内置图片指令：`#Usage`（别名 `#用量`）渲染当前用户的历史指令用量卡片；`#Info` 渲染系统信息；`#Help`（别名 `#帮助`）渲染指令与插件列表。

## 字体

内置渲染使用以下 OFL 字体，文件和许可证位于 `core/src/main/resources/fonts/`：

- JetBrains Mono：标题、标签和数值
- Noto Sans Mono CJK SC：包含中文的值文本，保证中英混排正常显示

## 许可

[AGPL-3.0](LICENSE) © Zincoid
