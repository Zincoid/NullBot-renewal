const core = window.ConsoleUtils;

export const {
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
    clearToken,
    requireToken
} = core;

export const {t} = window.ConsoleUI;

export const PAGE_SIZE = 20;
export const UPLOAD_LINE_TTL = 4000;
export const BIZ_TYPES = [0, 1, 2, 3, 4];
export const ACCESS_LEVELS = [-2, -1, 0, 1, 2];

export function options(items, selected) {
    const selectedValue = selected == null ? '' : String(selected);
    return items.map(item => {
        const value = typeof item === 'string' ? item : item.value;
        const label = typeof item === 'string' ? item : item.label;
        return `<option value="${escapeHtml(value)}"${String(value) === selectedValue ? ' selected' : ''}>${escapeHtml(label)}</option>`;
    }).join('');
}

export function setRowStatus(element, text, kind = '', autoHide = false) {
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

export function bizLabel(value) {
    const key = 'files.type' + value;
    const label = t(key);
    return label === key ? value : label;
}

export function bizOptions(selected) {
    return options(BIZ_TYPES.map(value => ({value, label: bizLabel(value)})), selected);
}

export function accessOptions(selected) {
    return options(ACCESS_LEVELS.map(value => ({value, label: t('access.' + value)})), selected);
}
