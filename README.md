# NullBot Renewal

基于 Spring Boot 4 和 Java 27 的聊天机器人，采用适配器架构和多模块 Maven 工程。支持 Shiro（OneBot / QQ）与 stdin 调试适配器、外部插件、AI 对话、控制台管理，以及 MySQL / SQLite。

## 特性

- **适配器解耦**：核心只依赖 `spi`，新增机器人框架只需新增 `adapter-*` 模块和对应构建 profile。
- **指令框架**：指令注册、别名、权限、用量统计和限速责任链。
- **外部插件**：独立 JAR 通过 `ServiceLoader` 加载，按插件隔离数据目录；控制台可查看状态并在运行时启用、禁用、重载。
- **AI 能力**：OpenAI 兼容接口、工具调用、消息窗口记忆、提示词注入防护。
- **控制台**：登录、概览、配置、文件、角色/群设置、关键字、语录、插件管理和关于信息。
- **双数据库**：MySQL / SQLite，SQLite 启动自动建表。
- **中英双语**：指令帮助和机器人回复支持中文 / 英文。

## 技术栈

Java 27 · Spring Boot 4.0.8 · MyBatis-Plus 3.5.17 · Shiro 2.5.5 · Maven Wrapper（Maven 3.10.0）

## 项目结构

| 路径 | 说明 |
| --- | --- |
| `spi` | 框架无关的事件、消息模型和 `BotClient` / `BotServer` 端口 |
| `core` | 核心应用：`platform`、`module`、`bot`、`console` |
| `adapters/adapter-shiro` | Shiro（QQ / OneBot）适配器 |
| `adapters/adapter-stdin` | 终端调试适配器 |
| `launcher` | 启动类、配置和可执行 JAR 打包（按 Maven profile 选择适配器） |
| `plugins/hello-plugin` | 示例插件（`Hello` / `你好`） |
| `plugins/sb6657-plugin` | 随机玩机器烂梗 |
| `plugins/pubg-plugin` | PUBG 资源地图 |
| `plugins/query-plugin` | AA、PRTS 与图片溯源查询 |
| `plugins/symmetry-plugin` | 图片对称 |

依赖方向：`adapter-* -> spi`，`core -> spi`，`launcher -> core + 一个由 Maven profile 选择的 adapter`。

## 快速开始

### 环境

- JDK 27
- 无需安装 Maven，仓库自带 `mvnw`（Windows 使用 `mvnw.cmd`）
- MySQL 8+ 可选，默认 SQLite

### 构建

```bash
# 默认构建 Shiro 适配器
./mvnw -DskipTests package

# 构建 stdin 适配器
./mvnw -DskipTests -Padapter-stdin package
```

主要产物：

- `launcher/target/launcher-shiro.jar`：Shiro 适配器可执行 JAR（默认构建）
- `launcher/target/launcher-stdin.jar`：stdin 适配器可执行 JAR（使用 `-Padapter-stdin` 构建）
- `core/target/core-0.0.1-SNAPSHOT.jar`：核心 JAR，供插件编译
- `plugins/*/target/*.jar`：外部插件 JAR，构建后自动复制到 `workspace/plugins/`

两个 profile 的产物文件名不同，可以分别构建后同时保留。

### 配置

```bash
cp application.yaml.template application-prod.yaml
```

外部配置统一使用 `nullbot.*`，基础配置会映射到 `bot.*`。`application-dev.yaml` / `application-prod.yaml` 已被 gitignore。

`launcher` 的基础配置只保留共享配置；适配器自己的连接配置由对应模块的 `adapter.yaml` 提供（Shiro 为 `adapters/adapter-shiro/src/main/resources/adapter.yaml`），launcher 通过 `spring.config.import: optional:classpath:adapter.yaml` 加载。每个适配器模块都会提供这个文件，`optional:` 主要是避免 IDE 跨模块资源检查报错；一次构建只包含一个适配器，切换适配器需要重新构建。

常用配置：

```yaml
nullbot:
  port: 8080              # 0 表示随机端口
  console:
    token: change-me      # 留空则每次启动随机生成
  db:
    type: sqlite
    sqlite:
      path: ./workspace/data/sqlite.db
  plugin:
    enabled: true         # 是否加载 plugins 目录中的 JAR
    dir: ./workspace/plugins
```

