# 插件开发指南

本文说明如何为 NullBot 编写外部插件。完整可运行示例见 [`plugins/hello-plugin`](plugins/hello-plugin)；包含服务、缓存、配置与图片渲染的示例见 [`plugins/gd-plugin`](plugins/gd-plugin) 和 [`plugins/query-plugin`](plugins/query-plugin)。

## 快速开始

一个插件是一个独立 JAR，包含：

- 一个 `Plugin` 实现，并通过 `ServiceLoader` 注册；
- 任意数量的 `Cmd` 指令与可选 AI `Tool`；
- 可选的 JTE 模板、字体和随 JAR 分发的资源。

最小模块结构：

```text
plugins/demo-plugin/
├── pom.xml
└── src/main/
    ├── java/com/zincoid/demo/
    │   ├── DemoPlugin.java
    │   ├── DemoText.java
    │   ├── DemoException.java      # 可选
    │   ├── cmd/DemoCmd.java
    │   ├── config/DemoConfig.java
    │   └── service/DemoService.java
    └── resources/
        └── META-INF/services/com.zincoid.nullbot.bot.plugin.Plugin
```

在 `plugins/pom.xml` 的 `<modules>` 中加入 `demo-plugin`，然后声明模块：

```xml
<parent>
    <groupId>com.zincoid</groupId>
    <artifactId>plugins</artifactId>
    <version>${revision}</version>
    <relativePath>../pom.xml</relativePath>
</parent>
<artifactId>demo-plugin</artifactId>
```

`revision` 由根 `pom.xml` 的 `<revision>` 属性统一控制，发布时只需修改这一处。

父模块已把 `core` 和 Lombok 设为 `provided`，并提供了复制 JAR 到 `workspace/plugins/` 的 `maven-antrun-plugin` 配置。简单插件只需在 `<build><plugins>` 中声明 `maven-antrun-plugin`；使用 JTE 模板时再声明 `jte-maven-plugin`（参考 `hello-plugin/pom.xml`）。

注册文件 `META-INF/services/com.zincoid.nullbot.bot.plugin.Plugin` 只写一行实现类全名：

```text
com.zincoid.demo.DemoPlugin
```

一个 JAR 只能有一个 `Plugin` 实现；缺失或多个实现都会加载失败。

## 插件入口

插件入口负责创建服务、组装指令并管理生命周期：

```java
package com.zincoid.demo;

import com.zincoid.demo.cmd.DemoCmd;
import com.zincoid.demo.service.DemoService;
import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.nullbot.bot.plugin.PluginContext;

import java.util.List;

public class DemoPlugin implements Plugin {

    private volatile DemoService service;

    @Override
    public String id() {
        return "demo";
    }

    @Override
    public String version() {
        return "1.1.0";
    }

    @Override
    public int apiVersion() {
        return 1; // 仅指令用 1；提供 AI 工具用 2；使用图片渲染用 3
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new DemoCmd(service()));
    }

    @Override
    public void start(PluginContext context) {
        this.service = new DemoService(DemoConfig.load(context.dataDir()));
    }

    @Override
    public void stop() {
        this.service = null;
    }

    DemoService service() {
        DemoService value = service;
        if (value == null) throw new IllegalStateException("Plugin not started");
        return value;
    }
}
```

`DemoConfig` 为示例配置类，读取方式见“数据、配置与资源”。

要点：

- `id()` 必须匹配 `[A-Za-z0-9_-]{1,64}`，且全局唯一；
- `start()` 在 `cmds()` / `tools()` 之前调用；
- `stop()` 在停用、重载或卸载时调用，应释放字段与资源；
- `cmds()` / `tools()` 可能被多次调用（例如帮助页列举指令），应轻量返回新实例，不要在 `cmds()` 里做重初始化；
- 指令通过构造器接收 `PluginContext`、服务等依赖，不直接依赖整个插件实例；
- 插件访问器保持包内可见即可。

## API 版本

| `apiVersion()` | 能力 |
| --- | --- |
| `1`（默认） | 指令 |
| `2` | 指令与 AI 工具 |
| `3` | 指令、AI 工具、图片渲染与插件模板 |

宿主当前支持 `1`–`3`，超出范围会拒绝加载。只使用指令时无需显式覆写 `apiVersion()`。

## 版本管理

