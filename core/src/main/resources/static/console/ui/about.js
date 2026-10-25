(() => {
    'use strict';

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
        const java = data.javaVersion
            ? `${data.javaVersion}${data.javaVendor ? ` (${data.javaVendor})` : ''}`
            : '-';
        aboutStartedAt = Date.now() - uptime;
        set('about-hero-name', name);
        set('about-name', name);
        set('about-author', data.author || 'Zincoid');
        set('about-version', data.version || '-');
        set('about-build-time', formatDateTime(data.buildTime));
        set('about-java', java);
        set('about-spring-boot', data.springBootVersion || '-');
        set('about-uptime', formatUptime(uptime));
        set('about-os', data.os || '-');
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
                        <dd id="about-author">Zincoid</dd>
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
                        <dt data-i18n="about.os">系统</dt>
                        <dd class="about-mono" id="about-os">-</dd>
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
        window.ConsoleUtils.bindBackdropClick(dialog, () => dialog.close());
        dialog.querySelectorAll('[data-close]').forEach(button =>
            button.addEventListener('click', () => dialog.close()));
    }

    function init() {
        mountAbout();
        bindAbout();
        const dialog = document.getElementById('about-dialog');
        if (dialog) window.ConsoleUI.apply(dialog);
    }

    if (document.readyState === 'loading')
        document.addEventListener('DOMContentLoaded', init, {once: true});
    else
        init();
})();
