# gd-plugin

Geometry Dash 关卡搜索、详情与本地进度（`#GD`），以及 GDDL B20 评分与账号绑定（`#GDDL`）；群聊与私聊均可用。插件 ID `gd`；搜索结果、关卡详情、本地 B20 与 GDDL B20 均以图片卡片输出。

## 指令

### GD

`#GD` 的别名为 `#几何冲刺`。

| 子命令 | 说明 |
| --- | --- |
| `search <关键词> [选项]` | 搜索关卡；回复序号查看详情，`next` 翻页，`end` 取消 |
| `level <关卡ID>` | 查看关卡详情，卡片内嵌社区缩略图（WebP 自动转为 PNG） |
| `register` | 注册本地进度用户；`update`、`remove`、`b20` 需要先注册 |
| `update <关卡ID> <进度>` | 通过 JDash 验证关卡存在后，记录或更新 1–100 的进度；回复显示关卡名与作者 |
| `remove <关卡ID>` | 删除本地进度记录 |
| `b20 [--all]` | 用本地记录与 GDDL 关卡评级计算本地 B20；默认前 5 条，`--all` 显示完整 20 条 |
| `unregister` | 注销本地用户并删除全部进度；有记录时会先要求确认 |

`search` 选项：

- `--diff, -d`：`easy,normal,hard,harder,insane,demon`（逗号分隔）；
- `--demon`：`easy,medium,hard,insane,extreme`（自动按 demon 难度搜索）；
- `--length, -l`：`tiny,short,medium,long,xl,platformer`（逗号分隔）；
- `--filter`：`star,no-star,featured,original,two-player,coins,epic,custom-song`；
- `--page, -p`：页码，从 1 开始。

### GDDL

| 子命令 | 说明 |
| --- | --- |
| `GDDL bind <GDDL用户名\|ID>` | 绑定当前聊天用户的 GDDL 账号；已有绑定不会覆盖 |
| `GDDL unbind` | 解除当前聊天用户的 GDDL 绑定 |
| `GDDL b20 [GDDL用户名\|ID] [--all]` | 查询指定用户的 B20；不带用户名时查询当前绑定账号。默认前 5 条，`--all` 显示完整 20 条 |

## 数据与计算

- 本地进度保存在插件数据目录的 `gd.db`（SQLite）；注册用户、关卡名/作者快照与进度记录都在其中。
- GDDL 绑定保存在插件数据目录的 `gddl-bindings.properties`；每个聊天用户只能绑定一个账号，更换前需先解绑。
- 本地 B20 使用自报进度，关卡评级来自 GDDL，**不是 GDDL 官方记录**；未评级关卡会被跳过并提示数量。
- B20 单关 `rt = rating^E × (进度/100)^1.5`，取最高的 20 条求平均；`rating` 使用 GDDL 原始小数评分，`E` 默认 1.5，可在配置中设 `score.tier-exponent=2` 使用平方。
- B20 列表的 tier 列显示四舍五入后的整数与原始评分，如 `25(25.33)`。
- GDDL 用户评分仅统计 GDDL 已提交的记录（`beaten`/`beating`），未提交到 GDDL 的关卡不计入。
- GDDL 评级缓存默认已评级 24 小时、未评级 1 小时，可通过 `gddl.tier-cache-ttl` / `gddl.unrated-cache-ttl` 调整。

## 配置

首次加载会自动在插件数据目录生成 `config.properties`，已有文件不会覆盖；模板见 [`config.properties.example`](config.properties.example)。

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `http.proxy` | 空 | `host:port` HTTP 代理，暂不支持认证 |
| `http.connect-timeout` | `8` | 连接超时（秒） |
| `http.request-timeout` | `15` | 请求超时（秒） |
| `http.retries` | `1` | 首次请求后的重试次数 |
| `http.user-agent` | `NullBot-GD/1.0` | User-Agent |
| `cache-ttl` | `600` | GDDL、B20 与缩略图缓存（秒） |
| `gddl.tier-cache-ttl` | `86400` | 已评级关卡缓存（秒） |
| `gddl.unrated-cache-ttl` | `3600` | 未评级或未收录关卡缓存（秒） |
| `score.tier-exponent` | `1.5` | B20 公式中的 rating 指数 |
| `gddl.api-base` | `https://gdladder.com/api` | GDDL API 地址 |
| `gddl.avatar-base` | `https://cdn.gdladder.com/avatars/` | GDDL 头像 CDN |
| `thumbnail.base` | `https://levelthumbs.prevter.me/thumbnail/` | 缩略图服务 |

服务器网络较慢时可使用代理或镜像地址，并适当增大超时；缩略图、GDDL 头像和本地 B20 头像成功获取后会缓存。
