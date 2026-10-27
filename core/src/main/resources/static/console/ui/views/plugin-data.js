import {
    $, t, api, apiBlob, escapeHtml, formatBytes, formatTime,
    setStatus, emptyRow, confirmAction, toast
} from '../app-shared.js';

// ═══ Plugin data ═════════════════

const pluginDataState = {
    jar: '',
    id: '',
    loaded: false,
    tab: 'config',
    path: '',
    entries: [],
    selection: new Set(),
    busy: false,
    error: '',
    configBusy: false,
    configLoaded: false,
    configDirty: false,
    configExists: false,
    generation: 0
};

function openPluginData(plugin) {
    if (!plugin || !plugin.id) return;
    pluginDataState.generation++;
    pluginDataState.jar = plugin.jar;
    pluginDataState.id = plugin.id;
    pluginDataState.loaded = plugin.state === 'LOADED';
    pluginDataState.tab = 'config';
    pluginDataState.path = '';
    pluginDataState.entries = [];
    pluginDataState.selection.clear();
    pluginDataState.busy = false;
    pluginDataState.error = '';
    pluginDataState.configBusy = false;
    pluginDataState.configLoaded = false;
    pluginDataState.configDirty = false;
    pluginDataState.configExists = false;
    $('plugin-data-folder-form').hidden = true;
    $('plugin-data-folder-name').value = '';
    $('plugin-data-subtitle').textContent = t('plugins.dataSubtitle', {
        id: plugin.id,
        jar: plugin.jar
    });
    $('plugin-data-crumbs').innerHTML = '';
    $('plugin-data-count').textContent = '';
    $('plugin-data-body').innerHTML = '';
    $('plugin-config-content').value = '';
    setStatus($('plugin-data-status'), '');
    setStatus($('plugin-config-status'), '');
    updatePluginConfigToolbar();
    $('plugin-data-dialog').showModal();
    switchPluginDataTab('config', true);
}

function resetPluginData() {
    pluginDataState.generation++;
    pluginDataState.jar = '';
    pluginDataState.id = '';
    pluginDataState.loaded = false;
    pluginDataState.tab = 'config';
    pluginDataState.path = '';
    pluginDataState.entries = [];
    pluginDataState.selection.clear();
    pluginDataState.busy = false;
    pluginDataState.error = '';
    pluginDataState.configBusy = false;
    pluginDataState.configLoaded = false;
    pluginDataState.configDirty = false;
    pluginDataState.configExists = false;
    $('plugin-data-folder-form').hidden = true;
    $('plugin-data-folder-name').value = '';
    $('plugin-data-files').value = '';
    $('plugin-data-body').innerHTML = '';
    $('plugin-data-crumbs').innerHTML = '';
    $('plugin-data-count').textContent = '';
    $('plugin-config-content').value = '';
    setStatus($('plugin-data-status'), '');
    setStatus($('plugin-config-status'), '');
}

async function loadPluginData() {
    const jar = pluginDataState.jar;
    const path = pluginDataState.path;
    const generation = pluginDataState.generation;
    if (!jar) return;
    pluginDataState.busy = true;
    pluginDataState.error = '';
    updatePluginDataToolbar();
    $('plugin-data-body').innerHTML = emptyRow(6, t('common.loading'));
    setStatus($('plugin-data-status'), '');
    try {
        const params = new URLSearchParams();
        if (path) params.set('path', path);
        const query = params.toString();
        const data = await api('/plugins/' + encodeURIComponent(jar) + '/data'
            + (query ? '?' + query : ''));
        if (pluginDataState.generation !== generation
                || pluginDataState.jar !== jar || pluginDataState.path !== path) return;
        pluginDataState.entries = data && Array.isArray(data.entries) ? data.entries : [];
        pluginDataState.selection.clear();
        renderPluginData();
    } catch (e) {
        if (pluginDataState.generation !== generation
                || pluginDataState.jar !== jar || pluginDataState.path !== path) return;
        pluginDataState.entries = [];
        pluginDataState.selection.clear();
        pluginDataState.error = e.message;
        renderPluginData();
        toast(t('plugins.dataLoadFailed', {msg: e.message}), 'error');
    } finally {
        if (pluginDataState.generation === generation
                && pluginDataState.jar === jar && pluginDataState.path === path) {
            pluginDataState.busy = false;
            updatePluginDataToolbar();
        }
    }
}