- 指令前缀默认为 `#`，可在控制台「配置」中修改 `cmd_prefix`。
- SQLite 启动时自动执行 `core/src/main/resources/schema/schema-sqlite.sql`。
- MySQL 需要手动执行根目录的 `schema-mysql.sql`，配置缺失或连接失败会直接启动失败。
- `nullbot.plugin.enabled=false` 时不会加载任何外部插件，控制台「插件」页只能查看插件目录。

### 运行

从仓库根目录启动默认 Shiro 构建：

```bash
java -jar launcher/target/launcher-shiro.jar
```

启动 stdin 构建：

```bash
./mvnw -DskipTests -Padapter-stdin package
java -jar launcher/target/launcher-stdin.jar
```

配置里的相对路径以运行目录为基准，建议从仓库根目录启动。也可以在运行目录放置 `application.yaml`，或用 `--spring.config.additional-location` 指定配置位置。

开发运行与测试：

```bash
./mvnw -DskipTests install

# Shiro（默认）
./mvnw -pl launcher spring-boot:run

# stdin
./mvnw -pl launcher -Padapter-stdin spring-boot:run

./mvnw test
./mvnw test -Padapter-stdin
```

## 控制台

启动日志会输出带 token 的地址：

```text
Console UI: http://localhost:<port>/console/ui?token=...
```

浏览器打开该地址即可进入控制台；未登录时会跳转到登录页。token 只保存在当前浏览器的 `localStorage`，右上角「退出」会清除。

控制台包含以下视图：

| 视图 | 内容 |
| --- | --- |
| 概览 | 调用趋势、活跃用户/群、指令与群排行 |
| 配置 | 运行时参数、默认值、重置与 secret 掩码 |
| 文件 | 批量上传、类型/关键词检索、编辑、下载、删除 |
| 角色 | 用户/群列表、权限、私聊开关、群设置、关键词规则 |
| 语录 | 语录检索与删除 |
| 插件 | 插件状态、指令列表、运行时启用/禁用/重载 |

右上角的「关于」弹窗展示应用名称、作者、构建版本、构建时间、Java 与 Spring Boot 版本。构建信息由 `spring-boot-maven-plugin` 的 `build-info` 目标生成；通过 IDE 或未打包方式运行时可能显示 `dev`。

### 插件管理

控制台「插件」页读取 `nullbot.plugin.dir` 下的 JAR（需要 `nullbot.plugin.enabled=true`），展示插件 ID、版本、API 版本、指令和加载状态，并支持：

- **启用**：加载 JAR 并注册指令。
- **禁用**：反注册指令、调用插件的 `stop()`、关闭类加载器。
- **重载**：卸载后重新加载，用于替换 JAR 后刷新。

禁用状态保存在插件目录的 `disabled.txt`（每行一个 JAR 文件名），重启后仍然生效。只有已经放进插件目录的 JAR 才能被管理；当前不提供通过控制台上传或删除 JAR。替换已加载的 JAR 前建议先禁用插件或停止进程，避免 Windows 下文件被占用。

插件管理接口位于 `/console/plugins`，需要控制台 token；`GET` 返回 `{"enabled":boolean,"items":[...]}`，`POST /console/plugins/{jar}/enable|disable|reload` 执行操作。

## 适配器

| 构建 profile | 适配器 | 产物 |
| --- | --- | --- |
| `adapter-shiro`（默认） | Shiro（QQ / OneBot） | `launcher-shiro.jar` |
| `adapter-stdin` | 终端调试 | `launcher-stdin.jar` |

一次构建只打入一个适配器，运行时不再通过 `nullbot.adapter` 切换；不要同时激活多个 adapter profile。适配器专属配置由对应模块的 `adapter.yaml` 提供。在 IDE 中运行 launcher 时，也需要在 Maven 面板选中对应的 adapter profile。新增适配器见下方开发说明。

### Shiro 连接模式

Shiro 默认使用反向 WebSocket（`shiro.ws.server.enable=true`）。正向 WebSocket 通过 `nullbot.*` 外部配置切换：

```yaml
nullbot:
  shiro:
    server:
      enable: false
    client:
      enable: true
      url: ws://websocket_server:port
```

反向 WS 的监听路径通过 `nullbot.shiro.server.url` 配置（默认 `/ws`），正向 WS 的连接地址通过 `nullbot.shiro.client.url` 配置。`server` 和 `client` 必须互斥；两者同时启用时 Shiro 会退出。`nullbot.shiro.client.reconnect-interval` 可调整正向连接的重连间隔，默认 5 秒。

