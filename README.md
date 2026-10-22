# NullBot Renewal

多模块 Maven 工程：

- `core`：Spring Boot 主应用
- `hello-plugin`：示例外部插件

`application-dev.yaml` / `application-prod.yaml` 将运行数据指向 `workspace/files`、`workspace/logs`、`workspace/plugins`、`workspace/data`；基础默认值仍是根目录下的 `files/`、`logs/`、`plugins/` 和 `./data/sqlite.db`。

## 构建

```bash
./mvnw -DskipTests package
```

产物：

- `core/target/core-0.0.1-SNAPSHOT-exec.jar`：可执行 JAR
- `core/target/core-0.0.1-SNAPSHOT.jar`：普通 JAR，供插件编译
- `hello-plugin/target/hello-plugin-0.0.1-SNAPSHOT.jar`
- 插件 JAR 会自动复制到 `workspace/plugins/`

## 运行

从仓库根目录：

```bash
java -jar core/target/core-0.0.1-SNAPSHOT-exec.jar
```

或：

```bash
./mvnw -pl core spring-boot:run
```

`spring-boot:run` 的工作目录已配置为仓库根目录，`data/`、`plugins/` 等相对路径与单模块时一致。

## 插件

插件默认关闭：

```yaml
bot:
  plugin:
    enabled: false
    dir: ./plugins
```

启用后，`workspace/plugins/` 下的 JAR 会在启动时通过 `ServiceLoader` 加载。示例插件见 `hello-plugin`，指令为 `Hello` / `你好插件`。