async function switchPluginDataTab(tab, force = false) {
    if (!force && pluginDataState.tab === tab) return;
    if (!force && pluginDataState.tab === 'config' && tab !== 'config'
            && pluginDataState.configDirty) {
        if (!await confirmAction(t('plugins.configDiscardConfirm'))) return;
        pluginDataState.configLoaded = false;
        pluginDataState.configDirty = false;
    }
    pluginDataState.tab = tab;
    document.querySelectorAll('[data-plugin-data-tab]').forEach(button =>
        button.classList.toggle('active', button.dataset.pluginDataTab === tab));
    $('plugin-data-tab-config').hidden = tab !== 'config';
    $('plugin-data-tab-files').hidden = tab !== 'files';
    if (tab === 'config') loadPluginConfig();
    else loadPluginData();
}

async function loadPluginConfig(force = false) {
    if (!pluginDataState.jar || pluginDataState.configBusy) return;
    if (pluginDataState.configLoaded && !force) return;
    if (force && pluginDataState.configDirty
            && !await confirmAction(t('plugins.configDiscardConfirm'))) return;
    const jar = pluginDataState.jar;
    const generation = pluginDataState.generation;
    pluginDataState.configBusy = true;
    updatePluginConfigToolbar();
    setStatus($('plugin-config-status'), t('plugins.configLoading'));
    try {
        const view = await api('/plugins/' + encodeURIComponent(jar) + '/config');
        if (pluginDataState.generation !== generation || pluginDataState.jar !== jar) return;
        $('plugin-config-content').value = view && typeof view.content === 'string'
            ? view.content
            : '';
        pluginDataState.configLoaded = true;
        pluginDataState.configDirty = false;
        pluginDataState.configExists = !!(view && view.exists);
        setStatus($('plugin-config-status'), pluginDataState.configExists
            ? ''
            : t('plugins.configMissing'));
    } catch (e) {
        if (pluginDataState.generation !== generation || pluginDataState.jar !== jar) return;
        pluginDataState.configLoaded = false;
        setStatus($('plugin-config-status'),
            t('plugins.configLoadFailed', {msg: e.message}), 'error');
    } finally {
        if (pluginDataState.generation === generation && pluginDataState.jar === jar) {
            pluginDataState.configBusy = false;
            updatePluginConfigToolbar();
        }
    }
}

function updatePluginConfigToolbar() {
    const busy = pluginDataState.configBusy;
    $('plugin-config-refresh').disabled = busy;
    $('plugin-config-save').disabled = busy;
    $('plugin-config-content').disabled = busy;
    const reload = $('plugin-config-save-reload');
    reload.disabled = busy || !pluginDataState.loaded;
    reload.hidden = !pluginDataState.loaded;
}

async function savePluginConfig(reload) {
    if (!pluginDataState.jar || pluginDataState.configBusy) return;
    const jar = pluginDataState.jar;
    const generation = pluginDataState.generation;
    pluginDataState.configBusy = true;
    updatePluginConfigToolbar();
    setStatus($('plugin-config-status'), t('plugins.configSaving'));
    try {
        const content = $('plugin-config-content').value;
        const view = await api('/plugins/' + encodeURIComponent(jar) + '/config', {
            method: 'PUT',
            body: JSON.stringify({content})
        });
        if (pluginDataState.generation !== generation || pluginDataState.jar !== jar) return;
        $('plugin-config-content').value = view && typeof view.content === 'string'
            ? view.content
            : content;
        pluginDataState.configLoaded = true;
        pluginDataState.configDirty = false;
        pluginDataState.configExists = true;
        if (!reload || !pluginDataState.loaded) {
            setStatus($('plugin-config-status'), t('plugins.configSaved'), 'ok');
            return;
        }
        setStatus($('plugin-config-status'), t('plugins.configReloading'));
        try {
            await api('/plugins/' + encodeURIComponent(jar) + '/reload', {method: 'POST'});
            if (pluginDataState.generation === generation && pluginDataState.jar === jar)
                setStatus($('plugin-config-status'), t('plugins.configSavedReloaded'), 'ok');
        } catch (e) {
            if (pluginDataState.generation === generation && pluginDataState.jar === jar)
                setStatus($('plugin-config-status'),
                    t('plugins.configSavedReloadFailed', {msg: e.message}), 'error');
        }
    } catch (e) {
        if (pluginDataState.generation === generation && pluginDataState.jar === jar)
            setStatus($('plugin-config-status'), e.message, 'error');
    } finally {
        if (pluginDataState.generation === generation && pluginDataState.jar === jar) {
            pluginDataState.configBusy = false;
            updatePluginConfigToolbar();
        }
    }
}

