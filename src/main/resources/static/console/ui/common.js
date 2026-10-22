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
            'nav.logout': '退出',

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
            'usage.commands': '指令排行',
            'usage.groupsRank': '群排行',
            'usage.noCommands': '暂无指令用量',
            'usage.noGroups': '暂无群用量',
            'usage.dm': '私聊',
            'usage.empty': '暂无数据',
            'usage.loadFailed': '加载用量失败：{msg}',

            'config.count': '共 {n} 项',
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
            'files.tagOptional': '标签（可选）',
            'files.tagPlaceholder': '同一批文件使用相同标签',
            'files.chooseFiles': '选择文件',
            'files.noFiles': '未选择文件',
            'files.selectedCount': '已选择 {n} 个文件',
            'files.upload': '上传',
            'files.waiting': '等待：{name}',
            'files.uploading': '上传中：{name}',
            'files.uploaded': '成功：{name}',
            'files.uploadFailed': '失败：{name}（{msg}）',
            'files.tooLarge': '失败：{name}（超过 64MB）',
            'files.selectFirst': '请选择文件',
            'files.uploadDone': '上传完成：成功 {ok} 个，失败 {fail} 个',
            'files.searchPlaceholder': '搜索关键词',
            'files.searchByName': '文件名',
            'files.searchByTag': '标签',
            'files.searchField': '搜索范围',
            'files.allTypes': '全部类型',
            'files.total': '共 {n} 个文件',
            'files.empty': '暂无文件',
            'files.loadFailed': '加载文件失败：{msg}',
            'files.name': '文件名',
            'files.tag': '标签',
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
            'files.type0': '图片收藏',
            'files.type1': '视频收藏',
            'files.type2': '音频收藏',
            'files.type3': '聊天',
            'files.type4': '表情',
            'files.type5': '二次元',

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
            'group.guard': '防注入',
            'group.prompt': '群提示词',
            'group.promptPlaceholder': '留空使用全局提示词',
            'group.save': '保存群设置',
            'group.positiveInts': '容量、补充数量与间隔必须是正整数',
            'group.saving': '保存中...',
            'group.saved': '群设置已保存',
            'group.keywordKeyword': '关键词',
            'group.keywordCmd': '指令名',
            'group.keywordArgs': '参数（空格分隔）',
            'group.keywordPlaceholders': '占位符：{userId} = 发送者 ID，{groupId} = 群号',
            'group.keywordAdd': '添加',
            'group.keywordSave': '保存修改',
            'group.keywordCancel': '取消编辑',
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
            'nav.logout': 'Sign out',

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
            'usage.commands': 'Top commands',
            'usage.groupsRank': 'Top groups',
            'usage.noCommands': 'No command usage',
            'usage.noGroups': 'No group usage',
            'usage.dm': 'Private chat',
            'usage.empty': 'No data',
            'usage.loadFailed': 'Failed to load usage: {msg}',

            'config.count': '{n} items',
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
            'files.tagOptional': 'Tag (optional)',
            'files.tagPlaceholder': 'Same tag for this batch',
            'files.chooseFiles': 'Choose files',
            'files.noFiles': 'No files selected',
            'files.selectedCount': '{n} files selected',
            'files.upload': 'Upload',
            'files.waiting': 'Waiting: {name}',
            'files.uploading': 'Uploading: {name}',
            'files.uploaded': 'Uploaded: {name}',
            'files.uploadFailed': 'Failed: {name} ({msg})',
            'files.tooLarge': 'Failed: {name} (over 64 MB)',
            'files.selectFirst': 'Choose files first',
            'files.uploadDone': 'Upload complete: {ok} succeeded, {fail} failed',
            'files.searchPlaceholder': 'Search keyword',
            'files.searchByName': 'Name',
            'files.searchByTag': 'Tag',
            'files.searchField': 'Search field',
            'files.allTypes': 'All types',
            'files.total': 'Files: {n}',
            'files.empty': 'No files',
            'files.loadFailed': 'Failed to load files: {msg}',
            'files.name': 'File name',
            'files.tag': 'Tag',
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
            'files.type0': 'Image collection',
            'files.type1': 'Video collection',
            'files.type2': 'Audio collection',
            'files.type3': 'Chat',
            'files.type4': 'Meme',
            'files.type5': 'ACG',

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
            'group.guard': 'Injection guard',
            'group.prompt': 'Group prompt',
            'group.promptPlaceholder': 'Leave blank to use the global prompt',
            'group.save': 'Save settings',
            'group.positiveInts': 'Capacity, refill, and interval must be positive integers',
            'group.saving': 'Saving...',
            'group.saved': 'Group settings saved',
            'group.keywordKeyword': 'Keyword',
            'group.keywordCmd': 'Command',
            'group.keywordArgs': 'Arguments (space-separated)',
            'group.keywordPlaceholders': 'Placeholders: {userId} = sender ID, {groupId} = group ID',
            'group.keywordAdd': 'Add',
            'group.keywordSave': 'Save changes',
            'group.keywordCancel': 'Cancel edit',
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
        updateLocaleButton();
        updateThemeButton();
    }

    function init() {
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
