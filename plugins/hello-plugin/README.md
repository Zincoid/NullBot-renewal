# hello-plugin

入门示例插件，演示指令、配置、图片渲染与 AI 工具。插件 ID `hello`。

## 指令

`#Hello`（别名 `#你好`），群聊指令。

- 默认回复 `Hello from plugin! Data: <hello.data>`；
- `--image` / `-i`：渲染示例图片卡片。

## 配置

首次加载自动在插件数据目录创建 `config.properties`，已有文件不会覆盖。

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `hello.data` | `hello-plugin-test` | 默认回复中 `Data:` 后的值 |

修改后需重载插件。

## AI 工具

注册 `hello` 工具，参数 `text`，返回问候语。