function renderPluginData() {
    const state = pluginDataState;
    $('plugin-data-subtitle').textContent = t('plugins.dataSubtitle', {
        id: state.id,
        jar: state.jar
    });
    renderPluginDataCrumbs();
    $('plugin-data-count').textContent = t('plugins.dataCount', {n: state.entries.length});
    const body = $('plugin-data-body');
    if (state.error) {
        body.innerHTML = emptyRow(6, t('plugins.dataLoadFailed', {msg: state.error}));
    } else if (!state.entries.length) {
        body.innerHTML = emptyRow(6, t('plugins.dataEmpty'));
    } else {
        body.innerHTML = state.entries
            .map((entry, index) => pluginDataRow(entry, index))
            .join('');
    }
    updatePluginDataToolbar();
}

function renderPluginDataCrumbs() {
    const nav = $('plugin-data-crumbs');
    nav.innerHTML = '';
    const parts = pluginDataState.path ? pluginDataState.path.split('/') : [];
    const paths = [''];
    let current = '';
    parts.forEach(part => {
        current = current ? current + '/' + part : part;
        paths.push(current);
    });
    paths.forEach((path, index) => {
        if (index) {
            const sep = document.createElement('span');
            sep.className = 'plugin-data-sep';
            sep.textContent = '/';
            nav.appendChild(sep);
        }
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'plugin-data-crumb';
        button.textContent = index === 0 ? t('plugins.dataRoot') : parts[index - 1];
        button.addEventListener('click', () => navigatePluginData(path));
        nav.appendChild(button);
    });
}

function pluginDataRow(entry, index) {
    const name = escapeHtml(entry.name);
    const title = escapeHtml(entry.name);
    const selected = pluginDataState.selection.has(entry.name);
    const isDir = entry.type === 'dir';
    const nameCell = isDir
        ? `<button type="button" class="file-name file-name--link"
                   data-action="open" data-index="${index}" title="${title}"
                   aria-label="${escapeHtml(t('plugins.dataOpen'))}: ${title}">${name}</button>`
        : `<span class="file-name" title="${title}">${name}</span>`;
    const actions = [];
    if (!isDir) {
        actions.push(`<button type="button" class="small btn-download" data-action="download"
                   data-index="${index}">${escapeHtml(t('common.download'))}</button>`);
    }
    actions.push(`<button type="button" class="small danger btn-delete" data-action="delete"
                   data-index="${index}">${escapeHtml(t('common.delete'))}</button>`);
    return `
        <tr>
            <td class="check-col"><input type="checkbox" class="plugin-data-select"
                    data-index="${index}" aria-label="${escapeHtml(t('files.select'))}"
                    ${selected ? 'checked' : ''}></td>
            <td>${nameCell}</td>
            <td class="nowrap">${escapeHtml(isDir ? t('plugins.dataTypeDir') : t('plugins.dataTypeFile'))}</td>
            <td class="nowrap">${isDir ? '-' : formatBytes(entry.size)}</td>
            <td class="nowrap">${escapeHtml(formatTime(entry.modified))}</td>
            <td class="actions">${actions.join(' ')}</td>
        </tr>`;
}

