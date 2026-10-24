(() => {
    'use strict';

    const THEME_KEY = 'nullbot.console.theme';
    const LOCALE_KEY = 'nullbot.console.locale';

    const MESSAGES = {
        zh: {
            'app.title': 'NullBot 控制台',

            'common.refresh': '刷新',
            'common.search': '搜索',
            'common.reset': '重置',
            'common.save': '保存',
            'common.cancel': '取消',
            'common.confirm': '确认',
            'common.menu': '菜单',
            'common.close': '关闭',
            'common.edit': '编辑',
            'common.download': '下载',
            'common.delete': '删除',
            'common.loading': '加载中...',
            'common.prev': '上一页',
            'common.next': '下一页',
            'common.actions': '操作',
            'common.id': 'ID',
            'common.time': '时间',
            'common.loadError': '加载失败：{msg}',
            'common.switchLanguage': '切换语言',
            'common.toggleTheme': '切换主题',

            'nav.usage': '概览',
            'nav.config': '配置',
            'nav.files': '文件',
            'nav.accounts': '角色',
            'nav.quotes': '语录',
            'nav.plugins': '插件',
            'nav.logout': '退出',
            'nav.about': '关于',

            'view.usage': '概览',
            'view.usage.sub': '调用趋势与排行',
            'view.config': '配置',
            'view.config.sub': '运行时参数与默认值',
            'view.files': '文件',
            'view.files.sub': '上传、检索与维护文件记录',
            'view.accounts': '角色',
            'view.accounts.sub': '权限、群设置与关键词规则',
            'view.quotes': '语录',
            'view.quotes.sub': '语录浏览与清理',
            'view.plugins': '插件',
            'view.plugins.sub': '外部插件加载与运行时管理',

            'usage.days7': '近 7 天',
            'usage.days14': '近 14 天',
            'usage.days30': '近 30 天',
            'usage.total': '总调用',
            'usage.users': '活跃用户',
            'usage.groups': '活跃群',
            'usage.period': '统计区间',
            'usage.days': '{n} 天',
            'usage.daily': '每日调用',
            'usage.note': '{first} ~ {last} · 单位：次',
            'usage.cmds': '指令排行',
            'usage.groupsRank': '群排行',
            'usage.noCmds': '暂无指令用量',
            'usage.noGroups': '暂无群用量',
            'usage.dm': '私聊',
            'usage.empty': '暂无数据',
            'usage.loadFailed': '加载用量失败：{msg}',

            'bot.current': '当前账号',
            'bot.online': '在线',
            'bot.offline': '离线',
            'bot.none': '未连接',
            'bot.id': 'ID：{id}',
            'bot.loadFailed': '加载失败',

            'config.count': '配置: {n}',
            'config.loading': '加载中...',
            'config.reset': '重置',
            'config.resetConfirm': '重置 {key} 为默认值？',
            'config.modified': '已修改',
            'config.default': '默认值：',
            'config.empty': '（空）',
            'config.saving': '保存中...',
            'config.saved': '已保存',
            'config.resetting': '重置中...',
            'config.resetDone': '已重置',

            'files.uploadTitle': '批量上传',
            'files.bizType': '业务类型',
            'files.descriptionOptional': '描述（可选）',
            'files.descriptionPlaceholder': '同一批文件使用相同描述',
            'files.chooseFiles': '选择文件',
            'files.noFiles': '未选择文件',
            'files.selectedCount': '已选择 {n} 个文件',
            'files.upload': '上传',
            'files.waiting': '等待：{name}',
            'files.uploading': '上传中：{name}',
            'files.uploaded': '成功：{name}',
            'files.uploadFailed': '失败：{name}（{msg}）',
            'files.selectFirst': '请选择文件',
            'files.uploadDone': '上传完成：成功 {ok} 个，失败 {fail} 个',
            'files.searchPlaceholder': '搜索文件名或描述',
            'files.allTypes': '全部类型',
            'files.total': '共 {n} 个文件',
            'files.empty': '暂无文件',
            'files.loadFailed': '加载文件失败：{msg}',
            'files.name': '文件名',
            'files.description': '描述',
            'files.type': '类型',
            'files.size': '大小',
            'files.uploader': '上传者',
            'files.actions': '操作',
            'files.dialogTitle': '修改文件信息',
            'files.original': '原始文件名',
            'files.nameRequired': '文件名不能为空',
            'files.saving': '保存中...',
            'files.saved': '文件信息已保存',
            'files.deleteConfirm': '删除文件「{name}」？磁盘文件也会删除。',
            'files.deleted': '文件已删除',
            'files.deleteFailed': '删除失败：{msg}',
            'files.downloadFailed': '下载失败：{msg}',
            'files.preview': '预览',
            'files.previewFailed': '预览失败：{msg}',
            'files.previewUnsupported': '该格式不支持在线预览，请下载后查看（{type}）',
            'files.select': '选择',
            'files.selectAll': '全选',
            'files.batchDelete': '批量删除',
            'files.batchDeleteCount': '删除所选（{n}）',
            'files.batchDeleteConfirm': '删除选中的 {n} 个文件？磁盘文件也会删除。',
            'files.batchDeleteDone': '批量删除完成：成功 {ok} 个，失败 {fail} 个',
            'files.type0': '图片收藏',
            'files.type1': '视频收藏',
            'files.type2': '音频收藏',
            'files.type3': '聊天',
            'files.type4': '表情',

            'accounts.usersTab': '用户',
            'accounts.groupsTab': '群组',
            'accounts.userSearch': '搜索用户 ID 或昵称',
            'accounts.groupSearch': '搜索群 ID 或群名',
            'accounts.usersTotal': '共 {n} 个用户',
            'accounts.groupsTotal': '共 {n} 个群',
            'accounts.usersEmpty': '暂无用户',
            'accounts.groupsEmpty': '暂无群',
            'accounts.loadUsersFailed': '加载用户失败：{msg}',
            'accounts.loadGroupsFailed': '加载群失败：{msg}',
            'accounts.groupName': '群名',
            'accounts.nickname': '昵称',
            'accounts.access': '权限',
            'accounts.settings': '设置',
            'accounts.userSettingTitle': '用户设置',
            'accounts.privateChat': '启用私聊',
            'accounts.aiPrompt': '私聊提示词',
            'accounts.aiPromptPlaceholder': '留空使用默认提示词',
            'accounts.userSettingSaved': '用户设置已保存',
            'accounts.saving': '保存中...',
            'accounts.userSaved': '用户已保存',
            'accounts.groupSaved': '群已保存',
            'accounts.saveFailed': '保存失败：{msg}',
            'accounts.userDeleteConfirm': '删除用户「{name}」？',
            'accounts.groupDeleteConfirm': '删除群「{name}」？',
            'accounts.userDeleted': '用户已删除',
            'accounts.groupDeleted': '群已删除',
            'accounts.deleteFailed': '删除失败：{msg}',

            'access.-2': '封禁',
            'access.-1': '受限',
            'access.0': '普通',
            'access.1': '管理',
            'access.2': '超级管理',

            'group.id': '群 ID：{id}',
            'group.settingTab': '群设置',
            'group.keywordTab': '关键字规则',
            'group.language': '语言',
            'group.languageAuto': 'auto（跟随全局）',
            'group.languageZh': 'zh（中文）',
            'group.languageEn': 'en（英文）',
            'group.joinNotice': '入群通知',
            'group.leaveNotice': '退群通知',
            'group.antiRecall': '防撤回',
            'group.effort': 'AI 强度',
            'group.effort0': '最低',
            'group.effort1': '低',
            'group.effort2': '中',
            'group.effort3': '高',
            'group.effort4': '极高',
            'group.effort5': '最大',
            'group.effort6': '极限',
            'group.scope': '限速维度',
            'group.scope0': '按用户',
            'group.scope1': '按群',
            'group.scope2': '按指令',
            'group.capacity': '限速容量',
            'group.refill': '补充数量',
            'group.interval': '补充间隔（分钟）',
            'group.limitEnabled': '启用限速',
            'group.autoChat': '自动回复',
            'group.chatFrequency': '自动回复频率（0~1）',
            'group.frequencyRange': '自动回复频率必须在 0 到 1 之间',
            'group.guard': '防注入',
            'group.prompt': '群提示词',
            'group.promptPlaceholder': '留空使用全局提示词',
            'group.save': '保存',
            'group.positiveInts': '容量、补充数量与间隔必须是正整数',
            'group.saving': '保存中...',
            'group.saved': '群设置已保存',
            'group.keywordKeyword': '关键词',
            'group.keywordCmd': '指令名',
            'group.keywordArgType': '参数类型',
            'group.keywordArgText': '文本',
            'group.keywordArgUser': '用户 ID',
            'group.keywordArgGroup': '群组 ID',
            'group.keywordArgValue': '参数值',
            'group.keywordArgAdd': '添加参数',
            'group.keywordArgRemove': '删除参数',
            'group.keywordPlaceholders': '占位符：{userId} = 发送者 ID，{groupId} = 群号；选择用户 ID / 群组 ID 会自动填入',
            'group.keywordAdd': '添加',
            'group.keywordSave': '保存修改',
            'group.keywordCancel': '取消',
            'group.keywordRequired': '关键词和指令名不能为空',
            'group.keywordAdded': '关键词规则已添加',
            'group.keywordUpdated': '关键词规则已更新',
            'group.keywordDeleted': '关键词规则已删除',
            'group.keywordDeleteConfirm': '删除关键词规则「{keyword}」？',
            'group.keywordEmpty': '暂无关键词规则',
            'group.keyword': '关键词',
            'group.cmd': '指令',
            'group.args': '参数',
            'group.enabled': '启用',
            'group.disabled': '禁用',
            'group.tableStatus': '状态',
            'group.tableActions': '操作',

            'quotes.searchPlaceholder': '搜索内容、昵称或用户 ID',
            'quotes.total': '共 {n} 条语录',
            'quotes.empty': '暂无语录',
            'quotes.loadFailed': '加载语录失败：{msg}',
            'quotes.nickname': '昵称',
            'quotes.userId': '用户 ID',
            'quotes.content': '内容',
            'quotes.deleted': '语录已删除',
            'quotes.deleteConfirm': '删除语录 #{id}？',
            'quotes.deleteFailed': '删除失败：{msg}',

            'plugins.total': '共 {n} 个插件',
            'plugins.empty': '插件目录为空',
            'plugins.jar': 'JAR 文件',
            'plugins.id': '插件 ID',
            'plugins.version': '版本',
            'plugins.commands': '指令',
            'plugins.state': '状态',
            'plugins.state.LOADED': '已加载',
            'plugins.state.DISABLED': '已禁用',
            'plugins.state.FAILED': '加载失败',
            'plugins.state.AVAILABLE': '未加载',
            'plugins.enable': '启用',
            'plugins.disable': '禁用',
            'plugins.reload': '重载',
            'plugins.retry': '重试',
            'plugins.disableConfirm': '禁用并卸载插件 {jar}？正在执行的指令可能被中断。',
            'plugins.enabled': '插件已启用：{jar}',
            'plugins.disabled': '插件已禁用：{jar}',
            'plugins.reloaded': '插件已重载：{jar}',
            'plugins.actionFailed': '插件操作失败：{msg}',
            'plugins.loadFailed': '加载插件列表失败：{msg}',
            'plugins.globalDisabled': '插件加载已在配置中关闭（nullbot.plugin.enabled=false），当前只能查看插件目录。',

            'about.title': '关于',
            'about.subtitle': '应用与构建信息',
            'about.desc': '机器人控制台',
            'about.name': '应用名称',
            'about.author': '作者',
            'about.version': '构建版本',
            'about.buildTime': '构建时间',
            'about.javaVersion': 'Java',
            'about.springBootVersion': 'Spring Boot',
            'about.uptime': '运行时间',
            'about.currentTime': '当前时间',

            'pager.info': '第 {page} / {pages} 页 · 共 {total} 条',
            'pager.perPage': '每页条数',
            'pager.perPageOption': '{n} 条/页',

            'login.title': 'NullBot 控制台 · 登录',
            'login.tab': '控制台登录',
            'login.heading': '欢迎回来',
            'login.desc': '输入控制台 token 以继续',
            'login.token': 'Token',
            'login.submit': '登录',
            'login.hint': 'token 只保存在当前浏览器的 localStorage。',
            'login.checkingSaved': '正在验证已保存的 token...',
            'login.checking': '验证中...',
            'login.invalid': 'token 无效，请重新输入',
            'login.connection': '无法连接控制台接口：{msg}',
            'login.required': '请输入 token'
        },
        en: {
            'app.title': 'NullBot Console',

            'common.refresh': 'Refresh',
            'common.search': 'Search',
            'common.reset': 'Reset',
            'common.save': 'Save',
            'common.cancel': 'Cancel',
            'common.confirm': 'Confirm',
            'common.menu': 'Menu',
            'common.close': 'Close',
            'common.edit': 'Edit',
            'common.download': 'Download',
            'common.delete': 'Delete',
            'common.loading': 'Loading...',
            'common.prev': 'Previous',
            'common.next': 'Next',
            'common.actions': 'Actions',
            'common.id': 'ID',
            'common.time': 'Time',
            'common.loadError': 'Load failed: {msg}',
            'common.switchLanguage': 'Switch language',
            'common.toggleTheme': 'Toggle theme',

            'nav.usage': 'Overview',
            'nav.config': 'Configuration',
            'nav.files': 'Files',
            'nav.accounts': 'Roles',
            'nav.quotes': 'Quotes',
            'nav.plugins': 'Plugins',
            'nav.logout': 'Sign out',
            'nav.about': 'About',

            'view.usage': 'Overview',
            'view.usage.sub': 'Usage trends and rankings',
            'view.config': 'Configuration',
            'view.config.sub': 'Runtime parameters and defaults',
            'view.files': 'Files',
            'view.files.sub': 'Upload, search, and maintain file records',
            'view.accounts': 'Roles',
            'view.accounts.sub': 'Permissions, group settings, and keyword rules',
            'view.quotes': 'Quotes',
            'view.quotes.sub': 'Browse and clean up quotes',
            'view.plugins': 'Plugins',
            'view.plugins.sub': 'External plugin loading and runtime management',

            'usage.days7': 'Last 7 days',
            'usage.days14': 'Last 14 days',
            'usage.days30': 'Last 30 days',
            'usage.total': 'Total calls',
            'usage.users': 'Active users',
            'usage.groups': 'Active groups',
            'usage.period': 'Period',
            'usage.days': '{n} days',
            'usage.daily': 'Daily calls',
            'usage.note': '{first} ~ {last} · Unit: calls',
            'usage.cmds': 'Top commands',
            'usage.groupsRank': 'Top groups',
            'usage.noCmds': 'No command usage',
            'usage.noGroups': 'No group usage',
            'usage.dm': 'Private chat',
            'usage.empty': 'No data',
            'usage.loadFailed': 'Failed to load usage: {msg}',

            'bot.current': 'Current account',
            'bot.online': 'Online',
            'bot.offline': 'Offline',
            'bot.none': 'Not connected',
            'bot.id': 'ID: {id}',
            'bot.loadFailed': 'Failed to load',

            'config.count': 'Config: {n}',
            'config.loading': 'Loading...',
            'config.reset': 'Reset',
            'config.resetConfirm': 'Reset {key} to its default value?',
            'config.modified': 'Modified',
            'config.default': 'default: ',
            'config.empty': '(empty)',
            'config.saving': 'Saving...',
            'config.saved': 'Saved',
            'config.resetting': 'Resetting...',
            'config.resetDone': 'Reset',

            'files.uploadTitle': 'Bulk upload',
            'files.bizType': 'Business type',
            'files.descriptionOptional': 'Description (optional)',
            'files.descriptionPlaceholder': 'Same description for this batch',
            'files.chooseFiles': 'Choose files',
            'files.noFiles': 'No files selected',
            'files.selectedCount': '{n} files selected',
            'files.upload': 'Upload',
            'files.waiting': 'Waiting: {name}',
            'files.uploading': 'Uploading: {name}',
            'files.uploaded': 'Uploaded: {name}',
            'files.uploadFailed': 'Failed: {name} ({msg})',
            'files.selectFirst': 'Choose files first',
            'files.uploadDone': 'Upload complete: {ok} succeeded, {fail} failed',
            'files.searchPlaceholder': 'Search file name or description',
            'files.allTypes': 'All types',
            'files.total': 'Files: {n}',
            'files.empty': 'No files',
            'files.loadFailed': 'Failed to load files: {msg}',
            'files.name': 'File name',
            'files.description': 'Description',
            'files.type': 'Type',
            'files.size': 'Size',
            'files.uploader': 'Uploader',
            'files.actions': 'Actions',
            'files.dialogTitle': 'Edit file info',
            'files.original': 'Original file name',
            'files.nameRequired': 'File name is required',
            'files.saving': 'Saving...',
            'files.saved': 'File info saved',
            'files.deleteConfirm': 'Delete file "{name}"? The stored file will also be removed.',
            'files.deleted': 'File deleted',
            'files.deleteFailed': 'Delete failed: {msg}',
            'files.downloadFailed': 'Download failed: {msg}',
            'files.preview': 'Preview',
            'files.previewFailed': 'Preview failed: {msg}',
            'files.previewUnsupported': 'This format cannot be previewed in the browser. Please download it ({type}).',
            'files.select': 'Select',
            'files.selectAll': 'Select all',
            'files.batchDelete': 'Delete selected',
            'files.batchDeleteCount': 'Delete selected ({n})',
            'files.batchDeleteConfirm': 'Delete {n} selected files? The stored files will also be removed.',
            'files.batchDeleteDone': 'Batch delete done: {ok} succeeded, {fail} failed',
            'files.type0': 'Image collection',
            'files.type1': 'Video collection',
            'files.type2': 'Audio collection',
            'files.type3': 'Chat',
            'files.type4': 'Meme',

            'accounts.usersTab': 'Users',
            'accounts.groupsTab': 'Groups',
            'accounts.userSearch': 'Search user ID or nickname',
            'accounts.groupSearch': 'Search group ID or name',
            'accounts.usersTotal': 'Users: {n}',
            'accounts.groupsTotal': 'Groups: {n}',
            'accounts.usersEmpty': 'No users',
            'accounts.groupsEmpty': 'No groups',
            'accounts.loadUsersFailed': 'Failed to load users: {msg}',
            'accounts.loadGroupsFailed': 'Failed to load groups: {msg}',
            'accounts.groupName': 'Group name',
            'accounts.nickname': 'Nickname',
            'accounts.access': 'Access',
            'accounts.settings': 'Settings',
            'accounts.userSettingTitle': 'User settings',
            'accounts.privateChat': 'Private chat',
            'accounts.aiPrompt': 'Private chat prompt',
            'accounts.aiPromptPlaceholder': 'Leave blank to use the default prompt',
            'accounts.userSettingSaved': 'User settings saved',
            'accounts.saving': 'Saving...',
            'accounts.userSaved': 'User saved',
            'accounts.groupSaved': 'Group saved',
            'accounts.saveFailed': 'Save failed: {msg}',
            'accounts.userDeleteConfirm': 'Delete user "{name}"?',
            'accounts.groupDeleteConfirm': 'Delete group "{name}"?',
            'accounts.userDeleted': 'User deleted',
            'accounts.groupDeleted': 'Group deleted',
            'accounts.deleteFailed': 'Delete failed: {msg}',

            'access.-2': 'Banned',
            'access.-1': 'Restricted',
            'access.0': 'Normal',
            'access.1': 'Manager',
            'access.2': 'Admin',

            'group.id': 'Group ID: {id}',
            'group.settingTab': 'Settings',
            'group.keywordTab': 'Keyword rules',
            'group.language': 'Language',
            'group.languageAuto': 'auto (follow global)',
            'group.languageZh': 'zh (Chinese)',
            'group.languageEn': 'en (English)',
            'group.joinNotice': 'Join notice',
            'group.leaveNotice': 'Leave notice',
            'group.antiRecall': 'Anti-recall',
            'group.effort': 'AI effort',
            'group.effort0': 'Minimal',
            'group.effort1': 'Low',
            'group.effort2': 'Medium',
            'group.effort3': 'High',
            'group.effort4': 'XHigh',
            'group.effort5': 'Max',
            'group.effort6': 'Ultra',
            'group.scope': 'Rate limit scope',
            'group.scope0': 'User',
            'group.scope1': 'Group',
            'group.scope2': 'Command',
            'group.capacity': 'Rate capacity',
            'group.refill': 'Refill amount',
            'group.interval': 'Refill interval (minutes)',
            'group.limitEnabled': 'Enable rate limit',
            'group.autoChat': 'Auto reply',
            'group.chatFrequency': 'Auto reply frequency (0-1)',
            'group.frequencyRange': 'Auto reply frequency must be between 0 and 1',
            'group.guard': 'Injection guard',
            'group.prompt': 'Group prompt',
            'group.promptPlaceholder': 'Leave blank to use the global prompt',
            'group.save': 'Save',
            'group.positiveInts': 'Capacity, refill, and interval must be positive integers',
            'group.saving': 'Saving...',
            'group.saved': 'Group settings saved',
            'group.keywordKeyword': 'Keyword',
            'group.keywordCmd': 'Command',
            'group.keywordArgType': 'Argument type',
            'group.keywordArgText': 'Text',
            'group.keywordArgUser': 'User ID',
            'group.keywordArgGroup': 'Group ID',
            'group.keywordArgValue': 'Value',
            'group.keywordArgAdd': 'Add argument',
            'group.keywordArgRemove': 'Remove argument',
            'group.keywordPlaceholders': 'Placeholders: {userId} = sender ID, {groupId} = group ID; choosing User ID / Group ID fills them automatically',
            'group.keywordAdd': 'Add',
            'group.keywordSave': 'Save changes',
            'group.keywordCancel': 'Cancel',
            'group.keywordRequired': 'Keyword and command are required',
            'group.keywordAdded': 'Keyword rule added',
            'group.keywordUpdated': 'Keyword rule updated',
            'group.keywordDeleted': 'Keyword rule deleted',
            'group.keywordDeleteConfirm': 'Delete keyword rule "{keyword}"?',
            'group.keywordEmpty': 'No keyword rules',
            'group.keyword': 'Keyword',
            'group.cmd': 'Command',
            'group.args': 'Arguments',
            'group.enabled': 'Enable',
            'group.disabled': 'Disable',
            'group.tableStatus': 'Status',
            'group.tableActions': 'Actions',

            'quotes.searchPlaceholder': 'Search content, nickname, or user ID',
            'quotes.total': 'Quotes: {n}',
            'quotes.empty': 'No quotes',
            'quotes.loadFailed': 'Failed to load quotes: {msg}',
            'quotes.nickname': 'Nickname',
            'quotes.userId': 'User ID',
            'quotes.content': 'Content',
            'quotes.deleted': 'Quote deleted',
            'quotes.deleteConfirm': 'Delete quote #{id}?',
            'quotes.deleteFailed': 'Delete failed: {msg}',

            'plugins.total': 'Plugins: {n}',
            'plugins.empty': 'The plugin directory is empty',
            'plugins.jar': 'JAR file',
            'plugins.id': 'Plugin ID',
            'plugins.version': 'Version',
            'plugins.commands': 'Commands',
            'plugins.state': 'State',
            'plugins.state.LOADED': 'Loaded',
            'plugins.state.DISABLED': 'Disabled',
            'plugins.state.FAILED': 'Load failed',
            'plugins.state.AVAILABLE': 'Not loaded',
            'plugins.enable': 'Enable',
            'plugins.disable': 'Disable',
            'plugins.reload': 'Reload',
            'plugins.retry': 'Retry',
            'plugins.disableConfirm': 'Disable and unload plugin {jar}? Commands in progress may be interrupted.',
            'plugins.enabled': 'Plugin enabled: {jar}',
            'plugins.disabled': 'Plugin disabled: {jar}',
            'plugins.reloaded': 'Plugin reloaded: {jar}',
            'plugins.actionFailed': 'Plugin action failed: {msg}',
            'plugins.loadFailed': 'Failed to load plugins: {msg}',
            'plugins.globalDisabled': 'Plugin loading is disabled (nullbot.plugin.enabled=false); the plugin directory is read-only here.',

            'about.title': 'About',
            'about.subtitle': 'Application and build information',
            'about.desc': 'Bot console',
            'about.name': 'Application',
            'about.author': 'Author',
            'about.version': 'Build version',
            'about.buildTime': 'Build time',
            'about.javaVersion': 'Java',
            'about.springBootVersion': 'Spring Boot',
            'about.uptime': 'Uptime',
            'about.currentTime': 'Current time',

            'pager.info': 'Page {page} / {pages} · {total} total',
            'pager.perPage': 'Rows per page',
            'pager.perPageOption': '{n} / page',

            'login.title': 'NullBot Console · Sign in',
            'login.tab': 'Console login',
            'login.heading': 'Welcome back',
            'login.desc': 'Enter the console token to continue',
            'login.token': 'Token',
            'login.submit': 'Sign in',
            'login.hint': 'The token is stored only in this browser’s localStorage.',
            'login.checkingSaved': 'Validating the saved token...',
            'login.checking': 'Validating...',
            'login.invalid': 'Invalid token. Try again.',
            'login.connection': 'Cannot reach the console API: {msg}',
            'login.required': 'Enter a token'
        }
    };

    function readStore(key) {
        try {
            return localStorage.getItem(key);
        } catch (e) {
            return null;
        }
    }

    function writeStore(key, value) {
        try {
            localStorage.setItem(key, value);
        } catch (e) {
            // Storage may be unavailable; the choice just won't persist.
        }
    }

    const savedLocale = readStore(LOCALE_KEY);
    let locale = savedLocale === 'en' ? 'en' : 'zh';

    const darkQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const savedTheme = readStore(THEME_KEY);
    let themeOverride = savedTheme === 'light' || savedTheme === 'dark' ? savedTheme : null;
    let theme = themeOverride || (darkQuery.matches ? 'dark' : 'light');

    document.documentElement.lang = locale === 'zh' ? 'zh-CN' : 'en';
    document.documentElement.dataset.theme = theme;

    function t(key, params) {
        const table = MESSAGES[locale] || MESSAGES.zh;
        const template = table[key] ?? MESSAGES.zh[key] ?? key;
        if (!params) return template;
        return template.replace(/\{(\w+)\}/g, (match, name) =>
            Object.prototype.hasOwnProperty.call(params, name) ? String(params[name]) : match);
    }

    function apply(root) {
        const scope = root || document;
        scope.querySelectorAll('[data-i18n]').forEach(element => {
            element.textContent = t(element.dataset.i18n);
        });
        scope.querySelectorAll('[data-i18n-placeholder]').forEach(element => {
            element.placeholder = t(element.dataset.i18nPlaceholder);
        });
        scope.querySelectorAll('[data-i18n-title]').forEach(element => {
            element.title = t(element.dataset.i18nTitle);
        });
        scope.querySelectorAll('[data-i18n-aria-label]').forEach(element => {
            element.setAttribute('aria-label', t(element.dataset.i18nAriaLabel));
        });
    }

    function updateLocaleButton() {
        const button = document.getElementById('locale-toggle');
        if (!button) return;
        button.textContent = locale === 'zh' ? 'EN' : '中文';
        button.title = t('common.switchLanguage');
        button.setAttribute('aria-label', t('common.switchLanguage'));
    }

    function updateThemeButton() {
        const button = document.getElementById('theme-toggle');
        if (!button) return;
        button.title = t('common.toggleTheme');
        button.setAttribute('aria-label', t('common.toggleTheme'));
    }

    function notify(name, detail) {
        document.dispatchEvent(new CustomEvent(name, {detail}));
    }

    const ABOUT_ENDPOINT = '/console/about';
    const ABOUT_CLOCK_INTERVAL = 1000;

    let aboutTimer = 0;
    let aboutStartedAt = 0;

    function formatDateTime(value) {
        if (!value) return '-';
        const date = value instanceof Date ? value : new Date(value);
        if (Number.isNaN(date.getTime())) return String(value);
        const pad = number => String(number).padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} `
            + `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    }

    function formatUptime(value) {
        const total = Math.max(0, Math.floor((Number(value) || 0) / 1000));
        const days = Math.floor(total / 86400);
        const hours = Math.floor(total % 86400 / 3600);
        const minutes = Math.floor(total % 3600 / 60);
        const seconds = total % 60;
        const pad = number => String(number).padStart(2, '0');
        return days > 0
            ? `${days}d ${pad(hours)}h ${pad(minutes)}m ${pad(seconds)}s`
            : `${hours}h ${pad(minutes)}m ${pad(seconds)}s`;
    }

    function renderAboutClock() {
        const element = document.getElementById('about-time');
        if (element) element.textContent = formatDateTime(new Date());
        const uptime = document.getElementById('about-uptime');
        if (uptime && aboutStartedAt)
            uptime.textContent = formatUptime(Date.now() - aboutStartedAt);
    }

    function renderAbout(data) {
        const name = data.name || 'NullBot';
        const set = (id, value) => {
            const element = document.getElementById(id);
            if (element) element.textContent = value;
        };
        const uptime = Number(data.uptime) || 0;
        aboutStartedAt = Date.now() - uptime;
        set('about-hero-name', name);
        set('about-name', name);
        set('about-version', data.version || '-');
        set('about-build-time', formatDateTime(data.buildTime));
        set('about-java', data.javaVersion || '-');
        set('about-spring-boot', data.springBootVersion || '-');
        set('about-uptime', formatUptime(uptime));
    }

    async function loadAbout() {
        try {
            const response = await fetch(ABOUT_ENDPOINT, {headers: {Accept: 'application/json'}});
            if (!response.ok) return;
            renderAbout(await response.json());
        } catch {
            // Keep the placeholders when the endpoint is unreachable.
        }
    }

    function openAbout() {
        const dialog = document.getElementById('about-dialog');
        if (!dialog) return;
        if (!dialog.open) dialog.showModal();
        renderAboutClock();
        clearInterval(aboutTimer);
        aboutTimer = setInterval(renderAboutClock, ABOUT_CLOCK_INTERVAL);
        loadAbout();
    }

    function closeAbout() {
        clearInterval(aboutTimer);
        aboutTimer = 0;
    }

    function mountAbout() {
        if (!document.getElementById('about-toggle') || document.getElementById('about-dialog'))
            return;
        const dialog = document.createElement('dialog');
        dialog.id = 'about-dialog';
        dialog.className = 'about-dialog';
        dialog.innerHTML = `
            <div class="dialog-head">
                <div>
                    <h2 data-i18n="about.title">关于</h2>
                    <p class="muted" data-i18n="about.subtitle">应用与构建信息</p>
                </div>
                <button type="button" class="dialog-close" data-close
                        data-i18n-aria-label="common.close" aria-label="关闭">×</button>
            </div>
            <div class="dialog-body">
                <div class="about-hero">
                    <span class="about-hero__mark" aria-hidden="true">&gt;_</span>
                    <div class="about-hero__text">
                        <span class="about-hero__name" id="about-hero-name">NullBot</span>
                        <span class="about-hero__desc" data-i18n="about.desc">机器人控制台</span>
                    </div>
                </div>
                <dl class="about-list">
                    <div class="about-row">
                        <dt data-i18n="about.name">应用名称</dt>
                        <dd class="about-mono" id="about-name">NullBot</dd>
                    </div>
                    <div class="about-row">
                        <dt data-i18n="about.author">作者</dt>
                        <dd>Zincoid</dd>
                    </div>
                    <div class="about-row">
                        <dt data-i18n="about.version">构建版本</dt>
                        <dd class="about-mono" id="about-version">-</dd>
                    </div>
                    <div class="about-row">
                        <dt data-i18n="about.buildTime">构建时间</dt>
                        <dd class="about-mono" id="about-build-time">-</dd>
                    </div>
                    <div class="about-row">
                        <dt data-i18n="about.javaVersion">Java</dt>
                        <dd class="about-mono" id="about-java">-</dd>
                    </div>
                    <div class="about-row">
                        <dt data-i18n="about.springBootVersion">Spring Boot</dt>
                        <dd class="about-mono" id="about-spring-boot">-</dd>
                    </div>
                    <div class="about-row">
                        <dt data-i18n="about.uptime">运行时间</dt>
                        <dd class="about-mono" id="about-uptime">-</dd>
                    </div>
                    <div class="about-row">
                        <dt data-i18n="about.currentTime">当前时间</dt>
                        <dd class="about-mono" id="about-time">-</dd>
                    </div>
                </dl>
            </div>`;
        document.body.append(dialog);
    }

    function bindAbout() {
        const toggle = document.getElementById('about-toggle');
        const dialog = document.getElementById('about-dialog');
        if (!toggle || !dialog) return;
        toggle.addEventListener('click', openAbout);
        dialog.addEventListener('close', closeAbout);
        dialog.addEventListener('click', event => {
            if (event.target === dialog) dialog.close();
        });
        dialog.querySelectorAll('[data-close]').forEach(button =>
            button.addEventListener('click', () => dialog.close()));
    }

    function setLocale(next) {
        locale = next === 'en' ? 'en' : 'zh';
        writeStore(LOCALE_KEY, locale);
        document.documentElement.lang = locale === 'zh' ? 'zh-CN' : 'en';
        apply();
        updateLocaleButton();
        updateThemeButton();
        notify('console:localechange', {locale});
    }

    function toggleLocale() {
        setLocale(locale === 'zh' ? 'en' : 'zh');
    }

    function applyTheme(next) {
        theme = next === 'dark' ? 'dark' : 'light';
        themeOverride = theme;
        writeStore(THEME_KEY, theme);
        document.documentElement.dataset.theme = theme;
        notify('console:themechange', {theme});
    }

    function followSystem() {
        if (themeOverride) return;
        const next = darkQuery.matches ? 'dark' : 'light';
        if (next === theme) return;
        theme = next;
        document.documentElement.dataset.theme = theme;
        notify('console:themechange', {theme});
    }

    if (typeof darkQuery.addEventListener === 'function') {
        darkQuery.addEventListener('change', followSystem);
    } else if (typeof darkQuery.addListener === 'function') {
        darkQuery.addListener(followSystem);
    }

    function toggleTheme(event) {
        const next = theme === 'dark' ? 'light' : 'dark';
        if (!event || typeof document.startViewTransition !== 'function'
            || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            applyTheme(next);
            return;
        }
        const x = event.clientX ?? window.innerWidth / 2;
        const y = event.clientY ?? window.innerHeight / 2;
        const radius = Math.hypot(
            Math.max(x, window.innerWidth - x),
            Math.max(y, window.innerHeight - y));
        const transition = document.startViewTransition(() => applyTheme(next));
        transition.ready.then(() => {
            document.documentElement.animate(
                {clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`]},
                {duration: 500, easing: 'ease-in-out', pseudoElement: '::view-transition-new(root)'});
        });
    }

    function bindControls() {
        const themeButton = document.getElementById('theme-toggle');
        if (themeButton) themeButton.addEventListener('click', toggleTheme);
        const localeButton = document.getElementById('locale-toggle');
        if (localeButton) localeButton.addEventListener('click', toggleLocale);
        bindAbout();
        updateLocaleButton();
        updateThemeButton();
    }

    function init() {
        mountAbout();
        apply();
        bindControls();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    window.ConsoleUI = {
        t,
        apply,
        setLocale,
        toggleLocale,
        setTheme: applyTheme,
        toggleTheme,
        get locale() {
            return locale;
        },
        get theme() {
            return theme;
        }
    };
})();
