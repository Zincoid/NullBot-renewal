(() => {
    'use strict';

    const TOKEN_KEY = 'nullbot.console.token';
    const LOGIN_PAGE = 'login.html';

    // ═══ Helpers ═════════════════════

    const $ = id => document.getElementById(id);

    function t(key, params) {
        return window.ConsoleUI.t(key, params);
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function formatBytes(value) {
        let size = Number(value) || 0;
        if (size < 1024) return size + ' B';
        const units = ['KB', 'MB', 'GB', 'TB'];
        let index = 0;
        size /= 1024;
        while (size >= 1024 && index < units.length - 1) {
            size /= 1024;
            index++;
        }
        return size.toFixed(size >= 10 ? 0 : 1) + ' ' + units[index];
    }

    function formatTime(value) {
        if (!value) return '-';
        return String(value).replace('T', ' ').slice(0, 16);
    }

    function setStatus(element, text, kind = '') {
        element.textContent = text;
        element.className = 'status' + (kind ? ' ' + kind : '');
    }

    function emptyRow(colspan, text) {
        return `<tr><td colspan="${colspan}" class="empty">${escapeHtml(text)}</td></tr>`;
    }

    function renderPager(element, data, change, resize) {
        const pages = Math.max(1, Math.ceil(data.total / data.size));
        element.innerHTML = '';
        const info = document.createElement('span');
        info.textContent = t('pager.info', {page: data.page, pages, total: data.total});

        const sizeLabel = document.createElement('label');
        sizeLabel.className = 'pager-size';
        const sizeSelect = document.createElement('select');
        sizeSelect.setAttribute('aria-label', t('pager.perPage'));
        [10, 20, 50, 100].forEach(size => {
            const option = document.createElement('option');
            option.value = String(size);
            option.textContent = t('pager.perPageOption', {n: size});
            option.selected = Number(data.size) === size;
            sizeSelect.append(option);
        });
        sizeSelect.onchange = () => {
            if (typeof resize === 'function') resize(Number(sizeSelect.value));
        };
        sizeLabel.append(sizeSelect);

        const prev = document.createElement('button');
        prev.type = 'button';
        prev.textContent = t('common.prev');
        prev.disabled = data.page <= 1;
        prev.onclick = () => change(data.page - 1);
        const next = document.createElement('button');
        next.type = 'button';
        next.textContent = t('common.next');
        next.disabled = data.page >= pages;
        next.onclick = () => change(data.page + 1);
        element.append(info, sizeLabel, prev, next);
    }

    // ═══ Token ═══════════════════════

    function getToken() {
        return (localStorage.getItem(TOKEN_KEY) || '').trim();
    }

    function setToken(value) {
        localStorage.setItem(TOKEN_KEY, value);
    }

    function clearToken() {
        localStorage.removeItem(TOKEN_KEY);
    }

    function requireToken() {
        const params = new URLSearchParams(location.search);
        const queryToken = (params.get('token') || '').trim();
        if (queryToken) {
            setToken(queryToken);
            params.delete('token');
            const search = params.toString();
            history.replaceState(null, '',
                location.pathname + (search ? '?' + search : '') + location.hash);
        }
        const token = getToken();
        if (!token) {
            location.replace(LOGIN_PAGE);
            return '';
        }
        return token;
    }

    // ═══ API ═════════════════════════

    async function api(path, request = {}) {
        const token = getToken();
        const headers = {...(token ? {token} : {}), ...(request.headers || {})};
        if (request.body != null && !(request.body instanceof FormData))
            headers['Content-Type'] = 'application/json';
        const res = await fetch('/console' + path, {...request, headers});
        const text = await res.text();
        let body = null;
        if (text) {
            try {
                body = JSON.parse(text);
            } catch {
                body = text;
            }
        }
        if (res.status === 401) {
            clearToken();
            location.replace(LOGIN_PAGE);
            throw new Error('Unauthorized');
        }
        if (!res.ok) {
            const error = new Error(
                (body && body.detail) || (typeof body === 'string' && body) || res.statusText);
            error.status = res.status;
            throw error;
        }
        return body;
    }

    async function apiBlob(path) {
        const token = getToken();
        const res = await fetch('/console' + path, {headers: {token}});
        if (res.status === 401) {
            clearToken();
            location.replace(LOGIN_PAGE);
            throw new Error('Unauthorized');
        }
        if (!res.ok) {
            let message = res.statusText;
            try {
                const body = await res.json();
                message = body.detail || message;
            } catch {
                // keep the status text
            }
            throw new Error(message);
        }
        return res.blob();
    }

    // ═══ Toast ═══════════════════════

    function raiseToast(box) {
        if (typeof box.showPopover !== 'function') return;
        try {
            if (box.matches(':popover-open')) box.hidePopover();
            box.showPopover();
        } catch {
            // Keep the toast in the normal layer when the popover API is unavailable.
        }
    }

    function toast(message, kind = '') {
        const box = $('toast');
        if (!box) return;
        const item = document.createElement('div');
        item.className = 'toast-item' + (kind ? ' ' + kind : '');
        const text = document.createElement('span');
        text.className = 'toast-item__text';
        text.textContent = message;
        const cursor = document.createElement('span');
        cursor.className = 'toast-item__cursor';
        cursor.textContent = '█';
        cursor.setAttribute('aria-hidden', 'true');
        item.append(text, cursor);
        box.appendChild(item);
        raiseToast(box);
        requestAnimationFrame(() => item.classList.add('show'));
        setTimeout(() => {
            item.classList.remove('show');
            setTimeout(() => item.remove(), 220);
        }, 3200);
    }

    // ═══ Confirm ═════════════════════

    let confirmResolve = null;
    let confirmTimer = 0;

    function finishConfirm(result) {
        if (!confirmResolve) return;
        const resolve = confirmResolve;
        confirmResolve = null;
        clearTimeout(confirmTimer);
        confirmTimer = 0;
        const dialog = $('confirm-dialog');
        if (dialog) {
            dialog.classList.remove('confirm-dialog--run');
            const bar = $('confirm-bar');
            if (bar) bar.classList.remove('confirm-dialog__bar--run');
            if (dialog.open) dialog.close();
        }
        resolve(result);
    }

    function confirmAction(message) {
        const dialog = $('confirm-dialog');
        const bar = $('confirm-bar');
        const messageElement = $('confirm-message');
        if (!dialog || !bar || !messageElement) return Promise.resolve(false);
        if (confirmResolve) finishConfirm(false);
        messageElement.textContent = message;
        if (!dialog.open) dialog.showModal();
        dialog.classList.remove('confirm-dialog--run');
        bar.classList.remove('confirm-dialog__bar--run');
        void dialog.offsetWidth;
        dialog.classList.add('confirm-dialog--run');
        bar.classList.add('confirm-dialog__bar--run');
        dialog.focus();
        confirmTimer = setTimeout(() => finishConfirm(false), 10000);
        return new Promise(resolve => {
            confirmResolve = resolve;
        });
    }

    function bindBackdropClick(element, close) {
        let pressed = false;
        let released = false;
        element.addEventListener('pointerdown', event => {
            pressed = event.target === element;
            released = false;
        });
        element.addEventListener('pointerup', event => {
            released = event.target === element;
        });
        element.addEventListener('click', event => {
            if (pressed && released && event.target === element) close();
            pressed = false;
            released = false;
        });
    }

    function bindConfirm() {
        const dialog = $('confirm-dialog');
        if (!dialog) return;
        const ok = $('confirm-ok');
        const cancel = $('confirm-cancel');
        if (ok) ok.onclick = () => finishConfirm(true);
        if (cancel) cancel.onclick = () => finishConfirm(false);
        bindBackdropClick(dialog, () => finishConfirm(false));
        dialog.addEventListener('cancel', event => {
            event.preventDefault();
            finishConfirm(false);
        });
    }

    if (document.readyState === 'loading')
        document.addEventListener('DOMContentLoaded', bindConfirm, {once: true});
    else
        bindConfirm();

    window.ConsoleUtils = {
        $,
        escapeHtml,
        formatBytes,
        formatTime,
        setStatus,
        emptyRow,
        renderPager,
        api,
        apiBlob,
        toast,
        confirmAction,
        bindBackdropClick,
        getToken,
        setToken,
        clearToken,
        requireToken
    };
})();