## 插件

插件目录中的 JAR 会在启动完成后通过 `ServiceLoader` 加载，也可以在控制台运行时管理。每个插件拥有独立数据目录，即插件目录下的 `<插件ID>/`（默认配置为 `workspace/plugins/<插件ID>/`），可用于配置、缓存和资源文件。

### 内置插件

| 模块 | 插件 ID | 指令 | 说明 |
| --- | --- | --- | --- |
| `hello-plugin` | `hello` | `Hello` / `你好` | 示例插件，返回插件数据目录 |
| `sb6657-plugin` | `sb6657` | `SB6657` / `6657` / `玩机器` / `玩机器烂梗` | 从接口随机获取一条玩机器烂梗 |
| `pubg-plugin` | `pubg` | `PUBG` / `吃鸡地图` | 发送地图资源图片 |
| `query-plugin` | `query` | `AA` / `模型查询`、`PRTS` / `PRTS查询`、`Trace` / `图溯源` / `溯源` | AA、PRTS 查询与 SauceNAO 图片溯源 |
| `symmetry-plugin` | `symmetry` | `Symmetry` / `图像对称` / `对称` | 将图片一半镜像到另一侧 |

### 指令示例

默认前缀为 `#`，以下省略前缀时以实际配置为准。

```text
#对称 左
#对称 右 @用户
#对称 下 123456
[引用图片] #对称 上
```

对称指令支持：

- 方式：`左` / `右` / `上` / `下`，也接受 `left` / `right` / `top` / `bottom`，默认左。
- 图片来源：引用图片、消息附带图片、`@用户`、直接填写 QQ 号。
- 头像通过适配器的用户信息接口获取，Shiro 适配器支持；不支持用户查询的适配器会提示无法获取头像。

### 插件配置

- **query**：复制 `plugins/query-plugin/config.properties.example` 到 `workspace/plugins/query/config.properties`，或设置环境变量 `AA_API_KEY`、`SAUCENAO_API_KEY`（环境变量优先）。
- **pubg**：把地图图片放入插件目录下的 `pubg/maps/`（默认 `workspace/plugins/pubg/maps/`），文件名需为 `Erangel.png`、`Miramar.png`、`Vikendi.png`、`Deston.png`、`Rondo.png`、`Tiger.png`。
- **symmetry**、**sb6657**、**hello**：无需额外配置。

## 开发

### 新增适配器

- 入站：把框架事件转换成 `BotEvent`，调用 `BotServer.handle(client, event)`。
- 出站：实现 `BotClient`。
- 在 `adapters/` 下新建模块，依赖 `spi`，并加入 `adapters/pom.xml`。
- 在 `launcher/pom.xml` 增加一个 Maven profile，把该模块作为唯一的 adapter 依赖，并设置独立的 `finalName`。
- 在模块的 `src/main/resources/adapter.yaml` 中放适配器默认配置；launcher 会通过 `optional:classpath:adapter.yaml` 加载。每个适配器都应提供该文件，`optional:` 只是避免 IDE 检查或缺失时阻塞启动。
- 适配器组件直接注册为 Spring bean；一次构建只包含一个适配器，不需要运行时条件注解。
- 第三方 starter 的自动配置会随依赖自然加载，不需要额外 exclude。
- 参考实现：`adapters/adapter-stdin`、`adapters/adapter-shiro`。

### 编写插件

- 实现 `Plugin`，在 `META-INF/services/com.zincoid.nullbot.bot.plugin.Plugin` 中注册。
- 指令实现 `Cmd`，用 `@CmdMapping` 声明名称；插件 API 版本当前为 1。
- 插件模块依赖 `core`，作用域 `provided`。
- 插件 JAR 不能包含 `com/zincoid/nullbot/` 下的宿主类。
- 在 `plugins/pom.xml` 中注册模块；`package` 后会自动复制到 `workspace/plugins/`。
- 插件启用后即可在控制台看到状态并进行启用、禁用、重载；数据目录为插件目录下的 `<插件ID>/`。
- 参考实现：`plugins/hello-plugin`、`plugins/symmetry-plugin`。

## 许可证

本项目采用 [GNU Affero General Public License v3.0](LICENSE)（AGPL-3.0）。项目依赖的 Shiro 同样采用 AGPL-3.0。
