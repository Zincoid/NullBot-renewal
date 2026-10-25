import {$, t, bindBackdropClick, requireToken, getToken, clearToken} from './app-shared.js';
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
import {bind as bindQuotes, loadQuotes} from './views/quotes.js';
import {bind as bindPlugins, loadPlugins} from './views/plugins.js';
import {bind as bindPluginData, onLocaleChange as onPluginDataLocaleChange} from './views/plugin-data.js';

function main() {
    // ═══ Navigation ══════════════════

    const loaders = {
        usage: () => {
            loadBot();
            return loadUsage();
        },
        config: loadConfig,
        files: loadFiles,
        accounts: loadAccounts,
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

    // ═══ Binding ═════════════════════

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

        const nav = document.querySelector('.nav');
        const navToggle = $('nav-toggle');
        const closeNav = () => {
            nav.classList.remove('nav-open');
            navToggle.classList.remove('nav-toggle--open');
            navToggle.setAttribute('aria-expanded', 'false');
        };
        navToggle.onclick = event => {
            event.stopPropagation();
            const open = !nav.classList.contains('nav-open');
            nav.classList.toggle('nav-open', open);
            navToggle.classList.toggle('nav-toggle--open', open);
            navToggle.setAttribute('aria-expanded', String(open));
        };
        document.addEventListener('click', event => {
            if (!nav.contains(event.target) && !navToggle.contains(event.target)) closeNav();
        });
        document.addEventListener('keydown', event => {
            if (event.key === 'Escape') closeNav();
        });
        window.addEventListener('hashchange', closeNav);
        window.matchMedia('(min-width: 1001px)').addEventListener('change', event => {
            if (event.matches) closeNav();
        });
        nav.querySelectorAll('a').forEach(link => link.addEventListener('click', closeNav));

        bindUsage();
        bindConfig();
        bindFiles();
        bindAccounts();
        bindQuotes();
        bindPlugins();
        bindPluginData();
    }

    function init() {
        window.ConsoleUI.apply();
        $('footer-year').textContent = new Date().getFullYear();
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
        onPluginDataLocaleChange();
        renderView();
    });

    init();
}

if (requireToken()) main();
