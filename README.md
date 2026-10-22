# NullBot Renewal

多模块 Maven 工程：

- `spi`：框架无关的事件与端口 SPI
- `core`：核心应用，普通 JAR
- `adapters/`：机器人框架适配器
  - `adapter-shiro`：Shiro（QQ）
  - `adapter-stdin`：终端调试
- `launcher`：Spring Boot 启动与打包
- `plugins/`：外部插件，示例为 `hello-plugin`

`application-dev.yaml` / `application-prod.yaml` 将运行数据指向 `workspace/files`、`workspace/logs`、`workspace/plugins`、`workspace/data`；基础默认值仍是根目录下的 `files/`、`logs/`、`plugins/` 和 `./data/sqlite.db`。

## 构建

```bash
./mvnw -DskipTests package
```

产物：

- `launcher/target/launcher-0.0.1-SNAPSHOT.jar`：可执行 JAR
- `core/target/core-0.0.1-SNAPSHOT.jar`：核心 JAR，供插件编译
- `plugins/hello-plugin/target/hello-plugin-0.0.1-SNAPSHOT.jar`
- 插件 JAR 会自动复制到 `workspace/plugins/`

## 运行

从仓库根目录：

```bash
java -jar launcher/target/launcher-0.0.1-SNAPSHOT.jar
```

或先把模块安装到本地仓库，再运行：

```bash
./mvnw -DskipTests install
./mvnw -pl launcher spring-boot:run
```

`spring-boot:run` 的工作目录已配置为仓库根目录，`data/`、`plugins/` 等相对路径与单模块时一致。

## 适配器

`launcher` 包含 Shiro 和 stdin 两个适配器，通过 `bot.adapter` 选择，默认 `shiro`：

```bash
java -jar launcher/target/launcher-0.0.1-SNAPSHOT.jar --bot.adapter=stdin
```

## 数据库

通过 `nullbot.db.type` 显式选择数据库，两种配置统一放在 `nullbot.db` 下：

```yaml
nullbot:
  db:
    type: mysql
    mysql:
      host: localhost
      port: 3306
      database: nullbot_renewal
      username: root
      password: your-password
    sqlite:
      path: ./workspace/data/sqlite.db
```

`mysql` 模式下配置缺失或连接失败会直接启动失败，不再自动回退到 SQLite；`sqlite` 模式只使用 SQLite。未配置时默认 `sqlite`，`application-dev.yaml` / `application-prod.yaml` 使用 `mysql`。

## 插件

插件默认关闭：

```yaml
bot:
  plugin:
    enabled: false
    dir: ./plugins
```

启用后，`workspace/plugins/` 下的 JAR 会在启动时通过 `ServiceLoader` 加载。示例插件见 `plugins/hello-plugin`，指令为 `Hello` / `你好`。
