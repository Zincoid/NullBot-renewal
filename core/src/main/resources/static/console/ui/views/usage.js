import {$, t, api, escapeHtml, toast} from '../app-shared.js';

// ═══ Usage ═══════════════════════

const usageState = {days: 14};
let usageTab = 'totals';
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
    if (usageTab === 'totals') return loadTotals();
    return loadCommandUsage();
}

async function loadCommandUsage() {
    hideChartTooltip();
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
    hideChartTooltip();
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

async function loadTotals() {
    try {
        const [cmd, ai] = await Promise.all([
            api('/usage/total'),
            api('/ai-usage/total')
        ]);
        $('total-cmd').textContent = formatNumber(cmd.total);
        $('total-ai').textContent = formatNumber(ai.total);
        $('total-input').textContent = formatNumber(ai.inputTokens);
        $('total-output').textContent = formatNumber(ai.outputTokens);
    } catch (e) {
        toast(t('usage.loadFailed', {msg: e.message}), 'error');
    }
}

function renderUsage(data) {
    hideChartTooltip();
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
    hideChartTooltip();
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
    return aiRankCalls(rank.count)
        + aiRankTokens('input', rank.inputTokens)
        + aiRankTokens('output', rank.outputTokens);
}

function aiRankCalls(value) {
    const label = t('usage.aiCallsShort');
    const text = compactNumber(value);
    return `<span class="rank-token ico-total" title="${escapeHtml(`${label} ${text}`)}">`
        + `${escapeHtml(text)}</span>`;
}

function aiRankTokens(kind, value) {
    const label = t(kind === 'input' ? 'usage.aiInputShort' : 'usage.aiOutputShort');
    const text = compactNumber(value);
    return `<span class="rank-token ico-${kind}" title="${escapeHtml(`${label} ${text}`)}">`
        + `${escapeHtml(text)}</span>`;
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
    return barChartSvg(days, {
        id: 'ai-token-chart',
        axis: compactNumber,
        label: t('usage.aiTokenDaily'),
        series: [
            {className: 'output', value: day => day.outputTokens},
            {className: 'input', value: day => day.inputTokens}
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

function axisLeft(labels, width, min = 36) {
    const chars = Math.max(1, ...labels.map(label => String(label).length));
    return Math.min(Math.max(min, chars * 7.5 + 12), Math.round(width * 0.3));
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
    let left = 36;
    const right = 16;
    const top = 10;
    const bottom = 24;
    const series = options.series || [{className: '', value: options.value}];
    const seriesValues = series.map(item =>
        days.map(day => Number(item.value(day)) || 0));
    const max = Math.max(1, ...seriesValues.flat());
    const axis = options.axis || formatNumber;
    const ticks = [0, 1, 2, 3].map(i => Math.round(max * (1 - i / 3)));
    left = axisLeft(ticks.map(axis), width);
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
        grid += `<line x1="${left}" y1="${lineY}" x2="${width - right}" y2="${lineY}" class="grid"/>`
            + `<text x="${left - 8}" y="${lineY + 4}" class="axis" text-anchor="end">`
            + `${escapeHtml(axis(ticks[i]))}</text>`;
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
            + ` width="${(hitRight - hitLeft).toFixed(1)}" height="${(baseline - top).toFixed(1)}"`
            + ` class="hit" data-tip="${escapeHtml(pointTitle(day, index))}"></rect>`
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

function barChartSvg(days, options) {
    if (!days.length) return `<p class="empty">${escapeHtml(t('usage.empty'))}</p>`;
    const element = $(options.id);
    const width = Math.max(320, Math.round(element.clientWidth || 720));
    const height = 150;
    let left = 40;
    const right = 16;
    const top = 12;
    const bottom = 24;
    const plotHeight = height - top - bottom;
    const baseline = height - bottom;
    const series = options.series || [{className: '', value: options.value}];
    const totals = days.map(day =>
        series.reduce((sum, item) => sum + (Number(item.value(day)) || 0), 0));
    const max = Math.max(1, ...totals);
    const axis = options.axis || formatNumber;
    const ticks = [0, 1, 2, 3].map(i => Math.round(max * (1 - i / 3)));
    left = axisLeft(ticks.map(axis), width, 40);
    const band = (width - left - right) / days.length;
    const x = index => left + band * (index + 0.5);

    let grid = '';
    for (let i = 0; i <= 3; i++) {
        const lineY = top + plotHeight * i / 3;
        grid += `<line x1="${left}" y1="${lineY.toFixed(1)}" x2="${width - right}" y2="${lineY.toFixed(1)}" class="grid"/>`
            + `<text x="${left - 8}" y="${lineY + 4}" class="axis" text-anchor="end">`
            + `${escapeHtml(axis(ticks[i]))}</text>`;
    }

    const tick = Math.max(1, Math.ceil(days.length / 6));
    let labels = '';
    days.forEach((day, index) => {
        if (index % tick !== 0 && index !== days.length - 1) return;
        labels += `<text x="${x(index).toFixed(1)}" y="${height - bottom + 16}" class="axis" text-anchor="middle">`
            + `${escapeHtml(String(day.date).slice(5))}</text>`;
    });

    const bars = days.map((day, index) => {
        const x0 = left + band * index;
        const barWidth = index === days.length - 1 ? band : band + 0.5;
        let segmentBottom = baseline;
        const segments = series.map((item, seriesIndex) => {
            const value = Number(item.value(day)) || 0;
            if (value <= 0) return '';
            const segmentHeight = plotHeight * value / max;
            const segmentTop = segmentBottom - segmentHeight;
            const overlap = seriesIndex > 0 && segmentBottom < baseline ? 0.5 : 0;
            const className = item.className ? ' ' + item.className : '';
            const rect = `<rect x="${x0.toFixed(1)}" y="${segmentTop.toFixed(1)}"`
                + ` width="${barWidth.toFixed(1)}" height="${(segmentHeight + overlap).toFixed(1)}"`
                + ` class="bar${className}"/>`;
            segmentBottom = segmentTop;
            return rect;
        }).join('');
        const tooltip = options.tooltip
            ? options.tooltip(day)
            : `${day.date} · ${formatNumber(totals[index])}`;
        return `<g class="bar-point">`
            + segments
            + `<rect x="${x0.toFixed(1)}" y="${top}" width="${band.toFixed(1)}"`
            + ` height="${plotHeight}" class="hit" data-tip="${escapeHtml(tooltip)}"></rect>`
            + `</g>`;
    }).join('');

    return `<svg viewBox="0 0 ${width} ${height}" role="img" aria-label="${escapeHtml(options.label)}">
        ${grid}
        ${bars}
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
    const count = value || (rank => escapeHtml(String(Number(rank.count) || 0)));
    element.innerHTML = ranks.map((rank, index) => `
        <div class="rank-row">
            <span><span class="rank-index">${index + 1}</span>${escapeHtml(label(rank))}</span>
            <span class="rank-count">${count(rank)}</span>
            <div class="rank-bar"><span style="width:${(Number(rank.count) || 0) / max * 100}%"></span></div>
        </div>`).join('');
}

function chartHit(target) {
    return target instanceof Element
        ? target.closest('.chart .hit[data-tip]')
        : null;
}

function placeChartTooltip(event) {
    const tip = $('chart-tooltip');
    const gap = 14;
    const edge = 8;
    const rect = tip.getBoundingClientRect();
    let left = event.clientX + gap;
    let top = event.clientY + gap;
    if (left + rect.width > window.innerWidth - edge)
        left = event.clientX - rect.width - gap;
    if (top + rect.height > window.innerHeight - edge)
        top = event.clientY - rect.height - gap;
    tip.style.left = Math.max(edge, left) + 'px';
    tip.style.top = Math.max(edge, top) + 'px';
}

function showChartTooltip(hit, event) {
    const tip = $('chart-tooltip');
    tip.textContent = hit.dataset.tip || '';
    tip.hidden = false;
    placeChartTooltip(event);
}

function hideChartTooltip() {
    $('chart-tooltip').hidden = true;
}

function bindChartTooltip() {
    document.addEventListener('pointerover', event => {
        const hit = chartHit(event.target);
        if (hit) showChartTooltip(hit, event);
    });
    document.addEventListener('pointermove', event => {
        const hit = chartHit(event.target);
        if (!hit) {
            hideChartTooltip();
            return;
        }
        showChartTooltip(hit, event);
    });
    document.addEventListener('pointerout', event => {
        const hit = chartHit(event.target);
        if (!hit) return;
        if (chartHit(event.relatedTarget)) return;
        hideChartTooltip();
    });
    window.addEventListener('scroll', hideChartTooltip, true);
    window.addEventListener('resize', hideChartTooltip);
}

function showUsageTab(tab) {
    usageTab = tab === 'ai' || tab === 'totals' ? tab : 'commands';
    document.querySelectorAll('[data-usage-tab]').forEach(button =>
        button.classList.toggle('active', button.dataset.usageTab === usageTab));
    $('usage-tab-commands').hidden = usageTab !== 'commands';
    $('usage-tab-ai').hidden = usageTab !== 'ai';
    $('usage-tab-totals').hidden = usageTab !== 'totals';
    $('usage-days-field').hidden = usageTab === 'totals';
}

export function bind() {
    bindChartTooltip();
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
            hideChartTooltip();
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
