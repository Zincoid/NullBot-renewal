# gallery-plugin

从插件数据目录随机发送图库图片。插件 ID `gallery`。

## 指令

| 指令 | 说明 | 权限 |
| --- | --- | --- |
| `#Anime`（别名 `#二次元`） | 从 `anime/` 随机发送一张图片 | normal |
| `#Zincoid`（别名 `#zn`、`#锌`） | 从 `zincoid/` 随机发送一张图片 | admin |

群聊、私聊与戳一戳均可触发，分类目录会递归扫描子目录。

## 数据

插件数据目录结构：

```text
gallery/
├── anime/
└── zincoid/
```

支持 png/jpg/jpeg/gif/bmp；以 `.` 开头的文件或目录会跳过。分类中没有图片时提示暂无图片，可在控制台“插件数据”页管理文件。无配置文件。