- Maven 模块版本由根 `pom.xml` 的 `<revision>` 属性统一控制，发布时只改这一处。
- `Plugin.version()` 默认读取插件 JAR manifest 的 `Implementation-Version`；Maven 构建会自动写入，内部插件无需手改。
- 从 IDE 或 classpath 运行时可能取不到版本，此时默认返回 `dev`；需要固定版本时可以覆写 `version()`。
- `apiVersion()` 是宿主兼容版本，当前为 `1`–`3`，不随宿主发布号变化。
- 外部插件可以使用独立版本；升级宿主时只需确认 `apiVersion()` 仍在支持范围内。

## 宿主服务

`PluginContext` 只暴露宿主允许插件使用的服务：

| 方法 | 用途 |
| --- | --- |
| `dataDir()` | 插件独立数据目录 |
| `renderer()` | SVG/JTE 图片渲染与字体注册 |
| `config()` | 宿主配置存储 |
| `inputs()` | 多轮输入等待 |
| `model()` | AI 模型调用 |
| `users()` / `groups()` | 用户、群记录 |
| `groupSettings()` / `userSettings()` | 群、用户设置 |
| `quotes()` | 语录服务 |
| `files()` | 文件记录与磁盘存储 |
| `languages()` | 回复语言服务 |

插件配置和缓存文件放在 `dataDir()` 下，不要依赖宿主的内部工作目录。

## 插件结构

根包只放插件入口、文案与可选的插件异常；其余按需分层：

```text
com.zincoid.<plugin>
├── XxxPlugin.java
├── XxxText.java
├── XxxException.java      # 可选
├── cmd/                   # 指令
├── config/                # 可选
├── service/               # 可选
├── model/                 # 可选
├── render/                # 可选
├── tool/                  # 可选，AI 工具
└── binding/               # 可选，插件自有持久化
```

单类插件不需要为了结构完整而创建空包。指令统一放在 `cmd`；配置、服务、模型、渲染、工具、绑定按实际需要拆分。

## 指令

指令实现 `com.zincoid.nullbot.bot.cmd.Cmd`，用 `@CmdMapping` 声明名称和别名。第一个名称是指令的规范名称。

```java
@Slf4j
@CmdMapping({"Demo", "示例"})
@RequiredArgsConstructor
public class DemoCmd implements Cmd {

    private final DemoService service;

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    private void handle(CmdContext ctx, CmdArgs args) {
        String keyword = args.rest("").trim();
        if (keyword.isEmpty())
            throw new CmdWarnException(DemoText.MISSING_KEYWORD.of(ctx.language()));
        String result;
        try {
            result = service.query(keyword);
        } catch (DemoException e) {
            log.warn("Demo query failed: {}", keyword, e);
            throw new CmdWarnException(DemoText.QUERY_FAILED.of(ctx.language()));
        }
        ctx.send(result);
    }

    @Override
    public String getHelp(Language language) {
        return DemoText.HELP.of(language);
    }
}
```

`Cmd` 为群聊消息、私聊消息、群戳一戳、私聊戳一戳分别提供 `run` 重载；未实现的类型默认返回“不支持”的本地化提示。按需覆写即可。

常用回调：

| 方法 | 说明 |
| --- | --- |
| `getHelp(Language)` | `指令 -H` 与帮助列表使用的文案 |
| `getAiHelp()` | 供 AI 阅读的英文说明；`isAiCallable()` 为 true 时建议覆写 |
| `isAiCallable()` | 是否允许 AI 通过 `call` 工具调用，默认 `false` |
| `getAccess()` | 所需限权，默认 `Access.NORMAL` |

只在指令确实没有副作用、参数可由 AI 安全生成时才开启 `isAiCallable()`。

## 参数与消息

`CmdArgs` 会自动解析选项，并将其从位置参数中移除：

- `--all`、`-a`：布尔选项；
- `--limit=10`、`-l=10`：带值选项；
- 其余参数按原顺序作为位置参数。

```java
boolean all = args.hasOpt("all", "a");
int limit = args.optInt("limit", "l", 10);
String name = args.next("");       // 没有时返回默认值
String rest = args.rest("");       // 其余位置参数，空格连接
```

`CmdContext` 提供：

- `ctx.send(String)` / `ctx.send(MsgContent)`：发送到指令来源会话；
- `ctx.language()`：当前回复语言；
- `ctx.client()`：`BotClient` 出站端口；
- `ctx.render(Text, ...)`：渲染 core 文案。

消息内容用 `MsgContent.builder()` 组合：

```java
ctx.send(MsgContent.builder()
        .at(event.userId())
        .text("结果")
        .imageBase64(png)
        .build());
```

