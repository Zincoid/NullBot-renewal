import {
    $, t, api, apiBlob, escapeHtml, formatBytes, formatTime, setStatus,
    emptyRow, renderPager, confirmAction, toast, bindBackdropClick,
    PAGE_SIZE, UPLOAD_LINE_TTL, bizLabel, bizOptions
} from '../app-shared.js';

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

export function populateSelects() {
    const upload = $('upload-type').value;
    const filter = $('file-filter-type').value;
    $('upload-type').innerHTML = bizOptions(upload || 0);
    $('file-filter-type').innerHTML =
        `<option value="">${escapeHtml(t('files.allTypes'))}</option>` + bizOptions(filter);
}

export function bind() {
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

    $('preview-download').addEventListener('click', () => {
        if (previewFile) downloadFile(previewFile);
    });
    bindBackdropClick($('preview-body'), () => $('preview-dialog').close());
    $('file-dialog').addEventListener('close', () => {
        editingFile = null;
    });
    $('preview-dialog').addEventListener('close', () => {
        releasePreview();
        previewFile = null;
    });
}

export function onLocaleChange() {
    populateSelects();
    setStatus($('file-form-status'), '');
    $('upload-status').innerHTML = '';
    updateUploadLabel();
}

export {loadFiles, updateUploadLabel};
