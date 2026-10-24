# 更新日志

## [Unreleased] - 2026-10-24

这是 NullBot Renewal 的首个开发版本，版本号待定。项目基于 Spring Boot 4 和 Java 27，采用多模块适配器架构：核心只依赖 `spi`，通过 Shiro（QQ / OneBot）或 stdin 适配器接入机器人框架。该版本支持外部插件、AI 对话、控制台管理、中英文回复，以及 MySQL / SQLite 两种数据库，许可证为 AGPL-3.0。

### 新增

#### 控制台

- 登录页：token 校验、`localStorage` 记忆、自动登录与退出登录。
- 视图：概览、配置、文件、角色/群设置、关键词规则、语录、插件。
- 文件：批量上传、类型/关键词检索、下载、编辑、删除，以及上传大小限制和正则文件名拆分。
- 角色：用户/群权限、私聊开关、群设置、关键词增删改与启停。
- 用量图表：7 / 14 / 30 天趋势和指令 / 群排行，使用内联 SVG 绘制。
- 关于弹窗：应用名称、作者、构建版本、构建时间、Java 版本、Spring Boot 版本与当前时间，支持中英文切换。
- `GET /console/about` 公开返回构建信息，供未登录的登录页读取。

#### 插件

- 控制台插件管理：查看插件 ID、版本、API 版本、指令、JAR 文件和加载状态；支持运行时启用、禁用、重载。
- 禁用状态持久化到插件目录的 `disabled.txt`，重启后保持；`nullbot.plugin.enabled=false` 时插件页只读。
- `symmetry-plugin`：`Symmetry` / `图像对称` / `对称`，将图片一半镜像到另一侧，支持左/右/上/下。
- `sb6657-plugin`：`SB6657` / `6657` / `玩机器` / `玩机器烂梗`，随机发送玩机器烂梗。
- `pubg-plugin`：`PUBG` / `吃鸡地图`，发送 PUBG 地图资源图片。
- `query-plugin`：`AA` / `模型查询`、`PRTS` / `PRTS查询`、`Trace` / `图溯源` / `溯源`。
- `hello-plugin`：`Hello` / `你好`，外部插件开发示例。

#### 构建

- `launcher` 生成 `META-INF/build-info.properties`，关于页显示真实的构建版本和构建时间。

### 改进

- 插件加载器保存插件实际注册的 `Cmd` 实例，`CmdRegistry` 支持按实例反注册，使插件可以安全卸载和重载。
- 插件状态文件使用临时文件加原子替换写入，避免并发或中断时留下半个文件。
- 适配器改为构建时选择：`launcher` 通过 Maven profile 只打入一个适配器（默认 Shiro，`-Padapter-stdin` 构建 stdin），产物为 `launcher-shiro.jar` / `launcher-stdin.jar`；移除 `bot.adapter`、`AdapterProperties` 和适配器条件注解，适配器配置由模块内 `adapter.yaml` 提供。
- Shiro 适配器配置支持反向 / 正向 WebSocket 切换：`adapter.yaml` 提供 `server` 与 `client` 占位符，二者互斥，可通过 `nullbot.shiro.*` 外部配置切换。
- README 全面更新：控制台、插件管理、内置插件、适配器配置、配置项与插件开发说明。

### 修复

- 修正 Maven 3.10 可重复构建默认时间导致的 `1980-02-01` 构建时间；`build-info` 改用 `${maven.build.timestamp}`。
