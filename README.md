# NullBot Renewal

基于 Spring Boot 4 和 Java 27 的 QQ 机器人，采用适配器架构和多模块 Maven 工程。支持 Shiro（OneBot）与 stdin 调试适配器、外部插件、AI 对话、控制台管理，以及 MySQL / SQLite。

## 特性

- **适配器解耦**：核心只依赖 `spi`，新增机器人框架只需新增 `adapter-*` 模块。
- **指令框架**：指令注册、别名、权限、用量统计和限速责任链。
- **外部插件**：独立 JAR 通过 `ServiceLoader` 加载，按插件隔离数据目录。
- **AI 能力**：OpenAI 兼容接口、工具调用、消息窗口记忆、提示词注入防护。
- **控制台**：配置、文件、用户/群、群设置、关键字、语录和用量图表。
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
| `launcher` | 启动类、配置和可执行 JAR 打包 |
| `plugins/hello-plugin` | 示例外部插件 |

依赖方向：`adapter-* -> spi`，`core -> spi`，`launcher -> core + adapters`。

## 快速开始

### 环境

- JDK 27
- 无需安装 Maven，仓库自带 `mvnw`（Windows 使用 `mvnw.cmd`）
- MySQL 8+ 可选，默认 SQLite

### 构建

```bash
./mvnw -DskipTests package
```

主要产物：

- `launcher/target/launcher-0.0.1-SNAPSHOT.jar`：可执行 JAR
- `core/target/core-0.0.1-SNAPSHOT.jar`：核心 JAR，供插件编译
- `plugins/hello-plugin/target/hello-plugin-0.0.1-SNAPSHOT.jar`：示例插件，构建后自动复制到 `workspace/plugins/`

### 配置

```bash
cp application.yaml.template application-prod.yaml
```

外部配置统一使用 `nullbot.*`，基础配置会映射到 `bot.*`。`application-dev.yaml` / `application-prod.yaml` 已被 gitignore。

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
  adapter: shiro
  plugin:
    enabled: false
    dir: ./plugins
```

dev/prod 配置把数据、日志、文件和插件放在 `workspace/` 下；不配置时使用根目录的 `data/`、`logs/`、`files/`、`plugins/`。

- SQLite 启动时自动执行 `core/src/main/resources/schema/schema-sqlite.sql`。
- MySQL 需要手动执行根目录的 `schema-mysql.sql`，配置缺失或连接失败会直接启动失败。

### 运行

从仓库根目录：

```bash
java -jar launcher/target/launcher-0.0.1-SNAPSHOT.jar
```

选择 stdin 适配器：

```bash
java -jar launcher/target/launcher-0.0.1-SNAPSHOT.jar --nullbot.adapter=stdin
```

配置里的相对路径以运行目录为基准，建议从仓库根目录启动。也可以在运行目录放置 `application.yaml`，或用 `--spring.config.additional-location` 指定配置位置。

开发运行与测试：

```bash
./mvnw -DskipTests install
./mvnw -pl launcher spring-boot:run

./mvnw test
```

## 控制台

启动日志会输出带 token 的地址：

```text
Console UI: http://localhost:<port>/console/ui?token=...
```

浏览器打开该地址即可进入控制台；未登录时会跳转到登录页。实际端口以启动日志为准。

## 适配器

| 值 | 适配器 |
| --- | --- |
| `shiro` | Shiro（QQ / OneBot） |
| `stdin` | 终端调试 |

默认 `shiro`。新增适配器见下方开发说明。

## 插件

把 `nullbot.plugin.enabled` 改为 `true` 即可启用。插件目录下的 JAR 会在启动时通过 `ServiceLoader` 加载。目录默认 `./plugins`，dev/prod 为 `./workspace/plugins`。示例见 `plugins/hello-plugin`，指令为 `Hello` / `你好`。

插件不能把 `com/zincoid/nullbot/` 下的宿主类打进 JAR。开发步骤见下方开发说明。

## 开发

### 新增适配器

- 入站：把框架事件转换成 `BotEvent`，调用 `BotServer.handle(client, event)`。
- 出站：实现 `BotClient`。
- 在 `adapters/` 下新建模块，依赖 `spi`，并加入 `adapters/pom.xml` 和 `launcher/pom.xml`。
- 在 `AdapterProperties.Adapter` 中增加枚举值，配置 `nullbot.adapter`。
- 参考实现：`adapters/adapter-stdin`、`adapters/adapter-shiro`。

### 编写插件

- 实现 `Plugin`，在 `META-INF/services/com.zincoid.nullbot.bot.plugin.Plugin` 中注册。
- 指令实现 `Cmd`，用 `@CmdMapping` 声明名称。
- 插件模块依赖 `core`，作用域 `provided`。
- 插件 JAR 不能包含 `com/zincoid/nullbot/` 下的宿主类。
- 参考实现：`plugins/hello-plugin`。

## 许可证

本项目采用 [GNU Affero General Public License v3.0](LICENSE)（AGPL-3.0）。项目依赖的 Shiro 同样采用 AGPL-3.0。