function updatePluginDataToolbar() {
    const state = pluginDataState;
    const busy = state.busy;
    const total = state.entries.length;
    const selected = state.selection.size;
    $('plugin-data-up').disabled = busy || !state.path;
    $('plugin-data-refresh').disabled = busy;
    $('plugin-data-new-folder').disabled = busy;
    $('plugin-data-upload').disabled = busy;
    $('plugin-data-delete').disabled = busy || selected === 0;
    $('plugin-data-delete').textContent = selected
        ? t('plugins.dataDeleteCount', {n: selected})
        : t('plugins.dataDelete');
    const selectAll = $('plugin-data-select-all');
    const selectedOnPage = state.entries.filter(entry => state.selection.has(entry.name)).length;
    selectAll.disabled = busy || total === 0;
    selectAll.checked = total > 0 && selectedOnPage === total;
    selectAll.indeterminate = selectedOnPage > 0 && selectedOnPage < total;
    document.querySelectorAll('#plugin-data-body input.plugin-data-select').forEach(input => {
        input.disabled = busy;
    });
}

function navigatePluginData(path) {
    if (pluginDataState.busy) return;
    pluginDataState.path = path || '';
    pluginDataState.selection.clear();
    $('plugin-data-folder-form').hidden = true;
    $('plugin-data-folder-name').value = '';
    loadPluginData();
}

function pluginDataParentPath() {
    const parts = pluginDataState.path ? pluginDataState.path.split('/') : [];
    parts.pop();
    return parts.join('/');
}

async function createPluginDataFolder(event) {
    event.preventDefault();
    if (pluginDataState.busy) return;
    const name = $('plugin-data-folder-name').value.trim();
    if (!name) {
        setStatus($('plugin-data-status'), t('plugins.dataFolderRequired'), 'error');
        return;
    }
    const jar = pluginDataState.jar;
    const generation = pluginDataState.generation;
    pluginDataState.busy = true;
    updatePluginDataToolbar();
    try {
        await api('/plugins/' + encodeURIComponent(jar) + '/data/folders', {
            method: 'POST',
            body: JSON.stringify({path: pluginDataState.path, name})
        });
        if (pluginDataState.generation !== generation || pluginDataState.jar !== jar) return;
        $('plugin-data-folder-form').hidden = true;
        $('plugin-data-folder-name').value = '';
        toast(t('plugins.dataFolderCreated', {name}), 'ok');
        await loadPluginData();
    } catch (e) {
        if (pluginDataState.generation === generation && pluginDataState.jar === jar)
            setStatus($('plugin-data-status'), e.message, 'error');
    } finally {
        if (pluginDataState.generation === generation && pluginDataState.jar === jar) {
            pluginDataState.busy = false;
            updatePluginDataToolbar();
        }
    }
}

async function uploadPluginData() {
    const input = $('plugin-data-files');
    const files = Array.from(input.files || []);
    if (!files.length || pluginDataState.busy) {
        input.value = '';
        return;
    }
    const existing = new Set(pluginDataState.entries
        .filter(entry => entry.type === 'file')
        .map(entry => entry.name.toLowerCase()));
    const collisions = files.filter(file => existing.has(file.name.toLowerCase()));
    if (collisions.length) {
        const names = collisions.slice(0, 3).map(file => file.name).join(', ');
        const more = collisions.length > 3 ? ` +${collisions.length - 3}` : '';
        const confirmed = await confirmAction(t('plugins.dataOverwriteConfirm', {
            n: collisions.length,
            names: names + more
        }));
        if (!confirmed) {
            input.value = '';
            return;
        }
    }
    const jar = pluginDataState.jar;
    const path = pluginDataState.path;
    const generation = pluginDataState.generation;
    pluginDataState.busy = true;
    updatePluginDataToolbar();
    setStatus($('plugin-data-status'), t('plugins.dataUploading', {n: files.length}));
    try {
        const form = new FormData();
        files.forEach(file => form.append('files', file));
        const params = new URLSearchParams();
        if (path) params.set('path', path);
        const query = params.toString();
        const result = await api('/plugins/' + encodeURIComponent(jar) + '/data/files'
            + (query ? '?' + query : ''), {method: 'POST', body: form});
        const items = result && Array.isArray(result.items) ? result.items : [];
        const ok = items.filter(item => item.ok).length;
        const fail = items.length - ok;
        toast(t('plugins.dataUploadDone', {ok, fail}), fail ? 'error' : 'ok');
        if (pluginDataState.generation === generation && pluginDataState.jar === jar) {
            await loadPluginData();
            if (fail) setStatus($('plugin-data-status'), pluginDataErrorSummary(items), 'error');
        }
    } catch (e) {
        if (pluginDataState.generation === generation && pluginDataState.jar === jar)
            setStatus($('plugin-data-status'), e.message, 'error');
        toast(t('plugins.dataUploadDone', {ok: 0, fail: files.length}), 'error');
    } finally {
        input.value = '';
        if (pluginDataState.generation === generation && pluginDataState.jar === jar) {
            pluginDataState.busy = false;
            updatePluginDataToolbar();
        }
    }
}

