import {
    $, t, api, escapeHtml, setStatus, setRowStatus, confirmAction
} from '../app-shared.js';

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

export function bind() {
    $('config-refresh').onclick = loadConfig;
}

export {loadConfig};