`GroupMsgEvent` / `PrivateMsgEvent` 的 `content()` 提供 `text()`、`atIds()`、`replyId()` 与 `segments()`；处理引用图片、@ 用户时从事件内容读取。

## 文案与异常

用户可见文案放在插件自己的 `XxxText` 枚举中，中文和英文成对维护：

```java
public enum DemoText {
    HELP("""
            Demo
            用法：Demo <关键词>
            别名：示例""", """
            Demo
            Usage: Demo <keyword>
            Aliases: 示例"""),
    MISSING_KEYWORD("缺少关键词", "Missing keyword"),
    QUERY_FAILED("查询失败，请稍后重试", "Query failed; try again later");

    private final String zh;
    private final String en;

    DemoText(String zh, String en) {
        this.zh = zh;
        this.en = en;
    }

    public String of(Language language) {
        return language == Language.EN ? en : zh;
    }

    public String of(Language language, Object... args) {
        return String.format(Locale.ROOT, of(language), args);
    }
}
```

异常分层：

- `CmdWarnException`：用户可修正的问题，例如参数错误、缺少输入；
- `CmdInfoException`：提示性结果，例如没有数据、已取消；
- 插件自定义 `XxxException`：网络、解析、图片处理等领域边界；
- 指令层捕获插件异常，记录日志并转换为本地化 `CmdWarnException`。

不要用宽泛的 `catch (Exception)` 吞掉错误，也不要把插件内部技术异常直接暴露给用户。

## AI 工具

提供 AI 工具时 `apiVersion()` 至少为 `2`，并在 `tools()` 中返回实现：

```java
public class DemoTool implements Tool {

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("demo_greet", "Returns a greeting.")
                .addString("name", "name to greet", true)
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args parsed = args == null || args.isBlank()
                ? new Args(null)
                : JsonUtils.parse(args, Args.class);
        String name = parsed.name() == null || parsed.name().isBlank()
                ? "world" : parsed.name();
        return ToolRes.of("Hello, " + name + "!");
    }

    private record Args(String name) {
    }
}
```

工具名必须全局唯一，建议使用 `<插件 ID>_<工具名>`。`ToolContext` 提供向当前会话发送消息、禁言、撤回和执行指令的能力；失败时返回 `ToolRes.error(...)`。`JsonUtils.parse` 解析失败会抛出 `PlatformException`，应在工具内捕获并转换为错误结果。工具会随插件停用或重载同步注销。

## 图片渲染

使用渲染能力时 `apiVersion()` 返回 `3`。`PluginContext.renderer()` 提供：

- `svg(String)`：原始 SVG → PNG；
- `template(String, Map)`：预编译 JTE 模板 → PNG；
- `font(byte[])`：注册 TTF/OTF 字体。

模板放在 `src/main/resources/templates/*.jte`，使用 `ContentType.Plain`：

```text
@param String title

<svg width="720" height="270" xmlns="http://www.w3.org/2000/svg">
    <text x="32" y="64" font-family="JetBrains Maple Mono" font-size="24">${title}</text>
</svg>
```

在 `pom.xml` 中声明 `jte-maven-plugin`（参考 `hello-plugin/pom.xml`），让模板在构建时预编译进 JAR。需要渲染的指令注入 `PluginContext` 后即可使用：

```java
byte[] png = context.renderer().template("demo.jte", Map.of("title", "Demo"));
ctx.send(MsgContent.builder().imageBase64(png).build());
```

渲染失败会抛出 `RenderException`（运行时异常）；指令层应捕获并转换为本地化的 `CmdWarnException`，不要让技术异常直接暴露给用户。

core 提供以下排版工具，避免每个插件重复测量、截断和列对齐：

- `SvgText` / `SvgLine` / `SvgPositionedLine`：文本宽度、截断、居中、转义；
- `SvgTable` / `SvgColumn` / `SvgCell`：表头、列宽、对齐与单元格定位；
- `ImageData`：data URI、WebP/PNG 解码、居中裁剪缩放与 PNG 编码；
- `SvgText.fontSegments`：中英与 emoji 混排分段。

插件 JAR 内 `fonts/` 下的 TTF/OTF 会在加载时自动注册，也可调用 `renderer().font(bytes)` 动态注册。字体注册是 JVM 全局的，插件卸载后不会撤销。

## 数据、配置与资源

`PluginContext.dataDir()` 是宿主为插件创建的独立数据目录，用于：

