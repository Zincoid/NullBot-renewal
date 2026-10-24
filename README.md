# NullBot Renewal

基于 Spring Boot 4 和 Java 27 的聊天机器人，采用适配器架构和多模块 Maven 工程。支持 Shiro（QQ / OneBot）、stdin 调试、外部插件、AI 对话、控制台，以及 MySQL / SQLite。

## 特性

- **适配器解耦**：核心只依赖 `spi`，新增框架只需新增 `adapter-*` 模块和构建 profile。
- **指令框架**：注册、别名、权限、用量统计和限速责任链。
- **外部插件**：独立 JAR 通过 `ServiceLoader` 加载，按插件隔离数据目录，控制台可运行时启用、禁用、重载。
- **AI 对话**：OpenAI 兼容接口、工具调用、消息窗口、提示词注入防护；支持 @、私聊和戳一戳触发。
- **控制台**：登录、概览、配置、文件、角色/群设置、关键字、语录、插件管理和关于。
- **双数据库**：MySQL / SQLite，SQLite 启动自动建表。
- **中英双语**：指令帮助和机器人回复支持中文 / 英文。

## 技术栈

Java 27 · Spring Boot 4.0.8 · MyBatis-Plus 3.5.17 · Shiro 2.5.5 · Maven Wrapper（Maven 3.10.0）

## 项目结构

| 路径 | 说明 |
| --- | --- |
| `spi` | 事件、消息模型和 `BotClient` / `BotServer` 端口 |
| `core` | 核心应用：`platform`、`module`、`bot`、`console` |
| `adapters/adapter-shiro` | Shiro（QQ / OneBot）适配器 |
| `adapters/adapter-stdin` | 终端调试适配器 |
| `launcher` | 启动类、配置和可执行 JAR 打包 |
| `plugins/*` | 示例与内置插件 |

依赖方向：`adapter-* -> spi`，`core -> spi`，`launcher -> core + 构建时选择的一个 adapter`。

## 快速开始

### 环境

- JDK 27
- 仓库自带 `mvnw`（Windows 使用 `mvnw.cmd`）
- MySQL 8+ 可选，默认 SQLite

### 构建

```bash
# Shiro（默认）
./mvnw -DskipTests package

# stdin
./mvnw -DskipTests -Padapter-stdin package
```

产物：`launcher/target/launcher-shiro.jar`、`launcher/target/launcher-stdin.jar`；`core/target/core-1.0.0.jar` 供插件编译；插件 JAR 构建后复制到 `workspace/plugins/`。

### 配置

```bash
cp application.yaml.template application-prod.yaml
```

`nullbot.*` 会映射到 `bot.*`，`application-dev.yaml` / `application-prod.yaml` 已被 gitignore。基础配置只保留共享项；适配器连接配置由模块内的 `adapter.yaml` 提供，launcher 通过 `spring.config.import: optional:classpath:adapter.yaml` 加载。一次构建只包含一个适配器，切换适配器需要重新构建。

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
    enabled: true
    dir: ./workspace/plugins
```

- 指令前缀默认为 `#`，可在控制台「配置」中修改 `cmd_prefix`。
- SQLite 启动时自动执行 `core/src/main/resources/schema/schema-sqlite.sql`。
- MySQL 需手动执行根目录的 `schema-mysql.sql`；连接失败会直接启动失败。

### 运行

```bash
java -jar launcher/target/launcher-shiro.jar
```

相对路径以运行目录为基准，建议从仓库根目录启动。开发运行：

```bash
./mvnw -DskipTests install
./mvnw -pl launcher spring-boot:run                 # Shiro
./mvnw -pl launcher -Padapter-stdin spring-boot:run # stdin
./mvnw test
```

## 控制台

启动日志会输出带 token 的地址，浏览器打开即可进入：

```text
Console UI: http://localhost:<port>/console/ui?token=...
```

| 视图 | 内容 |
| --- | --- |
| 概览 | 调用趋势、活跃用户/群、指令与群排行 |
| 配置 | 运行时参数、默认值、重置与 secret 掩码 |
| 文件 | 批量上传、正则拆分、类型/关键词检索、编辑、下载、删除 |
| 角色 | 用户/群权限、私聊开关、群设置、关键字规则 |
| 语录 | 检索与删除 |
| 插件 | 状态、指令列表、运行时启用/禁用/重载 |

