import {$, t, api, escapeHtml, toast} from '../app-shared.js';

// ═══ Usage ═══════════════════════

const usageState = {days: 14};
let usageTab = 'commands';
let usageData = null;
let aiUsageData = null;
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
    if (usageTab === 'ai') return loadAiUsage();
    return loadCommandUsage();
}

async function loadCommandUsage() {
    const chart = $('usage-chart');
    chart.innerHTML = `<p class="empty">${escapeHtml(t('common.loading'))}</p>`;
    try {
        renderUsage(await api('/usage?days=' + usageState.days));
    } catch (e) {
        chart.innerHTML = `<p class="empty">${escapeHtml(t('usage.loadFailed', {msg: e.message}))}</p>`;
        toast(t('usage.loadFailed', {msg: e.message}), 'error');
    }
}

async function loadAiUsage() {
    const charts = [$('ai-usage-chart'), $('ai-token-chart')];
    charts.forEach(chart => {
        chart.innerHTML = `<p class="empty">${escapeHtml(t('common.loading'))}</p>`;
    });
    try {
        renderAiUsage(await api('/ai-usage?days=' + usageState.days));
    } catch (e) {
        const message = t('usage.aiLoadFailed', {msg: e.message});
        charts.forEach(chart => {
            chart.innerHTML = `<p class="empty">${escapeHtml(message)}</p>`;
        });
        toast(message, 'error');
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
    renderRanks($('user-ranks'), data.topUsers, t('usage.noUsers'),
        rank => rank.name || rank.key);
}

function renderAiUsage(data) {
    aiUsageData = data;
    $('ai-usage-total').textContent = formatNumber(data.total);
    $('ai-usage-users').textContent = formatNumber(data.activeUsers);
    $('ai-usage-groups').textContent = formatNumber(data.activeGroups);
    $('ai-usage-range').textContent = t('usage.days', {n: data.days});
    $('ai-usage-input').textContent = formatNumber(data.inputTokens);
    $('ai-usage-output').textContent = formatNumber(data.outputTokens);
    const first = data.daily.length ? data.daily[0].date : '-';
    const last = data.daily.length ? data.daily[data.daily.length - 1].date : '-';
    $('ai-usage-chart-note').textContent = t('usage.aiNote', {first, last});
    $('ai-token-chart-note').textContent = t('usage.aiTokenNote', {first, last});
    $('ai-usage-chart').innerHTML = aiCallsSvg(data.daily);
    $('ai-token-chart').innerHTML = aiTokenSvg(data.daily);
    renderRanks($('ai-trigger-ranks'), data.triggers, t('usage.noTriggers'),
        rank => aiTriggerLabel(rank.key), aiRankLabel);
    renderRanks($('ai-group-ranks'), data.groups, t('usage.noGroups'),
        rank => rank.name || rank.key, aiRankLabel);
    renderRanks($('ai-user-ranks'), data.topUsers, t('usage.noUsers'),
        rank => rank.name || rank.key, aiRankLabel);
}

function aiTriggerLabel(key) {
    const i18nKey = 'usage.ai.trigger.' + key;
    const label = t(i18nKey);
    return label === i18nKey ? (key || '-') : label;
}

function aiRankLabel(rank) {
    return t('usage.aiRank', {
        count: formatNumber(rank.count),
        input: formatNumber(rank.inputTokens),
        output: formatNumber(rank.outputTokens)
    });
}

function aiCallsSvg(days) {
    return chartSvg(days, {
        id: 'ai-usage-chart',
        value: day => day.count,
        label: t('usage.aiDaily'),
        title: (day, value) => `${day.date} · ${formatNumber(value)}`
    });
}

function aiTokenSvg(days) {
    return chartSvg(days, {
        id: 'ai-token-chart',
        axis: compactNumber,
        label: t('usage.aiTokenDaily'),
        series: [
            {className: 'input', value: day => day.inputTokens},
            {className: 'output', value: day => day.outputTokens}
        ],
        tooltip: day => t('usage.aiTokenTooltip', {
            date: day.date,
            input: formatNumber(day.inputTokens),
            output: formatNumber(day.outputTokens)
        })
    });
}

function formatNumber(value) {
    return Number(value || 0).toLocaleString();
}

function compactNumber(value) {
    const number = Number(value) || 0;
    if (number >= 1000000000) return `${(number / 1000000000).toFixed(1).replace(/\.0$/, '')}B`;
    if (number >= 1000000) return `${(number / 1000000).toFixed(1).replace(/\.0$/, '')}M`;
    if (number >= 1000) return `${(number / 1000).toFixed(1).replace(/\.0$/, '')}k`;
    return String(Math.round(number));
}

function usageSvg(days) {
    return chartSvg(days, {
        id: 'usage-chart',
        value: day => day.count,
        label: t('usage.daily'),
        title: (day, value) => `${day.date} · ${formatNumber(value)}`
    });
}

function chartSvg(days, options) {
    if (!days.length) return `<p class="empty">${escapeHtml(t('usage.empty'))}</p>`;
    const element = $(options.id);
    const width = Math.max(320, Math.round(element.clientWidth || 720));
    const height = 120;
    const left = 36;
    const right = 16;
    const top = 10;
    const bottom = 24;
    const series = options.series || [{className: '', value: options.value}];
    const seriesValues = series.map(item =>
        days.map(day => Number(item.value(day)) || 0));
    const max = Math.max(1, ...seriesValues.flat());
    const axis = options.axis || formatNumber;
    const fillId = 'usage-fill-' + options.id;
    const step = days.length > 1 ? (width - left - right) / (days.length - 1) : 0;
    const x = index => left + step * index;
    const y = value => top + (height - top - bottom) * (1 - (Number(value) || 0) / max);
    const paths = seriesValues.map(values => {
        const points = values.map((value, index) => ({x: x(index), y: y(value)}));
        return {points, line: smoothPath(points)};
    });
    const baseline = height - bottom;
    const area = series.length === 1 && days.length > 1
        ? `${paths[0].line} L ${paths[0].points[paths[0].points.length - 1].x.toFixed(1)} ${baseline}`
            + ` L ${paths[0].points[0].x.toFixed(1)} ${baseline} Z`
        : '';
    const pointTitle = (day, index) => {
        if (options.tooltip) return options.tooltip(day);
        if (options.title) return options.title(day, seriesValues[0][index]);
        return `${day.date} · ${seriesValues.map(values => formatNumber(values[index])).join(' / ')}`;
    };

    let grid = '';
    for (let i = 0; i <= 3; i++) {
        const lineY = top + (height - top - bottom) * i / 3;
        const value = Math.round(max * (1 - i / 3));
        grid += `<line x1="${left}" y1="${lineY}" x2="${width - right}" y2="${lineY}" class="grid"/>`
            + `<text x="${left - 8}" y="${lineY + 4}" class="axis" text-anchor="end">`
            + `${escapeHtml(axis(value))}</text>`;
    }

    const tick = Math.max(1, Math.ceil(days.length / 6));
    let labels = '';
    days.forEach((day, index) => {
        if (index % tick !== 0 && index !== days.length - 1) return;
        labels += `<text x="${x(index)}" y="${height - bottom + 16}" class="axis" text-anchor="middle">`
            + `${escapeHtml(String(day.date).slice(5))}</text>`;
    });

    const dots = days.map((day, index) => {
        const point = paths[0].points[index];
        const hitLeft = index === 0 ? left : point.x - step / 2;
        const hitRight = index === days.length - 1 ? width - right : point.x + step / 2;
        const guides = paths.map((path, seriesIndex) => {
            const item = path.points[index];
            const className = series[seriesIndex].className ? ' ' + series[seriesIndex].className : '';
            return `<line x1="${left}" y1="${item.y.toFixed(1)}"`
                + ` x2="${width - right}" y2="${item.y.toFixed(1)}" class="guide${className}"/>`;
        }).join('');
        const circles = paths.map((path, seriesIndex) => {
            const item = path.points[index];
            const className = series[seriesIndex].className ? ' ' + series[seriesIndex].className : '';
            return `<circle cx="${item.x.toFixed(1)}" cy="${item.y.toFixed(1)}"`
                + ` r="2.5" class="dot${className}"/>`;
        }).join('');
        return `<g class="point">`
            + `<rect x="${hitLeft.toFixed(1)}" y="${top}"`
            + ` width="${(hitRight - hitLeft).toFixed(1)}" height="${(baseline - top).toFixed(1)}" class="hit">`
            + `<title>${escapeHtml(pointTitle(day, index))}</title></rect>`
            + `<line x1="${point.x.toFixed(1)}" y1="${top}"`
            + ` x2="${point.x.toFixed(1)}" y2="${baseline}" class="guide"/>`
            + guides
            + circles
            + `</g>`;
    }).join('');

    const lines = paths.map((path, index) => {
        const className = series[index].className ? ' ' + series[index].className : '';
        return `<path d="${path.line}" class="line${className}"/>`;
    }).join('');

    return `<svg viewBox="0 0 ${width} ${height}" role="img" aria-label="${escapeHtml(options.label)}">
        <defs>
            <linearGradient id="${fillId}" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" class="fill-start"/>
                <stop offset="100%" class="fill-end"/>
            </linearGradient>
        </defs>
        ${grid}
        <path d="${area}" fill="url(#${fillId})"/>
        ${lines}
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

function renderRanks(element, ranks, empty, label, value) {
    if (!ranks || !ranks.length) {
        element.innerHTML = `<p class="empty">${escapeHtml(empty)}</p>`;
        return;
    }
    const max = Math.max(1, ...ranks.map(rank => Number(rank.count) || 0));
    const count = value || (rank => String(Number(rank.count) || 0));
    element.innerHTML = ranks.map((rank, index) => `
        <div class="rank-row">
            <span><span class="rank-index">${index + 1}</span>${escapeHtml(label(rank))}</span>
            <span class="rank-count">${escapeHtml(count(rank))}</span>
            <div class="rank-bar"><span style="width:${(Number(rank.count) || 0) / max * 100}%"></span></div>
        </div>`).join('');
}

function showUsageTab(tab) {
    usageTab = tab === 'ai' ? 'ai' : 'commands';
    document.querySelectorAll('[data-usage-tab]').forEach(button =>
        button.classList.toggle('active', button.dataset.usageTab === usageTab));
    $('usage-tab-commands').hidden = usageTab !== 'commands';
    $('usage-tab-ai').hidden = usageTab !== 'ai';
}

export function bind() {
    showUsageTab(usageTab);
    document.querySelectorAll('[data-usage-tab]').forEach(button => {
        button.onclick = () => {
            if (usageTab === button.dataset.usageTab) return;
            showUsageTab(button.dataset.usageTab);
            loadUsage();
        };
    });
    $('usage-refresh').onclick = () => {
        loadBot();
        loadUsage();
    };
    $('usage-days').onchange = () => {
        usageState.days = Number($('usage-days').value) || 14;
        loadUsage();
    };
}

export function observeChart() {
    if (typeof ResizeObserver !== 'function') return;
    const observer = new ResizeObserver(() => {
        if (!usageData && !aiUsageData) return;
        cancelAnimationFrame(usageResizeFrame);
        usageResizeFrame = requestAnimationFrame(() => {
            const chart = $('usage-chart');
            if (usageData && chart && chart.clientWidth > 0)
                chart.innerHTML = usageSvg(usageData.daily);
            if (!aiUsageData) return;
            const calls = $('ai-usage-chart');
            if (calls && calls.clientWidth > 0)
                calls.innerHTML = aiCallsSvg(aiUsageData.daily);
            const tokens = $('ai-token-chart');
            if (tokens && tokens.clientWidth > 0)
                tokens.innerHTML = aiTokenSvg(aiUsageData.daily);
        });
    });
    ['usage-chart', 'ai-usage-chart', 'ai-token-chart'].forEach(id => {
        const element = $(id);
        if (element) observer.observe(element);
    });
}

export {loadBot, loadUsage};
