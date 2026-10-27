import {$, t, getToken, clearToken} from './app-shared.js';

const MAX_ENTRIES = 500;
const RECONNECT_DELAY = 3000;
const STREAM_IDLE_TIMEOUT = 45000;
const FRAME_BOUNDARY = /\r?\n\r?\n/;
const LEVELS = new Set(['trace', 'debug', 'info', 'warn', 'error']);

let bound = false;
let open = false;
let active = false;
let state = 'disconnected';
let controller = null;
let reconnectTimer = 0;
let autoScroll = true;

let panel;
let toggleButton;
let body;
let list;
let empty;
let clearButton;
let status;

export function bind() {
    if (bound) return;
    panel = $('log-panel');
    toggleButton = $('log-toggle');
    if (!panel || !toggleButton) return;
    body = $('log-body');
    list = $('log-list');
    empty = $('log-empty');
    clearButton = $('log-clear');
    status = $('log-status');

    toggleButton.addEventListener('click', () => open ? close() : show());
    clearButton.addEventListener('click', clear);
    body.addEventListener('scroll', () => {
        autoScroll = body.scrollTop + body.clientHeight >= body.scrollHeight - 24;
    });

    setState('disconnected');
    updateEmpty();
    bound = true;
    startStream();
}

export function onLocaleChange() {
    if (bound) setState(state);
}

function show() {
    open = true;
    panel.hidden = false;
    toggleButton.classList.add('is-active');
    toggleButton.setAttribute('aria-expanded', 'true');
    if (autoScroll) body.scrollTop = body.scrollHeight;
    startStream();
}

function close() {
    open = false;
    panel.hidden = true;
    toggleButton.classList.remove('is-active');
    toggleButton.setAttribute('aria-expanded', 'false');
}

function startStream() {
    if (active) return;
    const token = getToken();
    if (!token) {
        location.replace('login.html');
        return;
    }

    clearReconnect();
    const current = new AbortController();
    controller = current;
    active = true;
    setState('connecting');

    let idleTimer = 0;
    const clearIdle = () => {
        if (!idleTimer) return;
        clearTimeout(idleTimer);
        idleTimer = 0;
    };
    const touch = () => {
        if (controller !== current) return;
        clearIdle();
        idleTimer = setTimeout(() => {
            idleTimer = 0;
            if (controller !== current) return;
            setState('disconnected');
            current.abort();
        }, STREAM_IDLE_TIMEOUT);
    };
    touch();

    (async () => {
        try {
            const response = await fetch('/console/logs/stream', {
                headers: {token, Accept: 'text/event-stream'},
                cache: 'no-store',
                signal: current.signal
            });
            if (response.status === 401) {
                clearToken();
                location.replace('login.html');
                return;
            }
            if (!response.ok || !response.body)
                throw new Error('HTTP ' + response.status);
            if (controller !== current) return;

            setState('connected');
            touch();
            await read(response.body, touch);
        } catch {
            // A closed or stalled stream is retried below.
        } finally {
            clearIdle();
            if (controller !== current) return;
            controller = null;
            active = false;
            setState('disconnected');
            scheduleReconnect();
        }
    })();
}

async function read(stream, onActivity) {
    const reader = stream.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    for (;;) {
        const {value, done} = await reader.read();
        if (done) break;
        onActivity();
        buffer += decoder.decode(value, {stream: true});
        let match;
        while ((match = FRAME_BOUNDARY.exec(buffer))) {
            const frame = buffer.slice(0, match.index);
            buffer = buffer.slice(match.index + match[0].length);
            handleFrame(frame);
        }
    }
    buffer += decoder.decode();
    if (buffer.trim()) handleFrame(buffer);
}

function handleFrame(frame) {
    const data = frame.split(/\r?\n/)
        .filter(line => line.startsWith('data:'))
        .map(line => line.slice(5).replace(/^ /, ''))
        .join('\n');
    if (!data) return;
    try {
        append(JSON.parse(data));
    } catch {
        // The initial "connected" event is not a log entry.
    }
}

function append(entry) {
    if (!entry || typeof entry !== 'object') return;

    const line = document.createElement('div');
    line.className = 'log-line log-line--' + levelOf(entry.level);

    const time = document.createElement('span');
    time.className = 'log-line__time';
    time.textContent = timeOf(entry.timestamp);
    time.title = entry.timestamp || '';

    const level = document.createElement('span');
    level.className = 'log-line__level';
    level.textContent = entry.level || '';

    const logger = document.createElement('span');
    logger.className = 'log-line__logger';
    logger.textContent = entry.logger || '';
    logger.title = entry.thread ? `${entry.logger || ''} · ${entry.thread}` : (entry.logger || '');

    const message = document.createElement('span');
    message.className = 'log-line__message';
    message.textContent = entry.message || '';

    line.append(time, level, logger, message);
    if (entry.stackTrace) {
        const stack = document.createElement('pre');
        stack.className = 'log-line__stack';
        stack.textContent = entry.stackTrace;
        line.append(stack);
    }

    list.append(line);
    while (list.childElementCount > MAX_ENTRIES) list.firstElementChild.remove();
    updateEmpty();
    if (autoScroll) body.scrollTop = body.scrollHeight;
}

function scheduleReconnect() {
    if (reconnectTimer) return;
    reconnectTimer = setTimeout(() => {
        reconnectTimer = 0;
        startStream();
    }, RECONNECT_DELAY);
}

function clearReconnect() {
    if (!reconnectTimer) return;
    clearTimeout(reconnectTimer);
    reconnectTimer = 0;
}

function clear() {
    list.replaceChildren();
    updateEmpty();
}

function updateEmpty() {
    const hasEntries = list.childElementCount > 0;
    empty.hidden = hasEntries;
    clearButton.disabled = !hasEntries;
}

function setState(next) {
    state = next;
    panel.dataset.state = next;
    status.textContent = t('logs.' + next);
}

function levelOf(level) {
    const value = String(level || '').toLowerCase();
    return LEVELS.has(value) ? value : 'info';
}

function timeOf(timestamp) {
    const value = String(timestamp || '');
    const space = value.indexOf(' ');
    return space === -1 ? value : value.slice(space + 1);
}
