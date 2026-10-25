import {$, t, api, escapeHtml, emptyRow, toast, confirmAction} from '../app-shared.js';
import {openPluginData} from './plugin-data.js';

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
        $('plugins-upload').hidden = true;
        $('plugins-reload-all').hidden = true;
        toast(t('plugins.loadFailed', {msg: e.message}), 'error');
    }
}

function renderPlugins() {
    $('plugins-total').textContent = t('plugins.total', {n: pluginItems.length});
    $('plugins-upload').hidden = !pluginEnabled;
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
                <td class="col-center">${version}</td>
                <td>${commands}</td>
                <td class="col-center">${pluginBadge(plugin.state)}${error}</td>
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
    const jar = escapeHtml(plugin.jar);
    const data = plugin.id
        ? `<button class="small btn-data" data-plugin-action="data" data-jar="${jar}">${escapeHtml(t('plugins.data'))}</button>`
        : '';
    if (!pluginEnabled) return data || '<span class="muted">-</span>';
    const remove = `<button class="small danger btn-delete" data-plugin-action="delete" data-jar="${jar}">${escapeHtml(t('plugins.delete'))}</button>`;
    const tail = data
        ? ` <span class="actions-divider" aria-hidden="true"></span> ${data}`
        : '';
    if (plugin.state === 'LOADED') {
        return `<button class="small btn-reset" data-plugin-action="reload" data-jar="${jar}">${escapeHtml(t('plugins.reload'))}</button>
                <button class="small btn-toggle btn-disable" data-plugin-action="disable" data-jar="${jar}">${escapeHtml(t('plugins.disable'))}</button>
                ${remove}${tail}`;
    }
    if (plugin.state === 'DISABLED' || plugin.state === 'FAILED' || plugin.state === 'AVAILABLE') {
        const label = plugin.state === 'FAILED' ? t('plugins.retry') : t('plugins.enable');
        return `<button class="small btn-toggle btn-enable" data-plugin-action="enable" data-jar="${jar}">${escapeHtml(label)}</button>
                ${remove}${tail}`;
    }
    return data || '<span class="muted">-</span>';
}

async function pluginAction(action, jar) {
    if (action === 'data') {
        const plugin = pluginItems.find(item => item.jar === jar);
        if (plugin) openPluginData(plugin);
        return;
    }
    if (pluginBusy) return;
    if (action === 'disable' && !await confirmAction(t('plugins.disableConfirm', {jar}))) return;
    if (action === 'delete' && !await confirmAction(t('plugins.deleteConfirm', {jar}))) return;
    pluginBusy = true;
    try {
        if (action === 'delete') {
            await api('/plugins/' + encodeURIComponent(jar), {method: 'DELETE'});
        } else {
            await api('/plugins/' + encodeURIComponent(jar) + '/' + action, {method: 'POST'});
        }
        const key = action === 'enable' ? 'plugins.enabled'
            : action === 'disable' ? 'plugins.disabled'
            : action === 'delete' ? 'plugins.deleted' : 'plugins.reloaded';
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

async function uploadPlugin() {
    const input = $('plugins-upload-file');
    const file = input.files && input.files[0];
    if (!file) return;
    if (!pluginEnabled || pluginBusy) {
        input.value = '';
        return;
    }
    if (!file.name.endsWith('.jar')) {
        input.value = '';
        toast(t('plugins.uploadInvalid'), 'error');
        return;
    }
    pluginBusy = true;
    const button = $('plugins-upload');
    button.disabled = true;
    try {
        const form = new FormData();
        form.append('file', file);
        const data = await api('/plugins', {method: 'POST', body: form});
        if (data && data.state === 'FAILED')
            toast(t('plugins.uploadLoadFailed', {msg: data.error || t('plugins.state.FAILED')}), 'error');
        else
            toast(t('plugins.uploaded', {jar: (data && data.jar) || file.name}), 'ok');
    } catch (e) {
        toast(t('plugins.uploadFailed', {msg: e.message}), 'error');
    } finally {
        input.value = '';
        await loadPlugins();
        pluginBusy = false;
        button.disabled = false;
    }
}

export function bind() {
    $('plugins-refresh').onclick = loadPlugins;
    $('plugins-upload').onclick = () => {
        if (!pluginBusy) $('plugins-upload-file').click();
    };
    $('plugins-upload-file').addEventListener('change', uploadPlugin);
    $('plugins-reload-all').onclick = reloadAllPlugins;
    $('plugins-body').addEventListener('click', event => {
        const button = event.target.closest('button[data-plugin-action]');
        if (button) pluginAction(button.dataset.pluginAction, button.dataset.jar);
    });
}

export {loadPlugins};