「关于」展示应用名称、作者、构建版本/时间、Java 与 Spring Boot 版本。token 保存在浏览器 `localStorage`，右上角可退出。

插件页读取 `nullbot.plugin.dir` 下的 JAR（需 `nullbot.plugin.enabled=true`），禁用状态保存在插件目录的 `disabled.txt`。替换已加载的 JAR 前建议先禁用插件或停止进程。

## 适配器

| 构建 profile | 适配器 | 产物 |
| --- | --- | --- |
| `adapter-shiro`（默认） | Shiro（QQ / OneBot） | `launcher-shiro.jar` |
| `adapter-stdin` | 终端调试 | `launcher-stdin.jar` |

一次构建只打入一个适配器，不要同时激活多个 adapter profile；在 IDE 中运行 launcher 时也需选中对应 profile。

Shiro 默认使用反向 WebSocket，切换正向：

```yaml
nullbot:
  shiro:
    server:
      enable: false
    client:
      enable: true
      url: ws://websocket_server:port
```

`server` 与 `client` 必须互斥；`nullbot.shiro.client.reconnect-interval` 控制重连间隔（默认 5 秒）。

## 插件

插件 JAR 在启动后通过 `ServiceLoader` 加载，也可在控制台运行时管理。每个插件的数据目录为插件目录下的 `<插件ID>/`（默认 `workspace/plugins/<插件ID>/`）。

### 内置插件

| 模块 | 插件 ID | 指令 | 说明 |
| --- | --- | --- | --- |
| `hello-plugin` | `hello` | `Hello` / `你好` | 示例插件 |
| `sb6657-plugin` | `sb6657` | `SB6657` / `6657` / `玩机器` / `玩机器烂梗` | 随机玩机器烂梗 |
| `pubg-plugin` | `pubg` | `PUBG` / `吃鸡地图` | 发送 PUBG 地图资源图片 |
| `query-plugin` | `query` | `AA` / `模型查询`；`PRTS` / `PRTS查询`；`Trace` / `图溯源` / `溯源` | AA、PRTS 查询与 SauceNAO 图片溯源 |
| `symmetry-plugin` | `symmetry` | `Symmetry` / `图像对称` / `对称` | 图片半侧镜像 |

### 插件配置

- **query**：复制 `plugins/query-plugin/config.properties.example` 到 `workspace/plugins/query/config.properties`，或设置 `AA_API_KEY`、`SAUCENAO_API_KEY`（环境变量优先）。
- **pubg**：把地图图片放入 `workspace/plugins/pubg/maps/`，文件名为 `Erangel.png`、`Miramar.png`、`Vikendi.png`、`Deston.png`、`Rondo.png`、`Tiger.png`。
- **symmetry**、**sb6657**、**hello**：无需额外配置。

### 编写插件

- 实现 `Plugin`，并在 `META-INF/services/com.zincoid.nullbot.bot.plugin.Plugin` 注册。
- 指令实现 `Cmd`，使用 `@CmdMapping` 声明名称；插件 API 版本为 1。
- 插件模块依赖 `core`（`provided`），JAR 不能包含 `com/zincoid/nullbot/` 下的宿主类。
- 在 `plugins/pom.xml` 注册模块；`package` 后自动复制到 `workspace/plugins/`。

参考：`plugins/hello-plugin`、`plugins/symmetry-plugin`。

## 开发

### 新增适配器

- 入站：把框架事件转换为 `BotEvent`，调用 `BotServer.handle(client, event)`。
- 出站：实现 `BotClient`。
- 在 `adapters/` 新建模块并加入 `adapters/pom.xml`，在 `launcher/pom.xml` 增加对应 profile。
- 在模块的 `src/main/resources/adapter.yaml` 放默认配置。
- 适配器组件直接注册为 Spring bean；一次构建只包含一个适配器。

参考：`adapters/adapter-stdin`、`adapters/adapter-shiro`。

## 许可证

[GNU Affero General Public License v3.0](LICENSE)（AGPL-3.0）。项目依赖的 Shiro 同样采用 AGPL-3.0。
