# query-plugin

模型排行、PRTS 干员资料与图片溯源，群聊与私聊均可用。插件 ID `query`。

## 指令

### AA

`#AA`（别名 `#模型查询`）：查看 Artificial Analysis 模型排行。

- `AA [查询项] [数量]`；
- 查询项：智能（默认）、历史、性价比、速度、编程、成本、定价；
- 数量 1–25，默认 10；
- 需要配置 `aa.api-key`。

### PRTS

`#PRTS`（别名 `#PRTS查询`）：查询 PRTS 干员页面。

- `PRTS [栏目] <关键词>`，栏目为语音、档案、密录、悖论，默认输出概要；
- 示例：`PRTS 能天使`、`PRTS 档案 能天使`；
- 纯文本输出，超过 1800 字符会截断。

### Trace

`#Trace`（别名 `#图溯源`、`#溯源`）：通过 SauceNAO 查询引用图片的出处。

- 引用一张图片后发送 `Trace [数量]`；
- 数量 1–5，默认 1；
- 需要配置 `saucenao.api-key`。

## 配置

首次加载自动在插件数据目录创建 `config.properties`，已有文件不会覆盖；模板见 [`config.properties.example`](config.properties.example)。

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `aa.api-key` | 空 | Artificial Analysis API Key，AA 指令必需 |
| `saucenao.api-key` | 空 | SauceNAO API Key，Trace 指令必需 |
| `aa.cache-ttl` | `600` | AA 缓存时间（秒，最小 30） |
| `prts.cache-ttl` | `600` | PRTS 缓存时间（秒，最小 30） |
| `http.user-agent` | `NullBot-Query/1.0` | 请求 User-Agent |

修改后需重载插件。
