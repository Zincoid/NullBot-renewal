(() => {
    'use strict';

    const TOKEN_KEY = 'nullbot.console.token';
    const params = new URLSearchParams(location.search);
    const queryToken = (params.get('token') || '').trim();
    if (queryToken) {
        localStorage.setItem(TOKEN_KEY, queryToken);
        params.delete('token');
        const search = params.toString();
        history.replaceState(null, '',
            location.pathname + (search ? '?' + search : '') + location.hash);
    }
    const token = (localStorage.getItem(TOKEN_KEY) || '').trim();
    if (!token) {
        location.replace('login.html');
        return;
    }

    const $ = (id) => document.getElementById(id);
    const {t} = window.ConsoleUI;

    const PAGE_SIZE = 20;
    const UPLOAD_LINE_TTL = 4000;
    const BIZ_TYPES = [0, 1, 2, 3, 4];
    const ACCESS_LEVELS = [-2, -1, 0, 1, 2];

    function bizLabel(value) {
        const key = 'files.type' + value;
        const label = t(key);
        return label === key ? value : label;
    }

    function bizOptions(selected) {
        return options(BIZ_TYPES.map(value => ({value, label: bizLabel(value)})), selected);
    }

    function accessOptions(selected) {
        return options(ACCESS_LEVELS.map(value => ({value, label: t('access.' + value)})), selected);
    }

    // ═══ Helpers ═════════════════════

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function options(items, selected) {
        const selectedValue = selected == null ? '' : String(selected);
        return items.map(item => {
            const value = typeof item === 'string' ? item : item.value;
            const label = typeof item === 'string' ? item : item.label;
            return `<option value="${escapeHtml(value)}"${String(value) === selectedValue ? ' selected' : ''}>${escapeHtml(label)}</option>`;
        }).join('');
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

    function setRowStatus(element, text, kind = '', autoHide = false) {
        clearTimeout(element._statusTimer);
        element.textContent = text;
        element.className = 'row-status' + (kind ? ' ' + kind : '');
        if (autoHide) {
            element._statusTimer = setTimeout(() => {
                element.textContent = '';
                element.className = 'row-status';
            }, 3000);
        }
    }

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

    // ═══ API ═════════════════════════

    async function api(path, request = {}) {
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
            localStorage.removeItem(TOKEN_KEY);
            location.replace('login.html');
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
        const res = await fetch('/console' + path, {headers: {token}});
        if (res.status === 401) {
            localStorage.removeItem(TOKEN_KEY);
            location.replace('login.html');
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

    // ═══ Usage ═══════════════════════

    const usageState = {days: 14};
    let usageData = null;
    let usageResizeFrame = 0;

    async function loadBot() {
        try {
            renderBot(await api('/bot'));
        } catch {
            const img = $('bot-avatar-img');
            img.hidden = true;
            img.removeAttribute('src');
            $('bot-name').textContent = '-';
            $('bot-id').textContent = '';
            $('bot-state').textContent = t('bot.loadFailed');
            $('bot-state').className = 'bot-state error';
        }
    }

    function renderBot(data) {
        const online = Boolean(data && data.online);
        const id = online && data.userId ? String(data.userId) : '';
        $('bot-name').textContent = online
            ? (data.nickname || id || '-')
            : t('bot.none');
        $('bot-id').textContent = id ? t('bot.id', {id}) : '';
        $('bot-state').textContent = online ? t('bot.online') : t('bot.offline');
        $('bot-state').className = 'bot-state ' + (online ? 'online' : 'offline');

        const img = $('bot-avatar-img');
        if (online && data.avatar) {
            img.onerror = () => {
                img.hidden = true;
            };
            img.src = String(data.avatar);
            img.hidden = false;
        } else {
            img.onerror = null;
            img.hidden = true;
            img.removeAttribute('src');
        }
    }

    async function loadUsage() {
        const chart = $('usage-chart');
        chart.innerHTML = `<p class="empty">${escapeHtml(t('common.loading'))}</p>`;
        try {
            renderUsage(await api('/usage?days=' + usageState.days));
        } catch (e) {
            chart.innerHTML = `<p class="empty">${escapeHtml(t('usage.loadFailed', {msg: e.message}))}</p>`;
            toast(t('usage.loadFailed', {msg: e.message}), 'error');
        }
    }

    function renderUsage(data) {
        usageData = data;
        $('usage-total').textContent = Number(data.total || 0).toLocaleString();
        $('usage-users').textContent = Number(data.activeUsers || 0).toLocaleString();
        $('usage-groups').textContent = Number(data.activeGroups || 0).toLocaleString();
        $('usage-range').textContent = t('usage.days', {n: data.days});
        const first = data.daily.length ? data.daily[0].date : '-';
        const last = data.daily.length ? data.daily[data.daily.length - 1].date : '-';
        $('usage-chart-note').textContent = t('usage.note', {first, last});
        $('usage-chart').innerHTML = usageSvg(data.daily);
        renderRanks($('cmd-ranks'), data.cmds, t('usage.noCmds'),
            rank => rank.name || rank.key);
        renderRanks($('group-ranks'), data.groups, t('usage.noGroups'),
            rank => rank.key === '0' ? t('usage.dm') : (rank.name || rank.key));
    }

    function usageSvg(days) {
        if (!days.length) return `<p class="empty">${escapeHtml(t('usage.empty'))}</p>`;
        const width = Math.max(320, Math.round($('usage-chart').clientWidth || 720));
        const height = 120;
        const left = 36;
        const right = 16;
        const top = 10;
        const bottom = 24;
        const max = Math.max(1, ...days.map(day => Number(day.count) || 0));
        const step = days.length > 1 ? (width - left - right) / (days.length - 1) : 0;
        const x = index => left + step * index;
        const y = value => top + (height - top - bottom) * (1 - (Number(value) || 0) / max);
        const points = days.map((day, index) => ({x: x(index), y: y(day.count)}));
        const line = smoothPath(points);
        const baseline = height - bottom;
        const area = days.length > 1
            ? `${line} L ${points[points.length - 1].x.toFixed(1)} ${baseline}`
                + ` L ${points[0].x.toFixed(1)} ${baseline} Z`
            : '';

        let grid = '';
        for (let i = 0; i <= 3; i++) {
            const lineY = top + (height - top - bottom) * i / 3;
            const value = Math.round(max * (1 - i / 3));
            grid += `<line x1="${left}" y1="${lineY}" x2="${width - right}" y2="${lineY}" class="grid"/>`
                + `<text x="${left - 8}" y="${lineY + 4}" class="axis" text-anchor="end">${value}</text>`;
        }

        const tick = Math.max(1, Math.ceil(days.length / 6));
        let labels = '';
        days.forEach((day, index) => {
            if (index % tick !== 0 && index !== days.length - 1) return;
            labels += `<text x="${x(index)}" y="${height - bottom + 16}" class="axis" text-anchor="middle">`
                + `${escapeHtml(String(day.date).slice(5))}</text>`;
        });

        const dots = days.map((day, index) => {
            const point = points[index];
            const hitLeft = index === 0 ? left : point.x - step / 2;
            const hitRight = index === days.length - 1 ? width - right : point.x + step / 2;
            return `<g class="point">`
                + `<rect x="${hitLeft.toFixed(1)}" y="${top}"`
                + ` width="${(hitRight - hitLeft).toFixed(1)}" height="${(baseline - top).toFixed(1)}" class="hit">`
                + `<title>${escapeHtml(day.date)}：${Number(day.count) || 0}</title></rect>`
                + `<line x1="${point.x.toFixed(1)}" y1="${top}"`
                + ` x2="${point.x.toFixed(1)}" y2="${baseline}" class="guide"/>`
                + `<line x1="${left}" y1="${point.y.toFixed(1)}"`
                + ` x2="${width - right}" y2="${point.y.toFixed(1)}" class="guide"/>`
                + `<circle cx="${point.x.toFixed(1)}" cy="${point.y.toFixed(1)}" r="2.5" class="dot"/>`
                + `</g>`;
        }).join('');

        return `<svg viewBox="0 0 ${width} ${height}" role="img" aria-label="${escapeHtml(t('usage.daily'))}">
            <defs>
                <linearGradient id="usage-fill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" class="fill-start"/>
                    <stop offset="100%" class="fill-end"/>
                </linearGradient>
            </defs>
            ${grid}
            <path d="${area}" fill="url(#usage-fill)"/>
            <path d="${line}" class="line"/>
            ${dots}
            ${labels}
        </svg>`;
    }

    function smoothPath(points) {
        if (points.length < 2)
            return `M ${points[0].x.toFixed(1)} ${points[0].y.toFixed(1)}`;
        const widths = [];
        const slopes = [];
        for (let i = 0; i < points.length - 1; i++) {
            widths[i] = points[i + 1].x - points[i].x;
            slopes[i] = (points[i + 1].y - points[i].y) / widths[i];
        }
        const tangents = new Array(points.length);
        tangents[0] = slopes[0];
        tangents[points.length - 1] = slopes[slopes.length - 1];
        for (let i = 1; i < points.length - 1; i++) {
            if (slopes[i - 1] * slopes[i] <= 0) {
                tangents[i] = 0;
                continue;
            }
            const w1 = 2 * widths[i] + widths[i - 1];
            const w2 = widths[i] + 2 * widths[i - 1];
            tangents[i] = (w1 + w2) / (w1 / slopes[i - 1] + w2 / slopes[i]);
        }
        let path = `M ${points[0].x.toFixed(1)} ${points[0].y.toFixed(1)}`;
        for (let i = 0; i < points.length - 1; i++) {
            const handle = widths[i] / 3;
            const c1x = points[i].x + handle;
            const c1y = points[i].y + tangents[i] * handle;
            const c2x = points[i + 1].x - handle;
            const c2y = points[i + 1].y - tangents[i + 1] * handle;
            path += ` C ${c1x.toFixed(1)} ${c1y.toFixed(1)}`
                + ` ${c2x.toFixed(1)} ${c2y.toFixed(1)}`
                + ` ${points[i + 1].x.toFixed(1)} ${points[i + 1].y.toFixed(1)}`;
        }
        return path;
    }

    function renderRanks(element, ranks, empty, label) {
        if (!ranks || !ranks.length) {
            element.innerHTML = `<p class="empty">${escapeHtml(empty)}</p>`;
            return;
        }
        const max = Math.max(1, ...ranks.map(rank => Number(rank.count) || 0));
        element.innerHTML = ranks.map((rank, index) => `
            <div class="rank-row">
                <span><span class="rank-index">${index + 1}</span>${escapeHtml(label(rank))}</span>
                <span class="rank-count">${Number(rank.count) || 0}</span>
                <div class="rank-bar"><span style="width:${(Number(rank.count) || 0) / max * 100}%"></span></div>
            </div>`).join('');
    }

    // ═══ Config ══════════════════════

    async function loadConfig() {
        const status = $('config-status');
        const total = $('config-total');
        total.textContent = '';
        setStatus(status, t('config.loading'));
        try {
            const list = await api('/config');
            renderConfigs(list);
            setStatus(status, '');
            total.textContent = t('config.count', {n: list.length});
        } catch (e) {
            total.textContent = '';
            setStatus(status, e.message, 'error');
        }
    }

    function renderConfigs(list) {
        const container = $('configs');
        container.innerHTML = '';
        for (const item of list) {
            const card = document.createElement('section');
            card.className = 'config';
            const description = item.description || '';
            const defaultValue = item.defaultValue || t('config.empty');
            const defaultText = t('config.default') + defaultValue;
            card.innerHTML = `
                <div class="config-head">
                    <span class="badge"${item.customized ? '' : ' hidden'}>${escapeHtml(t('config.modified'))}</span>
                    <code>${escapeHtml(item.key)}</code>
                    <span class="desc" title="${escapeHtml(description)} · ${escapeHtml(defaultText)}">${escapeHtml(description)}<span class="default"> · ${escapeHtml(t('config.default'))}<code>${escapeHtml(defaultValue)}</code></span></span>
                </div>
                <textarea class="value" rows="2" spellcheck="false"></textarea>
                <div class="actions">
                    <span class="row-status"></span>
                    <button class="save small btn-save">${escapeHtml(t('common.save'))}</button>
                    <button class="reset small btn-reset">${escapeHtml(t('config.reset'))}</button>
                </div>`;
            const input = card.querySelector('.value');
            const rowStatus = card.querySelector('.row-status');
            input.value = item.value ?? '';
            card.querySelector('.save').onclick = () => saveConfig(card, item, input, rowStatus);
            card.querySelector('.reset').onclick = () => resetConfig(card, item, input, rowStatus);
            container.appendChild(card);
        }
    }

    async function saveConfig(card, item, input, rowStatus) {
        setRowStatus(rowStatus, t('config.saving'));
        try {
            const view = await api('/config/' + encodeURIComponent(item.key), {
                method: 'PUT',
                body: JSON.stringify({value: input.value})
            });
            item.value = view.value ?? '';
            input.value = item.value;
            card.querySelector('.badge').hidden = !view.customized;
            setRowStatus(rowStatus, t('config.saved'), 'ok', true);
        } catch (e) {
            setRowStatus(rowStatus, e.message, 'error');
        }
    }

    async function resetConfig(card, item, input, rowStatus) {
        if (!await confirmAction(t('config.resetConfirm', {key: item.key}))) return;
        setRowStatus(rowStatus, t('config.resetting'));
        try {
            const view = await api('/config/' + encodeURIComponent(item.key) + '/reset', {method: 'POST'});
            item.value = view.value ?? '';
            input.value = item.value;
            card.querySelector('.badge').hidden = !view.customized;
            setRowStatus(rowStatus, t('config.resetDone'), 'ok', true);
        } catch (e) {
            setRowStatus(rowStatus, e.message, 'error');
        }
    }

    // ═══ Files ═══════════════════════

    const fileState = {page: 1, size: PAGE_SIZE, type: '', keyword: '', items: []};
    const fileSelection = new Set();
    let editingFile = null;
    let previewUrl = null;
    let previewFile = null;
    let uploading = false;
    let deletingFiles = false;

    function updateFileSelection() {
        const selectAll = $('files-select-all');
        const batch = $('files-batch-delete');
        const total = fileState.items.length;
        const selectedOnPage = fileState.items.filter(file => fileSelection.has(file.id)).length;
        selectAll.disabled = total === 0;
        selectAll.checked = total > 0 && selectedOnPage === total;
        selectAll.indeterminate = selectedOnPage > 0 && selectedOnPage < total;
        batch.disabled = fileSelection.size === 0 || deletingFiles;
        batch.textContent = fileSelection.size
            ? t('files.batchDeleteCount', {n: fileSelection.size})
            : t('files.batchDelete');
    }

    async function loadFiles() {
        fileSelection.clear();
        const params = new URLSearchParams({
            page: String(fileState.page),
            size: String(fileState.size)
        });
        if (fileState.type) params.set('type', fileState.type);
        if (fileState.keyword) params.set('keyword', fileState.keyword);
        try {
            const data = await api('/files?' + params.toString());
            fileState.items = data.items;
            renderFiles(data);
        } catch (e) {
            fileState.items = [];
            $('files-body').innerHTML = emptyRow(9, t('common.loadError', {msg: e.message}));
            $('files-total').textContent = '';
            $('files-pager').innerHTML = '';
            updateFileSelection();
            toast(t('files.loadFailed', {msg: e.message}), 'error');
        }
    }

    function renderFiles(data) {
        $('files-total').textContent = t('files.total', {n: data.total});
        const body = $('files-body');
        if (!data.items.length) {
            body.innerHTML = emptyRow(9, t('files.empty'));
        } else {
            body.innerHTML = data.items.map(file => {
                const name = escapeHtml(file.original || '-');
                const title = escapeHtml(file.original || '');
                const fileName = previewKind(file)
                    ? `<button type="button" class="file-name file-name--link"
                               data-action="preview" data-id="${file.id}" title="${title}"
                               aria-label="${escapeHtml(t('files.preview'))}: ${title}">${name}</button>`
                    : `<span class="file-name" title="${title}">${name}</span>`;
                return `
                <tr>
                    <td class="check-col"><input type="checkbox" class="file-select" data-id="${file.id}"
                                                 aria-label="${escapeHtml(t('files.select'))}"
                                                 ${fileSelection.has(file.id) ? 'checked' : ''}></td>
                    <td>${file.id}</td>
                    <td>${fileName}</td>
                    <td><span class="file-description" title="${escapeHtml(file.description || '')}">${escapeHtml(file.description || '-')}</span></td>
                    <td class="nowrap">${escapeHtml(bizLabel(file.type) || file.type || '-')}</td>
                    <td class="nowrap">${formatBytes(file.size)}</td>
                    <td class="nowrap"><code>${escapeHtml(file.userId || '-')}</code></td>
                    <td class="nowrap">${formatTime(file.createdAt)}</td>
                    <td class="actions">
                        <button class="small btn-edit" data-action="edit" data-id="${file.id}">${escapeHtml(t('common.edit'))}</button>
                        <button class="small btn-download" data-action="download" data-id="${file.id}">${escapeHtml(t('common.download'))}</button>
                        <button class="small danger btn-delete" data-action="delete" data-id="${file.id}">${escapeHtml(t('common.delete'))}</button>
                    </td>
                </tr>`;
            }).join('');
        }
        renderPager($('files-pager'), data, page => {
            fileState.page = page;
            loadFiles();
        }, size => {
            fileState.size = size;
            fileState.page = 1;
            loadFiles();
        });
        updateFileSelection();
    }

    function openFileDialog(file) {
        editingFile = file;
        $('file-original').value = file.original || '';
        $('file-description').value = file.description || '';
        setStatus($('file-form-status'), '');
        $('file-dialog').showModal();
    }

    async function saveFileInfo(event) {
        event.preventDefault();
        if (!editingFile) return;
        const original = $('file-original').value.trim();
        if (!original) {
            setStatus($('file-form-status'), t('files.nameRequired'), 'error');
            return;
        }
        setStatus($('file-form-status'), t('files.saving'));
        try {
            await api('/files/' + editingFile.id, {
                method: 'PUT',
                body: JSON.stringify({
                    original,
                    description: $('file-description').value
                })
            });
            $('file-dialog').close();
            toast(t('files.saved'), 'ok');
            loadFiles();
        } catch (e) {
            setStatus($('file-form-status'), e.message, 'error');
        }
    }

    async function downloadFile(file) {
        try {
            const blob = await apiBlob('/files/' + file.id + '/content');
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = file.original || ('file-' + file.id);
            document.body.appendChild(link);
            link.click();
            link.remove();
            setTimeout(() => URL.revokeObjectURL(url), 10000);
        } catch (e) {
            toast(t('files.downloadFailed', {msg: e.message}), 'error');
        }
    }

    const UNPREVIEWABLE_AUDIO = new Set(['audio/silk', 'audio/x-silk', 'audio/amr']);

    function previewKind(file) {
        const type = (file.contentType || '').toLowerCase();
        if (type.startsWith('image/')) return 'image';
        if (type.startsWith('video/')) return 'video';
        if (type.startsWith('audio/')) return 'audio';
        return null;
    }

    function releasePreview() {
        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
            previewUrl = null;
        }
        $('preview-body').innerHTML = '';
    }

    async function openPreview(file) {
        const kind = previewKind(file);
        if (!kind) return;
        previewFile = file;
        const type = (file.contentType || '').toLowerCase();
        if (kind === 'audio' && UNPREVIEWABLE_AUDIO.has(type)) {
            releasePreview();
            $('preview-title').textContent = file.original || ('file-' + file.id);
            const wrap = document.createElement('div');
            wrap.className = 'preview-unsupported';
            const message = document.createElement('p');
            message.textContent = t('files.previewUnsupported', {type: file.contentType || ''});
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'small';
            button.textContent = t('common.download');
            button.addEventListener('click', () => downloadFile(file));
            wrap.append(message, button);
            $('preview-body').appendChild(wrap);
            $('preview-dialog').showModal();
            return;
        }
        try {
            const blob = await apiBlob('/files/' + file.id + '/content');
            releasePreview();
            previewUrl = URL.createObjectURL(blob);
            $('preview-title').textContent = file.original || ('file-' + file.id);
            const media = document.createElement(kind === 'image' ? 'img' : kind);
            media.className = 'preview-media';
            if (kind === 'image') {
                media.alt = file.original || '';
            } else {
                media.controls = true;
                media.autoplay = true;
                if (kind === 'video') media.volume = 0.25;
            }
            media.src = previewUrl;
            $('preview-body').appendChild(media);
            $('preview-dialog').showModal();
        } catch (e) {
            toast(t('files.previewFailed', {msg: e.message}), 'error');
        }
    }

    async function deleteFile(file) {
        if (!await confirmAction(t('files.deleteConfirm', {name: file.original || file.id}))) return;
        try {
            await api('/files/' + file.id, {method: 'DELETE'});
            toast(t('files.deleted'), 'ok');
            if (fileState.page > 1 && fileState.items.length === 1) fileState.page--;
            loadFiles();
        } catch (e) {
            toast(t('files.deleteFailed', {msg: e.message}), 'error');
        }
    }

    async function deleteSelectedFiles() {
        if (deletingFiles || fileSelection.size === 0) return;
        const ids = Array.from(fileSelection);
        if (!await confirmAction(t('files.batchDeleteConfirm', {n: ids.length}))) return;
        deletingFiles = true;
        updateFileSelection();
        try {
            const results = await Promise.allSettled(ids.map(id =>
                api('/files/' + id, {method: 'DELETE'})));
            const deleted = results.filter(result => result.status === 'fulfilled').length;
            const failed = results.length - deleted;
            if (fileState.page > 1 && deleted >= fileState.items.length) fileState.page--;
            toast(t('files.batchDeleteDone', {ok: deleted, fail: failed}),
                failed ? 'error' : 'ok');
        } finally {
            deletingFiles = false;
            loadFiles();
        }
    }

    function updateUploadLabel() {
        const count = ($('upload-files').files || []).length;
        $('upload-files-label').textContent = count
            ? t('files.selectedCount', {n: count})
            : t('files.noFiles');
    }

    function dismissUploadLine(line, delay = UPLOAD_LINE_TTL) {
        setTimeout(() => {
            line.classList.add('fade-out');
            setTimeout(() => line.remove(), 250);
        }, delay);
    }

    async function uploadFiles() {
        if (uploading) return;
        const files = Array.from($('upload-files').files || []);
        if (!files.length) {
            toast(t('files.selectFirst'), 'error');
            return;
        }
        const type = $('upload-type').value;
        const description = $('upload-description').value.trim();
        const statusBox = $('upload-status');
        statusBox.innerHTML = '';
        const lines = files.map(file => {
            const line = document.createElement('div');
            line.className = 'upload-line';
            line.textContent = t('files.waiting', {name: file.name});
            statusBox.appendChild(line);
            return line;
        });
        let next = 0;
        let success = 0;
        let failed = 0;

        uploading = true;
        $('upload-button').disabled = true;

        async function worker() {
            while (true) {
                const index = next++;
                if (index >= files.length) return;
                const file = files[index];
                const line = lines[index];
                line.textContent = t('files.uploading', {name: file.name});
                line.className = 'upload-line';
                try {
                    const form = new FormData();
                    form.append('type', type);
                    if (description) form.append('description', description);
                    form.append('file', file);
                    await api('/files', {method: 'POST', body: form});
                    success++;
                    line.textContent = t('files.uploaded', {name: file.name});
                    line.className = 'upload-line ok';
                    dismissUploadLine(line);
                } catch (e) {
                    failed++;
                    line.textContent = t('files.uploadFailed', {name: file.name, msg: e.message});
                    line.className = 'upload-line error';
                    dismissUploadLine(line);
                }
            }
        }

        await Promise.all(Array.from({length: Math.min(3, files.length)}, () => worker()));
        uploading = false;
        $('upload-button').disabled = false;
        $('upload-files').value = '';
        updateUploadLabel();
        toast(t('files.uploadDone', {ok: success, fail: failed}), failed ? 'error' : 'ok');
        fileState.page = 1;
        loadFiles();
    }

    // ═══ Accounts ════════════════════

    const userState = {page: 1, size: PAGE_SIZE, keyword: '', items: []};
    const groupState = {page: 1, size: PAGE_SIZE, keyword: '', items: []};
    let accountsTab = 'groups';
    let currentGroup = null;
    let currentUser = null;
    let currentKeywords = [];
    let editingKeyword = null;
    const KW_ARG_PRESETS = {user: '{userId}', group: '{groupId}'};
    const KW_ARG_TYPES = [
        {value: 'text', labelKey: 'group.keywordArgText'},
        {value: 'user', labelKey: 'group.keywordArgUser'},
        {value: 'group', labelKey: 'group.keywordArgGroup'}
    ];

    function loadAccounts() {
        if (accountsTab === 'users') loadUsers();
        else loadGroups();
    }

    function showAccountsTab(tab) {
        accountsTab = tab;
        document.querySelectorAll('[data-tab]').forEach(button =>
            button.classList.toggle('active', button.dataset.tab === tab));
        $('tab-users').hidden = tab !== 'users';
        $('tab-groups').hidden = tab !== 'groups';
    }

    function switchAccountsTab(tab) {
        showAccountsTab(tab);
        loadAccounts();
    }

    async function loadUsers() {
        const params = new URLSearchParams({
            page: String(userState.page),
            size: String(userState.size)
        });
        if (userState.keyword) params.set('keyword', userState.keyword);
        try {
            const data = await api('/users?' + params.toString());
            userState.items = data.items;
            renderUsers(data);
        } catch (e) {
            $('users-body').innerHTML = emptyRow(5, t('common.loadError', {msg: e.message}));
            $('users-total').textContent = '';
            $('users-pager').innerHTML = '';
            toast(t('accounts.loadUsersFailed', {msg: e.message}), 'error');
        }
    }

    function renderUsers(data) {
        $('users-total').textContent = t('accounts.usersTotal', {n: data.total});
        const body = $('users-body');
        if (!data.items.length) {
            body.innerHTML = emptyRow(5, t('accounts.usersEmpty'));
        } else {
            body.innerHTML = data.items.map(user => `
                <tr data-id="${escapeHtml(user.id)}">
                    <td><code>${escapeHtml(user.id)}</code></td>
                    <td><span class="name-text">${escapeHtml(user.name || '-')}</span></td>
                    <td><select class="access-select">${accessOptions(user.access)}</select></td>
                    <td class="private-chat-cell">
                        <div class="checks"><label><input type="checkbox" class="private-chat-toggle"
                            aria-label="${escapeHtml(t('accounts.privateChat'))}"
                            ${user.allowPrivateChat ? 'checked' : ''}></label></div>
                    </td>
                    <td class="actions">
                        <button class="small primary btn-save" data-save-user>${escapeHtml(t('common.save'))}</button>
                        <button class="small danger btn-delete" data-delete-user>${escapeHtml(t('common.delete'))}</button>
                        <span class="actions-divider" aria-hidden="true"></span>
                        <button class="small btn-settings" data-user-settings>${escapeHtml(t('accounts.settings'))}</button>
                    </td>
                </tr>`).join('');
        }
        renderPager($('users-pager'), data, page => {
            userState.page = page;
            loadUsers();
        }, size => {
            userState.size = size;
            userState.page = 1;
            loadUsers();
        });
    }

    async function loadGroups() {
        const params = new URLSearchParams({
            page: String(groupState.page),
            size: String(groupState.size)
        });
        if (groupState.keyword) params.set('keyword', groupState.keyword);
        try {
            const data = await api('/groups?' + params.toString());
            groupState.items = data.items;
            renderGroups(data);
        } catch (e) {
            $('groups-body').innerHTML = emptyRow(4, t('common.loadError', {msg: e.message}));
            $('groups-total').textContent = '';
            $('groups-pager').innerHTML = '';
            toast(t('accounts.loadGroupsFailed', {msg: e.message}), 'error');
        }
    }

    function renderGroups(data) {
        $('groups-total').textContent = t('accounts.groupsTotal', {n: data.total});
        const body = $('groups-body');
        if (!data.items.length) {
            body.innerHTML = emptyRow(4, t('accounts.groupsEmpty'));
        } else {
            body.innerHTML = data.items.map(group => `
                <tr data-id="${escapeHtml(group.id)}">
                    <td><code>${escapeHtml(group.id)}</code></td>
                    <td><span class="name-text">${escapeHtml(group.name || '-')}</span></td>
                    <td><select class="access-select">${accessOptions(group.access)}</select></td>
                    <td class="actions">
                        <button class="small primary btn-save" data-save-group>${escapeHtml(t('common.save'))}</button>
                        <button class="small danger btn-delete" data-delete-group>${escapeHtml(t('common.delete'))}</button>
                        <span class="actions-divider" aria-hidden="true"></span>
                        <button class="small btn-settings" data-group-settings>${escapeHtml(t('accounts.settings'))}</button>
                    </td>
                </tr>`).join('');
        }
        renderPager($('groups-pager'), data, page => {
            groupState.page = page;
            loadGroups();
        }, size => {
            groupState.size = size;
            groupState.page = 1;
            loadGroups();
        });
    }

    async function saveUser(button) {
        const row = button.closest('tr');
        const id = row.dataset.id;
        const access = Number(row.querySelector('.access-select').value);
        const allowPrivateChat = row.querySelector('.private-chat-toggle').checked;
        button.disabled = true;
        try {
            await api('/users/' + encodeURIComponent(id), {
                method: 'PUT',
                body: JSON.stringify({access, allowPrivateChat})
            });
            toast(t('accounts.userSaved'), 'ok');
        } catch (e) {
            toast(t('accounts.saveFailed', {msg: e.message}), 'error');
        } finally {
            button.disabled = false;
        }
    }

    async function saveGroup(button) {
        const row = button.closest('tr');
        const id = row.dataset.id;
        const access = Number(row.querySelector('.access-select').value);
        button.disabled = true;
        try {
            await api('/groups/' + encodeURIComponent(id), {
                method: 'PUT',
                body: JSON.stringify({access})
            });
            toast(t('accounts.groupSaved'), 'ok');
        } catch (e) {
            toast(t('accounts.saveFailed', {msg: e.message}), 'error');
        } finally {
            button.disabled = false;
        }
    }

    async function deleteUser(button) {
        const id = button.closest('tr').dataset.id;
        const user = userState.items.find(item => item.id === id);
        const name = user?.name || id;
        if (!await confirmAction(t('accounts.userDeleteConfirm', {name}))) return;
        button.disabled = true;
        try {
            await api('/users/' + encodeURIComponent(id), {method: 'DELETE'});
            toast(t('accounts.userDeleted'), 'ok');
            if (userState.page > 1 && userState.items.length === 1) userState.page--;
            loadUsers();
        } catch (e) {
            toast(t('accounts.deleteFailed', {msg: e.message}), 'error');
            button.disabled = false;
        }
    }

    async function deleteGroup(button) {
        const id = button.closest('tr').dataset.id;
        const group = groupState.items.find(item => item.id === id);
        const name = group?.name || id;
        if (!await confirmAction(t('accounts.groupDeleteConfirm', {name}))) return;
        button.disabled = true;
        try {
            await api('/groups/' + encodeURIComponent(id), {method: 'DELETE'});
            toast(t('accounts.groupDeleted'), 'ok');
            if (groupState.page > 1 && groupState.items.length === 1) groupState.page--;
            loadGroups();
        } catch (e) {
            toast(t('accounts.deleteFailed', {msg: e.message}), 'error');
            button.disabled = false;
        }
    }

    function openUserDialog(user) {
        currentUser = user;
        $('user-dialog-subtitle').textContent = user.name ? user.name + ' · ' + user.id : user.id;
        setStatus($('us-status'), '');
        $('us-effort').value = '3';
        $('us-guard').checked = true;
        $('us-prompt').value = '';
        $('user-dialog').showModal();
        loadUserSetting();
    }

    async function loadUserSetting() {
        if (!currentUser) return;
        try {
            const setting = await api('/users/' + encodeURIComponent(currentUser.id) + '/setting');
            $('us-effort').value = String(setting.aiEffort);
            $('us-guard').checked = setting.aiInjectionGuard;
            $('us-prompt').value = setting.aiPrompt || '';
            setStatus($('us-status'), '');
        } catch (e) {
            setStatus($('us-status'), e.message, 'error');
        }
    }

    async function saveUserSetting(event) {
        event.preventDefault();
        if (!currentUser) return;
        setStatus($('us-status'), t('accounts.saving'));
        try {
            await api('/users/' + encodeURIComponent(currentUser.id) + '/setting', {
                method: 'PUT',
                body: JSON.stringify({
                    aiEffort: Number($('us-effort').value),
                    aiPrompt: $('us-prompt').value,
                    aiInjectionGuard: $('us-guard').checked
                })
            });
            $('user-dialog').close();
            toast(t('accounts.userSettingSaved'), 'ok');
        } catch (e) {
            setStatus($('us-status'), e.message, 'error');
        }
    }

    function openGroupDialog(group) {
        currentGroup = group;
        $('group-dialog-title').textContent = group.name || group.id;
        $('group-dialog-subtitle').textContent = t('group.id', {id: group.id});
        setStatus($('gs-status'), '');
        setStatus($('kw-status'), '');
        resetKeywordForm();
        switchGroupTab('setting');
        $('group-dialog').showModal();
        loadGroupSetting();
        loadKeywords();
    }

    function switchGroupTab(tab) {
        document.querySelectorAll('[data-group-tab]').forEach(button =>
            button.classList.toggle('active', button.dataset.groupTab === tab));
        $('group-tab-setting').hidden = tab !== 'setting';
        $('group-tab-keywords').hidden = tab !== 'keywords';
    }

    async function loadGroupSetting() {
        if (!currentGroup) return;
        try {
            const setting = await api('/groups/' + encodeURIComponent(currentGroup.id) + '/setting');
            $('gs-language').value = String(setting.language);
            $('gs-join-notice').checked = setting.joinNoticeEnabled;
            $('gs-leave-notice').checked = setting.leaveNoticeEnabled;
            $('gs-anti-recall').checked = setting.antiRecallEnabled;
            $('gs-effort').value = String(setting.aiEffort);
            $('gs-scope').value = String(setting.limitScope);
            $('gs-capacity').value = setting.limitCapacity;
            $('gs-refill').value = setting.limitRefill;
            $('gs-interval').value = setting.limitInterval;
            $('gs-chat-frequency').value = setting.aiChatFrequency;
            $('gs-limit-enabled').checked = setting.limitEnabled;
            $('gs-auto-chat').checked = setting.aiAutoChatEnabled;
            $('gs-guard').checked = setting.aiInjectionGuard;
            $('gs-prompt').value = setting.aiPrompt || '';
            setStatus($('gs-status'), '');
        } catch (e) {
            setStatus($('gs-status'), e.message, 'error');
        }
    }

    async function saveGroupSetting(event) {
        event.preventDefault();
        if (!currentGroup) return;
        const capacity = Number($('gs-capacity').value);
        const refill = Number($('gs-refill').value);
        const interval = Number($('gs-interval').value);
        const frequencyText = $('gs-chat-frequency').value.trim();
        const frequency = Number(frequencyText);
        if (!Number.isInteger(capacity) || capacity < 1
            || !Number.isInteger(refill) || refill < 1
            || !Number.isInteger(interval) || interval < 1) {
            setStatus($('gs-status'), t('group.positiveInts'), 'error');
            return;
        }
        if (!frequencyText || !Number.isFinite(frequency) || frequency < 0 || frequency > 1) {
            setStatus($('gs-status'), t('group.frequencyRange'), 'error');
            return;
        }
        setStatus($('gs-status'), t('group.saving'));
        try {
            await api('/groups/' + encodeURIComponent(currentGroup.id) + '/setting', {
                method: 'PUT',
                body: JSON.stringify({
                    language: Number($('gs-language').value),
                    joinNoticeEnabled: $('gs-join-notice').checked,
                    leaveNoticeEnabled: $('gs-leave-notice').checked,
                    antiRecallEnabled: $('gs-anti-recall').checked,
                    aiEffort: Number($('gs-effort').value),
                    limitScope: Number($('gs-scope').value),
                    limitCapacity: capacity,
                    limitRefill: refill,
                    limitInterval: interval,
                    limitEnabled: $('gs-limit-enabled').checked,
                    aiInjectionGuard: $('gs-guard').checked,
                    aiAutoChatEnabled: $('gs-auto-chat').checked,
                    aiChatFrequency: frequency,
                    aiPrompt: $('gs-prompt').value
                })
            });
            toast(t('group.saved'), 'ok');
            loadGroupSetting();
        } catch (e) {
            setStatus($('gs-status'), e.message, 'error');
        }
    }

    async function loadKeywords() {
        if (!currentGroup) return;
        try {
            currentKeywords = await api('/groups/' + encodeURIComponent(currentGroup.id) + '/keywords');
            renderKeywords();
            setStatus($('kw-status'), '');
        } catch (e) {
            setStatus($('kw-status'), e.message, 'error');
        }
    }

    function renderKeywords() {
        const body = $('kw-body');
        if (!currentKeywords.length) {
            body.innerHTML = emptyRow(6, t('group.keywordEmpty'));
            return;
        }
        body.innerHTML = currentKeywords.map(rule => `
            <tr data-id="${rule.id}">
                <td>${rule.id}</td>
                <td>${escapeHtml(rule.keyword)}</td>
                <td><code>${escapeHtml(rule.cmd)}</code></td>
                <td>${escapeHtml((rule.args || []).join(' ')) || '<span class="muted">-</span>'}</td>
                <td><span class="badge${rule.enabled ? '' : ' off'}">${escapeHtml(rule.enabled ? t('group.enabled') : t('group.disabled'))}</span></td>
                <td class="actions">
                    <button class="small btn-edit" data-kw-action="edit" data-id="${rule.id}">${escapeHtml(t('common.edit'))}</button>
                    <button class="small btn-toggle ${rule.enabled ? 'btn-disable' : 'btn-enable'}" data-kw-action="toggle" data-id="${rule.id}">${escapeHtml(rule.enabled ? t('group.disabled') : t('group.enabled'))}</button>
                    <button class="small danger btn-delete" data-kw-action="delete" data-id="${rule.id}">${escapeHtml(t('common.delete'))}</button>
                </td>
            </tr>`).join('');
    }

    function kwArgType(value) {
        const text = String(value ?? '').trim();
        if (text === KW_ARG_PRESETS.user) return 'user';
        if (text === KW_ARG_PRESETS.group) return 'group';
        return 'text';
    }

    function kwArgTypeOptions(selected) {
        return options(KW_ARG_TYPES.map(item => ({value: item.value, label: t(item.labelKey)})), selected);
    }

    function kwArgRow(value, type) {
        const preset = KW_ARG_PRESETS[type] || '';
        const row = document.createElement('div');
        row.className = 'kw-arg-row';
        row.innerHTML = `
            <select class="kw-arg-type" aria-label="${escapeHtml(t('group.keywordArgType'))}">
                ${kwArgTypeOptions(type)}
            </select>
            <input class="kw-arg-value" value="${escapeHtml(value ?? '')}"
                   ${preset ? 'disabled' : ''}
                   placeholder="${escapeHtml(preset || t('group.keywordArgValue'))}"
                   aria-label="${escapeHtml(t('group.keywordArgValue'))}">
            <div class="kw-arg-btns">
                <button type="button" class="small kw-arg-btn kw-arg-add"
                        title="${escapeHtml(t('group.keywordArgAdd'))}"
                        aria-label="${escapeHtml(t('group.keywordArgAdd'))}">+</button>
                <button type="button" class="small kw-arg-btn kw-arg-remove"
                        title="${escapeHtml(t('group.keywordArgRemove'))}"
                        aria-label="${escapeHtml(t('group.keywordArgRemove'))}">−</button>
            </div>`;
        return row;
    }

    function renderKeywordArgs(args) {
        const list = $('kw-args-list');
        list.innerHTML = '';
        const values = Array.isArray(args) && args.length ? args : [''];
        values.forEach(value => list.appendChild(kwArgRow(value, kwArgType(value))));
    }

    function applyKwArgType(row, type) {
        const select = row.querySelector('.kw-arg-type');
        const input = row.querySelector('.kw-arg-value');
        select.value = type;
        const preset = KW_ARG_PRESETS[type];
        input.disabled = Boolean(preset);
        if (preset) input.value = preset;
        else if (Object.values(KW_ARG_PRESETS).includes(input.value.trim())) input.value = '';
        input.placeholder = preset || t('group.keywordArgValue');
    }

    function addKeywordArgRow(after) {
        const row = kwArgRow('', 'text');
        if (after) after.after(row);
        else $('kw-args-list').appendChild(row);
        row.querySelector('.kw-arg-value').focus();
    }

    function removeKeywordArgRow(row) {
        const list = $('kw-args-list');
        if (list.children.length <= 1) {
            applyKwArgType(row, 'text');
            const input = row.querySelector('.kw-arg-value');
            input.value = '';
            input.focus();
            return;
        }
        const index = Array.prototype.indexOf.call(list.children, row);
        row.remove();
        const next = list.children[Math.min(index, list.children.length - 1)];
        if (next) next.querySelector('.kw-arg-value').focus();
    }

    function collectKeywordArgs() {
        return Array.from(document.querySelectorAll('#kw-args-list .kw-arg-row'))
            .map(row => {
                const type = row.querySelector('.kw-arg-type').value;
                const value = row.querySelector('.kw-arg-value').value.trim();
                return value || KW_ARG_PRESETS[type] || '';
            })
            .filter(value => value !== '');
    }

    function editKeyword(rule) {
        editingKeyword = rule;
        $('kw-keyword').value = rule.keyword || '';
        $('kw-cmd').value = rule.cmd || '';
        renderKeywordArgs(rule.args || []);
        $('kw-save').textContent = t('group.keywordSave');
        $('kw-cancel').hidden = false;
        $('kw-keyword').focus();
    }

    function resetKeywordForm() {
        editingKeyword = null;
        $('kw-keyword').value = '';
        $('kw-cmd').value = '';
        renderKeywordArgs([]);
        $('kw-save').textContent = t('group.keywordAdd');
        $('kw-cancel').hidden = true;
        setStatus($('kw-status'), '');
    }

    async function saveKeyword() {
        if (!currentGroup) return;
        const keyword = $('kw-keyword').value.trim();
        const cmd = $('kw-cmd').value.trim();
        if (!keyword || !cmd) {
            setStatus($('kw-status'), t('group.keywordRequired'), 'error');
            return;
        }
        const body = {
            keyword,
            cmd,
            args: collectKeywordArgs()
        };
        const base = '/groups/' + encodeURIComponent(currentGroup.id) + '/keywords';
        try {
            if (editingKeyword) {
                await api(base + '/' + editingKeyword.id, {method: 'PUT', body: JSON.stringify(body)});
                toast(t('group.keywordUpdated'), 'ok');
            } else {
                await api(base, {method: 'POST', body: JSON.stringify(body)});
                toast(t('group.keywordAdded'), 'ok');
            }
            resetKeywordForm();
            loadKeywords();
        } catch (e) {
            setStatus($('kw-status'), e.message, 'error');
        }
    }

    async function toggleKeyword(rule) {
        if (!currentGroup) return;
        try {
            await api('/groups/' + encodeURIComponent(currentGroup.id) + '/keywords/'
                + rule.id + '/' + (rule.enabled ? 'disable' : 'enable'), {method: 'POST'});
            loadKeywords();
        } catch (e) {
            setStatus($('kw-status'), e.message, 'error');
        }
    }

    async function deleteKeyword(rule) {
        if (!currentGroup) return;
        if (!await confirmAction(t('group.keywordDeleteConfirm', {keyword: rule.keyword}))) return;
        try {
            await api('/groups/' + encodeURIComponent(currentGroup.id) + '/keywords/' + rule.id,
                {method: 'DELETE'});
            toast(t('group.keywordDeleted'), 'ok');
            loadKeywords();
        } catch (e) {
            setStatus($('kw-status'), e.message, 'error');
        }
    }

    // ═══ Quotes ══════════════════════

    const quoteState = {page: 1, size: PAGE_SIZE, keyword: '', items: []};

    async function loadQuotes() {
        const params = new URLSearchParams({
            page: String(quoteState.page),
            size: String(quoteState.size)
        });
        if (quoteState.keyword) params.set('keyword', quoteState.keyword);
        try {
            const data = await api('/quotes?' + params.toString());
            quoteState.items = data.items;
            renderQuotes(data);
        } catch (e) {
            $('quotes-body').innerHTML = emptyRow(6, t('common.loadError', {msg: e.message}));
            $('quotes-total').textContent = '';
            $('quotes-pager').innerHTML = '';
            toast(t('quotes.loadFailed', {msg: e.message}), 'error');
        }
    }

    function renderQuotes(data) {
        $('quotes-total').textContent = t('quotes.total', {n: data.total});
        const body = $('quotes-body');
        if (!data.items.length) {
            body.innerHTML = emptyRow(6, t('quotes.empty'));
        } else {
            body.innerHTML = data.items.map(quote => `
                <tr>
                    <td>${quote.id}</td>
                    <td>${escapeHtml(quote.nickname || '-')}</td>
                    <td class="nowrap"><code>${escapeHtml(quote.userId || '-')}</code></td>
                    <td class="quote-text">${escapeHtml(quote.text || '')}</td>
                    <td class="nowrap">${formatTime(quote.time)}</td>
                    <td class="actions"><button class="small danger btn-delete" data-delete-quote data-id="${quote.id}">${escapeHtml(t('common.delete'))}</button></td>
                </tr>`).join('');
        }
        renderPager($('quotes-pager'), data, page => {
            quoteState.page = page;
            loadQuotes();
        }, size => {
            quoteState.size = size;
            quoteState.page = 1;
            loadQuotes();
        });
    }

    async function deleteQuote(quote) {
        if (!await confirmAction(t('quotes.deleteConfirm', {id: quote.id}))) return;
        try {
            await api('/quotes/' + quote.id, {method: 'DELETE'});
            toast(t('quotes.deleted'), 'ok');
            if (quoteState.page > 1 && quoteState.items.length === 1) quoteState.page--;
            loadQuotes();
        } catch (e) {
            toast(t('quotes.deleteFailed', {msg: e.message}), 'error');
        }
    }

    // ═══ Plugins ═══════════════════

    let pluginItems = [];
    let pluginEnabled = true;
    let pluginBusy = false;

    async function loadPlugins() {
        const body = $('plugins-body');
        body.innerHTML = emptyRow(6, t('common.loading'));
        try {
            const data = await api('/plugins');
            pluginEnabled = data.enabled !== false;
            pluginItems = Array.isArray(data.items) ? data.items : [];
            renderPlugins();
        } catch (e) {
            body.innerHTML = emptyRow(6, t('common.loadError', {msg: e.message}));
            $('plugins-total').textContent = '';
            $('plugins-notice').hidden = true;
            $('plugins-reload-all').hidden = true;
            toast(t('plugins.loadFailed', {msg: e.message}), 'error');
        }
    }

    function renderPlugins() {
        $('plugins-total').textContent = t('plugins.total', {n: pluginItems.length});
        $('plugins-reload-all').hidden = !pluginEnabled;
        const notice = $('plugins-notice');
        notice.hidden = pluginEnabled;
        notice.textContent = pluginEnabled ? '' : t('plugins.globalDisabled');
        const body = $('plugins-body');
        if (!pluginItems.length) {
            body.innerHTML = emptyRow(6, t('plugins.empty'));
            return;
        }
        body.innerHTML = pluginItems.map(plugin => {
            const jar = escapeHtml(plugin.jar);
            const id = plugin.id
                ? `<code>${escapeHtml(plugin.id)}</code>`
                : '<span class="muted">-</span>';
            const versionText = plugin.version
                ? escapeHtml(plugin.version)
                : '<span class="muted">-</span>';
            const api = plugin.apiVersion == null
                ? ''
                : ` <span class="muted">api ${escapeHtml(plugin.apiVersion)}</span>`;
            const version = `<div class="plugin-version">${versionText}${api}</div>`;
            const commands = plugin.commands && plugin.commands.length
                ? `<div class="plugin-commands">${plugin.commands
                    .map(name => `<code>${escapeHtml(name)}</code>`).join('')}</div>`
                : '<span class="muted">-</span>';
            const error = plugin.error
                ? `<div class="plugin-error">${escapeHtml(plugin.error)}</div>`
                : '';
            return `
                <tr>
                    <td><code>${jar}</code></td>
                    <td>${id}</td>
                    <td>${version}</td>
                    <td>${commands}</td>
                    <td>${pluginBadge(plugin.state)}${error}</td>
                    <td class="actions">${pluginActions(plugin)}</td>
                </tr>`;
        }).join('');
    }

    function pluginBadge(state) {
        const key = 'plugins.state.' + state;
        const label = t(key);
        const kind = state === 'LOADED'
            ? 'badge ok'
            : state === 'FAILED' ? 'badge error' : 'badge off';
        return `<span class="${kind}">${escapeHtml(label === key ? state : label)}</span>`;
    }

    function pluginActions(plugin) {
        if (!pluginEnabled) return '<span class="muted">-</span>';
        const jar = escapeHtml(plugin.jar);
        if (plugin.state === 'LOADED') {
            return `<button class="small btn-reset" data-plugin-action="reload" data-jar="${jar}">${escapeHtml(t('plugins.reload'))}</button>
                    <button class="small btn-toggle btn-disable" data-plugin-action="disable" data-jar="${jar}">${escapeHtml(t('plugins.disable'))}</button>`;
        }
        if (plugin.state === 'DISABLED' || plugin.state === 'FAILED' || plugin.state === 'AVAILABLE') {
            const label = plugin.state === 'FAILED' ? t('plugins.retry') : t('plugins.enable');
            return `<button class="small btn-toggle btn-enable" data-plugin-action="enable" data-jar="${jar}">${escapeHtml(label)}</button>`;
        }
        return '<span class="muted">-</span>';
    }

    async function pluginAction(action, jar) {
        if (pluginBusy) return;
        if (action === 'disable' && !await confirmAction(t('plugins.disableConfirm', {jar}))) return;
        pluginBusy = true;
        try {
            await api('/plugins/' + encodeURIComponent(jar) + '/' + action, {method: 'POST'});
            const key = action === 'enable' ? 'plugins.enabled'
                : action === 'disable' ? 'plugins.disabled' : 'plugins.reloaded';
            toast(t(key, {jar}), 'ok');
        } catch (e) {
            toast(t('plugins.actionFailed', {msg: e.message}), 'error');
        } finally {
            pluginBusy = false;
        }
        await loadPlugins();
    }

    async function reloadAllPlugins() {
        if (pluginBusy) return;
        pluginBusy = true;
        const button = $('plugins-reload-all');
        button.disabled = true;
        try {
            await api('/plugins/reload', {method: 'POST'});
            toast(t('plugins.reloadedAll'), 'ok');
        } catch (e) {
            toast(t('plugins.actionFailed', {msg: e.message}), 'error');
        } finally {
            await loadPlugins();
            pluginBusy = false;
            button.disabled = false;
        }
    }

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

    let confirmResolve = null;
    let confirmTimer = 0;

    function finishConfirm(result) {
        if (!confirmResolve) return;
        const resolve = confirmResolve;
        confirmResolve = null;
        clearTimeout(confirmTimer);
        confirmTimer = 0;
        const dialog = $('confirm-dialog');
        dialog.classList.remove('confirm-dialog--run');
        $('confirm-bar').classList.remove('confirm-dialog__bar--run');
        if (dialog.open) dialog.close();
        resolve(result);
    }

    function confirmAction(message) {
        if (confirmResolve) finishConfirm(false);
        const dialog = $('confirm-dialog');
        const bar = $('confirm-bar');
        $('confirm-message').textContent = message;
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

    function populateSelects() {
        const upload = $('upload-type').value;
        const filter = $('file-filter-type').value;
        $('upload-type').innerHTML = bizOptions(upload || 0);
        $('file-filter-type').innerHTML =
            `<option value="">${escapeHtml(t('files.allTypes'))}</option>` + bizOptions(filter);
    }

    function bindDialogs() {
        document.querySelectorAll('dialog').forEach(dialog => {
            if (dialog.id === 'confirm-dialog') return;
            dialog.querySelectorAll('[data-close]').forEach(button =>
                button.addEventListener('click', () => dialog.close()));
            dialog.addEventListener('click', event => {
                if (event.target === dialog) dialog.close();
            });
            dialog.addEventListener('close', () => {
                if (dialog.id === 'file-dialog') editingFile = null;
                if (dialog.id === 'preview-dialog') {
                    releasePreview();
                    previewFile = null;
                }
                if (dialog.id === 'user-dialog') currentUser = null;
                if (dialog.id === 'group-dialog') {
                    currentGroup = null;
                    currentKeywords = [];
                    resetKeywordForm();
                }
            });
        });
        $('preview-download').addEventListener('click', () => {
            if (previewFile) downloadFile(previewFile);
        });
        $('preview-body').addEventListener('click', event => {
            if (event.target === event.currentTarget) $('preview-dialog').close();
        });
    }

    function bindEvents() {
        $('logout').onclick = () => {
            localStorage.removeItem(TOKEN_KEY);
            location.replace('login.html');
        };

        $('confirm-ok').onclick = () => finishConfirm(true);
        $('confirm-cancel').onclick = () => finishConfirm(false);
        $('confirm-dialog').addEventListener('click', event => {
            if (event.target === $('confirm-dialog')) finishConfirm(false);
        });
        $('confirm-dialog').addEventListener('cancel', event => {
            event.preventDefault();
            finishConfirm(false);
        });

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

        $('usage-refresh').onclick = () => {
            loadBot();
            loadUsage();
        };
        $('usage-days').onchange = () => {
            usageState.days = Number($('usage-days').value) || 14;
            loadUsage();
        };

        $('config-refresh').onclick = loadConfig;

        $('files-refresh').onclick = loadFiles;
        $('file-search').onclick = () => {
            fileState.keyword = $('file-keyword').value.trim();
            fileState.type = $('file-filter-type').value;
            fileState.page = 1;
            loadFiles();
        };
        $('file-reset').onclick = () => {
            fileState.keyword = '';
            fileState.type = '';
            fileState.page = 1;
            $('file-keyword').value = '';
            $('file-filter-type').value = '';
            loadFiles();
        };
        $('file-filter-type').onchange = () => {
            fileState.type = $('file-filter-type').value;
            fileState.page = 1;
            loadFiles();
        };
        $('file-keyword').addEventListener('keydown', event => {
            if (event.key === 'Enter') $('file-search').click();
        });
        $('upload-choose').onclick = () => $('upload-files').click();
        $('upload-files').addEventListener('change', updateUploadLabel);
        $('upload-button').onclick = uploadFiles;
        $('file-form').addEventListener('submit', saveFileInfo);

        $('files-body').addEventListener('click', event => {
            const button = event.target.closest('button[data-action]');
            if (!button) return;
            const file = fileState.items.find(item => item.id === Number(button.dataset.id));
            if (!file) return;
            if (button.dataset.action === 'edit') openFileDialog(file);
            else if (button.dataset.action === 'preview') openPreview(file);
            else if (button.dataset.action === 'download') downloadFile(file);
            else if (button.dataset.action === 'delete') deleteFile(file);
        });
        $('files-body').addEventListener('change', event => {
            const input = event.target.closest('input.file-select');
            if (!input) return;
            const id = Number(input.dataset.id);
            if (input.checked) fileSelection.add(id);
            else fileSelection.delete(id);
            updateFileSelection();
        });
        $('files-select-all').addEventListener('change', event => {
            const checked = event.target.checked;
            fileState.items.forEach(file => {
                if (checked) fileSelection.add(file.id);
                else fileSelection.delete(file.id);
            });
            document.querySelectorAll('#files-body input.file-select').forEach(input => {
                input.checked = checked;
            });
            updateFileSelection();
        });
        $('files-batch-delete').onclick = deleteSelectedFiles;

        $('accounts-refresh').onclick = loadAccounts;
        document.querySelectorAll('[data-tab]').forEach(button =>
            button.addEventListener('click', () => switchAccountsTab(button.dataset.tab)));
        $('user-search').onclick = () => {
            userState.keyword = $('user-keyword').value.trim();
            userState.page = 1;
            loadUsers();
        };
        $('user-reset').onclick = () => {
            userState.keyword = '';
            userState.page = 1;
            $('user-keyword').value = '';
            loadUsers();
        };
        $('user-keyword').addEventListener('keydown', event => {
            if (event.key === 'Enter') $('user-search').click();
        });
        $('group-search').onclick = () => {
            groupState.keyword = $('group-keyword').value.trim();
            groupState.page = 1;
            loadGroups();
        };
        $('group-reset').onclick = () => {
            groupState.keyword = '';
            groupState.page = 1;
            $('group-keyword').value = '';
            loadGroups();
        };
        $('group-keyword').addEventListener('keydown', event => {
            if (event.key === 'Enter') $('group-search').click();
        });

        $('users-body').addEventListener('click', event => {
            const save = event.target.closest('[data-save-user]');
            if (save) {
                saveUser(save);
                return;
            }
            const settings = event.target.closest('[data-user-settings]');
            if (settings) {
                const row = settings.closest('tr');
                const user = userState.items.find(item => item.id === row.dataset.id);
                if (user) openUserDialog(user);
                return;
            }
            const del = event.target.closest('[data-delete-user]');
            if (del) deleteUser(del);
        });
        $('groups-body').addEventListener('click', event => {
            const save = event.target.closest('[data-save-group]');
            if (save) {
                saveGroup(save);
                return;
            }
            const settings = event.target.closest('[data-group-settings]');
            if (settings) {
                const row = settings.closest('tr');
                const group = groupState.items.find(item => item.id === row.dataset.id);
                if (group) openGroupDialog(group);
                return;
            }
            const del = event.target.closest('[data-delete-group]');
            if (del) deleteGroup(del);
        });

        document.querySelectorAll('[data-group-tab]').forEach(button =>
            button.addEventListener('click', () => switchGroupTab(button.dataset.groupTab)));
        $('group-setting-form').addEventListener('submit', saveGroupSetting);
        $('user-setting-form').addEventListener('submit', saveUserSetting);
        $('kw-save').onclick = saveKeyword;
        $('kw-cancel').onclick = resetKeywordForm;
        $('kw-args-list').addEventListener('click', event => {
            const add = event.target.closest('.kw-arg-add');
            if (add) {
                addKeywordArgRow(add.closest('.kw-arg-row'));
                return;
            }
            const remove = event.target.closest('.kw-arg-remove');
            if (remove) removeKeywordArgRow(remove.closest('.kw-arg-row'));
        });
        $('kw-args-list').addEventListener('change', event => {
            const select = event.target.closest('.kw-arg-type');
            if (select) applyKwArgType(select.closest('.kw-arg-row'), select.value);
        });
        $('kw-body').addEventListener('click', event => {
            const button = event.target.closest('button[data-kw-action]');
            if (!button) return;
            const rule = currentKeywords.find(item => item.id === Number(button.dataset.id));
            if (!rule) return;
            if (button.dataset.kwAction === 'edit') editKeyword(rule);
            else if (button.dataset.kwAction === 'toggle') toggleKeyword(rule);
            else if (button.dataset.kwAction === 'delete') deleteKeyword(rule);
        });

        $('quotes-refresh').onclick = loadQuotes;
        $('quote-search').onclick = () => {
            quoteState.keyword = $('quote-keyword').value.trim();
            quoteState.page = 1;
            loadQuotes();
        };
        $('quote-reset').onclick = () => {
            quoteState.keyword = '';
            quoteState.page = 1;
            $('quote-keyword').value = '';
            loadQuotes();
        };
        $('quote-keyword').addEventListener('keydown', event => {
            if (event.key === 'Enter') $('quote-search').click();
        });
        $('quotes-body').addEventListener('click', event => {
            const button = event.target.closest('[data-delete-quote]');
            if (!button) return;
            const quote = quoteState.items.find(item => item.id === Number(button.dataset.id));
            if (quote) deleteQuote(quote);
        });

        $('plugins-refresh').onclick = loadPlugins;
        $('plugins-reload-all').onclick = reloadAllPlugins;
        $('plugins-body').addEventListener('click', event => {
            const button = event.target.closest('button[data-plugin-action]');
            if (button) pluginAction(button.dataset.pluginAction, button.dataset.jar);
        });
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
        if (typeof ResizeObserver === 'function') {
            new ResizeObserver(() => {
                if (!usageData) return;
                cancelAnimationFrame(usageResizeFrame);
                usageResizeFrame = requestAnimationFrame(() => {
                    const chart = $('usage-chart');
                    if (chart && chart.clientWidth > 0) chart.innerHTML = usageSvg(usageData.daily);
                });
            }).observe($('usage-chart'));
        }
    }

    window.addEventListener('hashchange', renderView);
    window.addEventListener('pageshow', () => {
        if (!localStorage.getItem(TOKEN_KEY)) location.replace('login.html');
    });

    document.addEventListener('console:localechange', () => {
        populateSelects();
        setStatus($('gs-status'), '');
        setStatus($('kw-status'), '');
        setStatus($('file-form-status'), '');
        $('upload-status').innerHTML = '';
        updateUploadLabel();
        if (currentGroup && $('group-dialog').open) {
            $('group-dialog-subtitle').textContent = t('group.id', {id: currentGroup.id});
            resetKeywordForm();
            renderKeywords();
        }
        renderView();
    });

    init();
})();