async function deletePluginDataSelection() {
    if (pluginDataState.busy || pluginDataState.selection.size === 0) return;
    const names = Array.from(pluginDataState.selection);
    if (!await confirmAction(t('plugins.dataDeleteConfirm', {n: names.length}))) return;
    const jar = pluginDataState.jar;
    const generation = pluginDataState.generation;
    pluginDataState.busy = true;
    updatePluginDataToolbar();
    try {
        const result = await api('/plugins/' + encodeURIComponent(jar) + '/data/delete', {
            method: 'POST',
            body: JSON.stringify({path: pluginDataState.path, names})
        });
        const items = result && Array.isArray(result.items) ? result.items : [];
        const ok = items.filter(item => item.ok).length;
        const fail = items.length - ok;
        toast(t('plugins.dataDeleteDone', {ok, fail}), fail ? 'error' : 'ok');
        if (pluginDataState.generation === generation && pluginDataState.jar === jar) {
            pluginDataState.selection.clear();
            await loadPluginData();
            if (fail) setStatus($('plugin-data-status'), pluginDataErrorSummary(items), 'error');
        }
    } catch (e) {
        if (pluginDataState.generation === generation && pluginDataState.jar === jar)
            setStatus($('plugin-data-status'), e.message, 'error');
        toast(t('plugins.dataDeleteDone', {ok: 0, fail: names.length}), 'error');
    } finally {
        if (pluginDataState.generation === generation && pluginDataState.jar === jar) {
            pluginDataState.busy = false;
            updatePluginDataToolbar();
        }
    }
}

async function deletePluginDataEntry(entry) {
    if (!entry || pluginDataState.busy) return;
    if (!await confirmAction(t('plugins.dataDeleteOneConfirm', {name: entry.name}))) return;
    const jar = pluginDataState.jar;
    const path = pluginDataState.path;
    const generation = pluginDataState.generation;
    pluginDataState.busy = true;
    updatePluginDataToolbar();
    try {
        const result = await api('/plugins/' + encodeURIComponent(jar) + '/data/delete', {
            method: 'POST',
            body: JSON.stringify({path, names: [entry.name]})
        });
        if (pluginDataState.generation !== generation || pluginDataState.jar !== jar) return;
        const item = result && Array.isArray(result.items) ? result.items[0] : null;
        if (!item || !item.ok) {
            const message = item && item.error
                ? item.error
                : t('plugins.dataDeleteDone', {ok: 0, fail: 1});
            toast(t('plugins.dataDeleteFailed', {msg: message}), 'error');
            return;
        }
        pluginDataState.selection.delete(entry.name);
        toast(t('plugins.dataDeleted', {name: entry.name}), 'ok');
        await loadPluginData();
    } catch (e) {
        if (pluginDataState.generation === generation && pluginDataState.jar === jar)
            toast(t('plugins.dataDeleteFailed', {msg: e.message}), 'error');
    } finally {
        if (pluginDataState.generation === generation && pluginDataState.jar === jar) {
            pluginDataState.busy = false;
            updatePluginDataToolbar();
        }
    }
}

