import {$, t, bindBackdropClick, requireToken, getToken, clearToken} from './app-shared.js';
import {bindPreview} from './preview.js';
import {bind as bindUsage, loadBot, loadUsage, observeChart} from './views/usage.js';
import {bind as bindConfig, loadConfig} from './views/config.js';
import {
    bind as bindFiles,
    loadFiles,
    onLocaleChange as onFilesLocaleChange,
    populateSelects,
    updateUploadLabel
} from './views/files.js';
import {
    bind as bindAccounts,
    loadAccounts,
    onLocaleChange as onAccountsLocaleChange,
    showAccountsTab
} from './views/accounts.js';
import {
    bind as bindMessages,
    loadMessages,
    onLocaleChange as onMessagesLocaleChange
} from './views/messages.js';
import {bind as bindQuotes, loadQuotes} from './views/quotes.js';
import {bind as bindPlugins, loadPlugins} from './views/plugins.js';
import {bind as bindPluginData, onLocaleChange as onPluginDataLocaleChange} from './views/plugin-data.js';
import {bind as bindLogs, onLocaleChange as onLogsLocaleChange} from './logs.js';

function main() {
    // ═══ Sidebar collapse ═════════
    const SIDEBAR_KEY = 'nullbot.console.sidebar';
    const sidebarToggle = $('sidebar-toggle');

    function sidebarCollapsed() {
        return document.body.classList.contains('sidebar-collapsed');
    }

    function syncSidebarToggle() {
        const collapsed = sidebarCollapsed();
        const label = t(collapsed ? 'nav.expandSidebar' : 'nav.collapseSidebar');
        sidebarToggle.title = label;
        sidebarToggle.setAttribute('aria-label', label);
        sidebarToggle.setAttribute('aria-expanded', String(!collapsed));
    }

    function setSidebarCollapsed(collapsed) {
        document.body.classList.toggle('sidebar-collapsed', collapsed);
        syncSidebarToggle();
    }

    try {
        setSidebarCollapsed(localStorage.getItem(SIDEBAR_KEY) === 'collapsed');
    } catch (e) {
        setSidebarCollapsed(false);
    }

    // ═══ Navigation ═══════════════

    const loaders = {
        usage: () => {
            loadBot();
            return loadUsage();
        },
        config: loadConfig,
        files: loadFiles,
        accounts: loadAccounts,
        messages: loadMessages,
        quotes: loadQuotes,
        plugins: loadPlugins
    };

    function currentView() {
        const raw = (location.hash || '').replace(/^#\/?/, '');
        return Object.prototype.hasOwnProperty.call(loaders, raw) ? raw : 'usage';
    }

    let lastView = null;

    function renderView() {
        const view = currentView();
        Object.keys(loaders).forEach(key => {
            $('view-' + key).hidden = key !== view;
        });
        document.querySelectorAll('#nav a').forEach(link =>
            link.classList.toggle('active', link.dataset.view === view));
        document.title = t('app.title') + ' · ' + t('view.' + view);
        if (view === 'accounts' && lastView !== 'accounts') showAccountsTab('groups');
        lastView = view;
        loaders[view]();
    }

    // ═══ Binding ══════════════════

    function bindDialogs() {
        document.querySelectorAll('dialog').forEach(dialog => {
            if (dialog.id === 'confirm-dialog') return;
            dialog.querySelectorAll('[data-close]').forEach(button =>
                button.addEventListener('click', () => dialog.close()));
            bindBackdropClick(dialog, () => dialog.close());
        });
    }

    function bindEvents() {
        $('logout').onclick = () => {
            clearToken();
            location.replace('login.html');
        };

        sidebarToggle.onclick = () => {
            const collapsed = !sidebarCollapsed();
            setSidebarCollapsed(collapsed);
            try {
                localStorage.setItem(SIDEBAR_KEY, collapsed ? 'collapsed' : 'expanded');
            } catch (e) {
                // Storage unavailable: keep the state for this session only.
            }
        };
        document.addEventListener('console:localechange', syncSidebarToggle);

        const nav = document.querySelector('.nav');
        const sidebar = document.querySelector('.sidebar');
        const navToggle = $('nav-toggle');
        const sidebarBackdrop = $('sidebar-backdrop');
        const closeNav = () => {
            document.body.classList.remove('sidebar-open');
            navToggle.classList.remove('nav-toggle--open');
            navToggle.setAttribute('aria-expanded', 'false');
        };
        navToggle.onclick = event => {
            event.stopPropagation();
            const open = !document.body.classList.contains('sidebar-open');
            document.body.classList.toggle('sidebar-open', open);
            navToggle.classList.toggle('nav-toggle--open', open);
            navToggle.setAttribute('aria-expanded', String(open));
        };
        sidebarBackdrop.addEventListener('click', closeNav);
        document.addEventListener('click', event => {
            if (!document.body.classList.contains('sidebar-open')) return;
            if (!sidebar.contains(event.target) && !navToggle.contains(event.target)) closeNav();
        });
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape') closeNav();
        });
        window.addEventListener('hashchange', closeNav);
        window.matchMedia('(min-width: 1001px)').addEventListener('change', event => {
            if (event.matches) closeNav();
        });
        nav.querySelectorAll('a').forEach(link => link.addEventListener('click', closeNav));

        bindPreview();
        bindUsage();
        bindConfig();
        bindFiles();
        bindAccounts();
        bindMessages();
        bindQuotes();
        bindPlugins();
        bindPluginData();
        bindLogs();
    }

    function bindClock() {
        const date = $('topbar-date');
        const time = $('topbar-time');
        const greeting = $('topbar-greeting');
        const pad = value => String(value).padStart(2, '0');
        const greetingKey = hour => {
            if (hour >= 23 || hour < 5) return 'nav.greeting.late';
            if (hour < 9) return 'nav.greeting.early';
            if (hour < 12) return 'nav.greeting.morning';
            if (hour < 14) return 'nav.greeting.noon';
            if (hour < 18) return 'nav.greeting.afternoon';
            return 'nav.greeting.evening';
        };
        const render = () => {
            const now = new Date();
            date.textContent = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
            time.textContent = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
            time.dateTime = now.toISOString();
            greeting.textContent = t(greetingKey(now.getHours()));
        };
        const schedule = () => setTimeout(() => {
            render();
            schedule();
        }, 1000 - Date.now() % 1000);
        render();
        schedule();
        document.addEventListener('console:localechange', render);
    }

    function init() {
        window.ConsoleUI.apply();
        $('footer-year').textContent = new Date().getFullYear();
        bindClock();
        populateSelects();
        bindDialogs();
        bindEvents();
        updateUploadLabel();
        if (!location.hash) history.replaceState(null, '', '#/usage');
        renderView();
        observeChart();
    }

    window.addEventListener('hashchange', renderView);
    window.addEventListener('pageshow', () => {
        if (!getToken()) location.replace('login.html');
    });

    document.addEventListener('console:localechange', () => {
        onFilesLocaleChange();
        onAccountsLocaleChange();
        onMessagesLocaleChange();
        onPluginDataLocaleChange();
        onLogsLocaleChange();
        renderView();
    });

    init();
}

if (requireToken()) main();