- `config.properties`：插件配置；
- 缓存、绑定、用户数据等持久化文件；
- 解压后的随包资源。

配置读取通常遵循“环境变量优先、其次 `config.properties`”的约定，示例见 `gd-plugin/config` 与 `query-plugin/config`。简单配置可以直接用 `Properties` 读取，不需要引入额外框架。

插件可把默认配置放在 `src/main/resources/config.properties`，在 `start()` 中调用 `PluginResources.extract(...)`：

```java
PluginResources.extract(DemoPlugin.class, "/config.properties", context.dataDir());
```

目标文件已存在时不会覆盖；默认配置应使用空密钥/安全占位值。新增配置项时插件仍需在代码中保留默认值，避免旧配置文件缺少键。

随 JAR 分发的图片/二进制资源可放在 `src/main/resources/`，启动时用 `PluginResources.extract(...)` 解压到数据目录；`pubg-plugin` 是示例。只读资源也可以直接从 classpath 读取。

插件不应写入 `dataDir()` 之外的路径，也不应依赖宿主的内部工作目录。

## 打包与运行

构建单个插件：

```bash
./mvnw -q -pl plugins/demo-plugin -am -DskipTests package
```

执行完整 `./mvnw package` 时，声明了 `maven-antrun-plugin` 的插件会把 JAR 复制到 `workspace/plugins/`。启用插件：

```yaml
nullbot:
  plugin:
    enabled: true
    dir: ./workspace/plugins
```

插件也可以在控制台“插件”页上传、启停、重载和删除。目录中的 JAR 会被复制后通过独立类加载器加载，因此升级时可直接替换 JAR 或在控制台重载。

如果插件依赖第三方库：

- `core` 必须保持 `provided`，不要把宿主 API 打进插件 JAR；加载器会拒绝包含 `com/zincoid/nullbot/` 的 JAR；
- 使用 `maven-shade-plugin` 打包第三方依赖；
- 配置 `ServicesResourceTransformer` 合并 `META-INF/services`；
- 排除签名文件 `META-INF/*.SF`、`*.DSA`、`*.RSA` 与 `module-info.class`。

完整配置可参考 `plugins/gd-plugin/pom.xml`。

## 调试

使用 stdin 适配器可以在不连接 QQ 的情况下验证插件：

```bash
./mvnw -P adapter-stdin package
java -jar launcher/target/launcher-stdin.jar \
  --nullbot.plugin.enabled=true \
  --nullbot.plugin.dir="$PWD/workspace/plugins"
```

输入指令并查看日志，例如 `msg g #Demo -H`。也可以在启动后使用 `#Help -p=demo` 检查插件是否加载、指令是否注册。

常见加载失败原因：

- `META-INF/services/...Plugin` 缺失或写错；
- 一个 JAR 中存在多个 `Plugin` 实现；
- 插件 ID 不合法或与其他插件重复；
- `apiVersion()` 超出 `1`–`3`；
- 插件 JAR 包含了宿主 `com.zincoid.nullbot.*` 类；
- 第三方依赖没有 shade，运行时出现 `NoClassDefFoundError`。

## 代码风格

- Javadoc 使用中文摘要 + 英文 `<p>` 摘要，`@param` 使用英文；
- 较长类用 `══════` 分节，分节顺序为 `Run` → 功能 → `Helpers` → `Cmd`；
- 日志使用平实文案，不加装饰前缀或 `[ClassName]`；
- 用户可见文案放 `XxxText`，不要硬编码中英文；
- 指令、服务、配置通过构造器注入，不使用全局单例；
- 只有需要区分领域错误时才添加插件异常，不必每个插件都有。

## 检查清单

- [ ] `id()`、`version()`、`apiVersion()` 正确
- [ ] 插件 JAR manifest 含 `Implementation-Version`（默认 `version()` 读取）
- [ ] `META-INF/services` 只注册一个 `Plugin`
- [ ] 插件 JAR 未打包 `com/zincoid/nullbot/**`
- [ ] `start()` 创建依赖，`stop()` 清理字段
- [ ] `cmds()` / `tools()` 可重复调用且无重副作用
- [ ] 每个指令有 `getHelp(Language)`，AI 可调用指令有 `getAiHelp()`
- [ ] 用户可见文案走 `XxxText`
- [ ] 网络/解析异常转换为本地化 `CmdWarnException`
- [ ] 使用图片渲染时 `apiVersion() == 3`
- [ ] 本地 stdin 适配器验证通过