async function downloadPluginData(entry) {
    if (!entry || entry.type === 'dir') return;
    const jar = pluginDataState.jar;
    const path = pluginDataState.path ? pluginDataState.path + '/' + entry.name : entry.name;
    try {
        const blob = await apiBlob('/plugins/' + encodeURIComponent(jar)
            + '/data/download?path=' + encodeURIComponent(path));
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = entry.name;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 10000);
    } catch (e) {
        toast(t('plugins.dataDownloadFailed', {msg: e.message}), 'error');
    }
}

function pluginDataErrorSummary(items) {
    const failed = items.filter(item => !item.ok);
    const parts = failed.slice(0, 3).map(item => item.error
        ? item.name + ': ' + item.error
        : item.name);
    const more = failed.length > 3 ? ` (+${failed.length - 3})` : '';
    return t('plugins.dataItemErrors', {errors: parts.join('; ') + more});
}

export function bind() {
    document.querySelectorAll('[data-plugin-data-tab]').forEach(button =>
        button.addEventListener('click', () =>
            switchPluginDataTab(button.dataset.pluginDataTab)));
    $('plugin-config-refresh').onclick = () => loadPluginConfig(true);
    $('plugin-config-save').onclick = () => savePluginConfig(false);
    $('plugin-config-save-reload').onclick = () => savePluginConfig(true);
    $('plugin-config-content').addEventListener('input', () => {
        pluginDataState.configDirty = true;
    });
    $('plugin-data-up').onclick = () => navigatePluginData(pluginDataParentPath());
    $('plugin-data-refresh').onclick = () => loadPluginData();
    $('plugin-data-new-folder').onclick = () => {
        if (pluginDataState.busy) return;
        $('plugin-data-folder-form').hidden = false;
        $('plugin-data-folder-name').focus();
    };
    $('plugin-data-folder-cancel').onclick = () => {
        $('plugin-data-folder-form').hidden = true;
        $('plugin-data-folder-name').value = '';
        setStatus($('plugin-data-status'), '');
    };
    $('plugin-data-folder-form').addEventListener('submit', createPluginDataFolder);
    $('plugin-data-upload').onclick = () => {
        if (!pluginDataState.busy) $('plugin-data-files').click();
    };
    $('plugin-data-files').addEventListener('change', uploadPluginData);
    $('plugin-data-delete').onclick = deletePluginDataSelection;
    $('plugin-data-select-all').addEventListener('change', event => {
        const checked = event.target.checked;
        pluginDataState.entries.forEach(entry => {
            if (checked) pluginDataState.selection.add(entry.name);
            else pluginDataState.selection.delete(entry.name);
        });
        document.querySelectorAll('#plugin-data-body input.plugin-data-select').forEach(input => {
            input.checked = checked;
        });
        updatePluginDataToolbar();
    });
    $('plugin-data-body').addEventListener('click', event => {
        if (pluginDataState.busy) return;
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const entry = pluginDataState.entries[Number(button.dataset.index)];
        if (!entry) return;
        if (button.dataset.action === 'open') {
            navigatePluginData(pluginDataState.path
                ? pluginDataState.path + '/' + entry.name
                : entry.name);
        } else if (button.dataset.action === 'download') {
            downloadPluginData(entry);
        } else if (button.dataset.action === 'delete') {
            deletePluginDataEntry(entry);
        }
    });
    $('plugin-data-body').addEventListener('change', event => {
        const input = event.target.closest('input.plugin-data-select');
        if (!input) return;
        const entry = pluginDataState.entries[Number(input.dataset.index)];
        if (!entry) return;
        if (input.checked) pluginDataState.selection.add(entry.name);
        else pluginDataState.selection.delete(entry.name);
        updatePluginDataToolbar();
    });
    $('plugin-data-dialog').addEventListener('close', () => resetPluginData());
}

export function onLocaleChange() {
    if (pluginDataState.jar && $('plugin-data-dialog').open) renderPluginData();
}

export {openPluginData};
