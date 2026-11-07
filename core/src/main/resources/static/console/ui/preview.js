import {$, t, toast, bindBackdropClick} from './app-shared.js';
import {downloadIcon} from './icons.js';

// ═══ Preview ════════════════════

const UNPREVIEWABLE_AUDIO = new Set(['audio/silk', 'audio/x-silk', 'audio/amr']);
const MIN_SCALE = 1;
const MAX_SCALE = 5;

let previewUrl = null;
let previewDownload = null;
let previewImage = null;
let scale = 1;
let translateX = 0;
let translateY = 0;
let dragging = false;
let dragStartX = 0;
let dragStartY = 0;
let dragBaseX = 0;
let dragBaseY = 0;

function previewKind(contentType) {
    const type = (contentType || '').toLowerCase();
    if (type.startsWith('image/')) return 'image';
    if (type.startsWith('video/')) return 'video';
    if (type.startsWith('audio/')) return 'audio';
    return null;
}

function applyTransform() {
    if (!previewImage) return;
    previewImage.style.transform =
        `translate(${translateX}px, ${translateY}px) scale(${scale})`;
    previewImage.style.cursor = dragging ? 'grabbing' : scale > MIN_SCALE ? 'grab' : 'zoom-in';
}

function resetZoom() {
    scale = MIN_SCALE;
    translateX = 0;
    translateY = 0;
    dragging = false;
    applyTransform();
}

function clampTranslate(x, y) {
    if (!previewImage) return;
    const baseW = previewImage.offsetWidth;
    const baseH = previewImage.offsetHeight;
    const maxX = Math.max(0, (baseW * scale - baseW) / 2);
    const maxY = Math.max(0, (baseH * scale - baseH) / 2);
    translateX = Math.min(maxX, Math.max(-maxX, x));
    translateY = Math.min(maxY, Math.max(-maxY, y));
}

function onWheel(event) {
    if (!previewImage) return;
    event.preventDefault();
    const delta = event.deltaY > 0 ? -0.2 : 0.25;
    scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale + delta));
    if (scale === MIN_SCALE) {
        translateX = 0;
        translateY = 0;
    } else {
        clampTranslate(translateX, translateY);
    }
    applyTransform();
}

function onDragStart(event) {
    if (!previewImage || scale <= MIN_SCALE || event.button !== 0) return;
    event.preventDefault();
    dragging = true;
    dragStartX = event.clientX;
    dragStartY = event.clientY;
    dragBaseX = translateX;
    dragBaseY = translateY;
    applyTransform();
}

function onDragMove(event) {
    if (!dragging) return;
    clampTranslate(
        dragBaseX + event.clientX - dragStartX,
        dragBaseY + event.clientY - dragStartY
    );
    applyTransform();
}

function onDragEnd() {
    if (!dragging) return;
    dragging = false;
    applyTransform();
}

function releasePreview() {
    if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
        previewUrl = null;
    }
    previewImage = null;
    scale = MIN_SCALE;
    translateX = 0;
    translateY = 0;
    dragging = false;
    $('preview-body').innerHTML = '';
}

function showUnsupported(title, contentType, download) {
    releasePreview();
    $('preview-title').textContent = title;
    const wrap = document.createElement('div');
    wrap.className = 'preview-unsupported';
    const message = document.createElement('p');
    message.textContent = t('files.previewUnsupported', {type: contentType || '-'});
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'small';
    button.innerHTML = downloadIcon;
    button.append(t('common.download'));
    button.addEventListener('click', () => download());
    wrap.append(message, button);
    $('preview-body').appendChild(wrap);
    $('preview-dialog').showModal();
}

export function previewable(contentType) {
    return !!previewKind(contentType);
}

export async function openPreview(item) {
    const kind = previewKind(item.contentType);
    const type = (item.contentType || '').toLowerCase();
    const download = () => item.download?.();
    previewDownload = download;
    if (!kind || (kind === 'audio' && UNPREVIEWABLE_AUDIO.has(type))) {
        showUnsupported(item.title || '', item.contentType || '', download);
        return;
    }
    try {
        const blob = await item.load();
        releasePreview();
        previewUrl = URL.createObjectURL(blob);
        $('preview-title').textContent = item.title || '';
        const media = document.createElement(kind === 'image' ? 'img' : kind);
        media.className = 'preview-media';
        if (kind === 'image') {
            media.alt = item.title || '';
            media.draggable = false;
            media.style.touchAction = 'none';
            previewImage = media;
            media.addEventListener('pointerdown', onDragStart);
            media.addEventListener('dblclick', resetZoom);
        } else {
            media.controls = true;
            media.autoplay = true;
            if (kind === 'video') media.volume = 0.25;
        }
        media.src = previewUrl;
        $('preview-body').appendChild(media);
        if (kind === 'image') applyTransform();
        $('preview-dialog').showModal();
    } catch (e) {
        toast(t('files.previewFailed', {msg: e.message}), 'error');
    }
}

export function bindPreview() {
    $('preview-download').insertAdjacentHTML('afterbegin', downloadIcon);
    $('preview-download').addEventListener('click', () => {
        if (previewDownload) previewDownload();
    });
    window.addEventListener('wheel', onWheel, {passive: false});
    window.addEventListener('pointermove', onDragMove);
    window.addEventListener('pointerup', onDragEnd);
    window.addEventListener('pointercancel', onDragEnd);
    bindBackdropClick($('preview-body'), () => $('preview-dialog').close());
    $('preview-dialog').addEventListener('close', () => {
        releasePreview();
        previewDownload = null;
    });
}
