import {
    $, t, api, apiBlob, escapeHtml, formatBytes, formatTime, setStatus, toast,
    confirmAction
} from '../app-shared.js';
import {openPreview} from '../preview.js';
import {downloadIcon, paperclipIcon, previewIcon, sendIcon} from '../icons.js';

// ═══ Messages ════════════════════

const state = {
    chats: [],
    chatId: null,
    chatKind: 'group',
    chatKindChosen: false,
    messages: [],
    chatsLoading: false,
    chatsError: '',
    messagesLoading: false,
    messagesError: '',
    sending: false,
    resetting: false
};
let chatsSeq = 0;
let messagesSeq = 0;

const attachmentState = {
    messageId: null,
    items: [],
    urls: [],
    seq: 0
};

function stamp(value) {
    if (!value) return '-';
    return String(value).replace('T', ' ').slice(0, 19);
}

function kindKey(type) {
    return type === 'group' || type === 'private' ? type : 'unknown';
}

function kindLabel(type) {
    return t('messages.kind.' + kindKey(type));
}

function roleLabel(role) {
    return t('messages.role.' + (role === 'assistant' ? 'assistant' : 'user'));
}

function currentChat() {
    return state.chats.find(chat => chat.chatId === state.chatId) || null;
}

function chatsOf(kind) {
    return state.chats.filter(chat => kindKey(chat.type) === kind);
}

function visibleChats() {
    return chatsOf(state.chatKind);
}

function ensureChatSelection() {
    const chats = visibleChats();
    if (chats.some(chat => chat.chatId === state.chatId)) return;
    state.chatId = chats.length ? chats[0].chatId : null;
}

function emptyState(text, retry = false) {
    const button = retry
        ? ` <button type="button" class="small" data-action="retry">${escapeHtml(t('common.refresh'))}</button>`
        : '';
    return `<div class="messages-state">${escapeHtml(text)}${button}</div>`;
}

// ═══ Render ══════════════════════

function renderChats() {
    const list = $('messages-chat-list');
    const switcher = $('messages-chat-switch');
    $('messages-group-count').textContent = chatsOf('group').length;
    $('messages-private-count').textContent = chatsOf('private').length;
    switcher.dataset.active = state.chatKind;
    switcher.querySelectorAll('button[data-chat-kind]').forEach(button => {
        const active = button.dataset.chatKind === state.chatKind;
        button.classList.toggle('active', active);
        button.setAttribute('aria-pressed', String(active));
    });
    if (state.chatsLoading) {
        list.innerHTML = emptyState(t('common.loading'));
        return;
    }
    if (state.chatsError) {
        list.innerHTML = emptyState(state.chatsError);
        return;
    }
    const chats = visibleChats();
    if (!chats.length) {
        list.innerHTML = emptyState(t(state.chatKind === 'private'
            ? 'messages.emptyPrivateChats'
            : 'messages.emptyGroupChats'));
        return;
    }
    list.innerHTML = chats.map(chat => {
        const active = chat.chatId === state.chatId;
        const name = chat.name || chat.chatId;
        const secondary = name === chat.chatId ? '' : chat.chatId;
        const count = t('messages.messagesCount', {n: chat.messages});
        return `
            <button type="button" class="messages-chat${active ? ' active' : ''}"
                    data-chat-id="${escapeHtml(chat.chatId)}" aria-pressed="${active}">
                <span class="messages-chat__main">
                    <span class="messages-chat__name">${escapeHtml(name)}</span>
                    ${secondary ? `<span class="messages-chat__id">${escapeHtml(secondary)}</span>` : ''}
                </span>
                <span class="messages-chat__side">
                    <span class="messages-chat__time">${escapeHtml(formatTime(chat.lastAt))}</span>
                    <span class="messages-chat__count" title="${escapeHtml(count)}">${escapeHtml(String(chat.messages))}</span>
                </span>
            </button>`;
    }).join('');
}

function resizeInput() {
    const input = $('messages-input');
    input.style.height = 'auto';
    input.style.height = Math.min(Math.max(input.scrollHeight, 40), 140) + 'px';
}

function renderComposer() {
    const form = $('messages-compose');
    const input = $('messages-input');
    const button = $('messages-send');
    const hasChat = !!currentChat();
    const busy = state.sending || state.resetting;
    form.hidden = !hasChat;
    input.disabled = busy;
    button.disabled = busy || !hasChat || !input.value.trim();
    $('messages-send-label').textContent =
        state.sending ? t('messages.sending') : t('messages.send');
    $('messages-reset').disabled = busy || !hasChat;
}

function renderHead() {
    const chat = currentChat();
    const head = $('messages-head');
    if (!chat) {
        head.hidden = true;
        renderComposer();
        return;
    }
    head.hidden = false;
    $('messages-chat-name').textContent = chat.name || chat.chatId;
    const chatIdElement = $('messages-chat-id');
    chatIdElement.textContent = chat.chatId;
    chatIdElement.title = chat.chatId;
    $('messages-chat-meta').textContent =
        t('messages.messagesCount', {n: chat.messages}) + ' · ' + formatTime(chat.lastAt);
    const kind = $('messages-chat-kind');
    kind.textContent = kindLabel(chat.type);
    kind.className = 'messages-kind messages-kind--' + kindKey(chat.type);
    renderComposer();
}

function renderMessage(message) {
    const assistant = message.role === 'assistant';
    const nickname = message.nickname || message.userId || '-';
    const count = Number(message.attachmentCount) || 0;
    const reasoning = message.reasoning && message.reasoning.trim()
        ? `<details class="messages-message__reasoning">
               <summary>${escapeHtml(t('messages.reasoning'))}</summary>
               <div class="messages-message__reasoning-body">${escapeHtml(message.reasoning)}</div>
           </details>`
        : '';
    const attachments = count > 0
        ? `<button type="button" class="messages-message__attachments"
                   data-action="attachments" data-message-id="${escapeHtml(message.id)}"
                   title="${escapeHtml(t('messages.attachmentsCount', {n: count}))}"
                   aria-label="${escapeHtml(t('messages.viewAttachments'))}">
               ${paperclipIcon}
               <span>${escapeHtml(String(count))}</span>
           </button>`
        : '';
    return `
        <article class="messages-message messages-message--${assistant ? 'assistant' : 'user'}">
            <div class="messages-message__meta">
                <span class="messages-message__role">${escapeHtml(roleLabel(message.role))}</span>
                <span class="messages-message__speaker" title="${escapeHtml(nickname)}">${escapeHtml(nickname)}</span>
                ${message.userId ? `<code class="messages-message__user" title="${escapeHtml(message.userId)}">${escapeHtml(message.userId)}</code>` : ''}
                <time class="messages-message__time">${escapeHtml(stamp(message.createdAt))}</time>
            </div>
            <div class="messages-message__row">
                <div class="messages-message__bubble"
                     title="${escapeHtml(t('messages.messageId', {id: message.id}))}">
                    <div class="messages-message__content">${escapeHtml(message.content || '')}</div>
                    ${reasoning}
                </div>
                ${attachments}
            </div>
        </article>`;
}

function renderMessages() {
    const timeline = $('messages-timeline');
    if (!state.chatId) {
        timeline.innerHTML = emptyState(t('messages.selectChat'));
        return;
    }
    if (state.messagesLoading) {
        timeline.innerHTML = emptyState(t('common.loading'));
        return;
    }
    if (state.messagesError) {
        timeline.innerHTML = emptyState(state.messagesError, true);
        return;
    }
    if (!state.messages.length) {
        timeline.innerHTML = emptyState(t('messages.emptyMessages'));
        return;
    }
    timeline.innerHTML = state.messages.map(renderMessage).join('');
    timeline.scrollTop = timeline.scrollHeight;
}

// ═══ Load ════════════════════════

async function loadChatMessages(chatId, seq = ++messagesSeq) {
    state.messagesLoading = true;
    state.messagesError = '';
    state.messages = [];
    renderMessages();
    try {
        const params = new URLSearchParams({chatId});
        const messages = await api('/messages?' + params.toString());
        if (seq !== messagesSeq) return;
        state.messages = Array.isArray(messages) ? messages : [];
        state.messagesLoading = false;
        renderMessages();
    } catch (e) {
        if (seq !== messagesSeq) return;
        state.messagesLoading = false;
        state.messagesError = t('messages.loadMessagesFailed', {msg: e.message});
        renderMessages();
        toast(state.messagesError, 'error');
    }
}

function selectChat(chatId) {
    if (!chatId || chatId === state.chatId) return;
    state.chatId = chatId;
    state.messages = [];
    state.messagesError = '';
    renderChats();
    renderHead();
    loadChatMessages(chatId);
}

function switchChatKind(kind) {
    if (kind !== 'group' && kind !== 'private') return;
    if (kind === state.chatKind) return;
    state.chatKind = kind;
    state.chatKindChosen = true;
    ensureChatSelection();
    renderChats();
    renderHead();
    if (!state.chatId) {
        state.messages = [];
        state.messagesError = '';
        renderMessages();
        return;
    }
    loadChatMessages(state.chatId);
}

export async function loadMessages() {
    const seq = ++chatsSeq;
    state.chatsLoading = true;
    state.chatsError = '';
    renderChats();
    try {
        const chats = await api('/messages/chats');
        if (seq !== chatsSeq) return;
        state.chats = Array.isArray(chats) ? chats : [];
        state.chatsLoading = false;
        if (!state.chatKindChosen) {
            state.chatKindChosen = true;
            if (!chatsOf('group').length && chatsOf('private').length)
                state.chatKind = 'private';
        }
        ensureChatSelection();
        renderChats();
        renderHead();
        if (state.chatId) {
            await loadChatMessages(state.chatId);
        } else {
            state.messages = [];
            renderMessages();
        }
    } catch (e) {
        if (seq !== chatsSeq) return;
        state.chatsLoading = false;
        state.chatsError = t('messages.loadChatsFailed', {msg: e.message});
        state.chats = [];
        state.chatId = null;
        state.messages = [];
        renderChats();
        renderHead();
        renderMessages();
        toast(state.chatsError, 'error');
    }
}

// ═══ Reset ═══════════════════════

async function resetChat() {
    if (!state.chatId || state.sending || state.resetting) return;
    const chatId = state.chatId;
    const chat = currentChat();
    const name = chat ? (chat.name || chat.chatId) : chatId;
    if (!await confirmAction(t('messages.resetConfirm', {name}))) return;
    state.resetting = true;
    renderComposer();
    try {
        await api('/messages?' + new URLSearchParams({chatId}), {method: 'DELETE'});
        toast(t('messages.resetDone'), 'ok');
        if (chatId === state.chatId) state.chatId = null;
        await loadMessages();
    } catch (e) {
        toast(t('messages.resetFailed', {msg: e.message}), 'error');
    } finally {
        state.resetting = false;
        renderComposer();
    }
}

// ═══ Send ════════════════════════

async function sendMessage(event) {
    event.preventDefault();
    if (state.sending || !state.chatId) return;
    const input = $('messages-input');
    const content = input.value.trim();
    if (!content) {
        input.focus();
        return;
    }
    const chatId = state.chatId;
    state.sending = true;
    renderComposer();
    try {
        const result = await api('/messages', {
            method: 'POST',
            body: JSON.stringify({chatId, content})
        });
        const stored = result?.stored !== false;
        toast(t(stored ? 'messages.sent' : 'messages.sentNotStored'), stored ? 'ok' : 'error');
        input.value = '';
        resizeInput();
        if (chatId === state.chatId) await loadMessages();
    } catch (e) {
        toast(t('messages.sendFailed', {msg: e.message}), 'error');
    } finally {
        state.sending = false;
        renderComposer();
        if (chatId === state.chatId) input.focus();
    }
}

// ═══ Attachments ═════════════════

function attachmentCard(id) {
    return document.querySelector(`#message-attachments-list [data-attachment-id="${id}"]`);
}

function releaseAttachmentUrls() {
    attachmentState.urls.forEach(url => URL.revokeObjectURL(url));
    attachmentState.urls = [];
}

function resetAttachments() {
    attachmentState.seq++;
    attachmentState.messageId = null;
    attachmentState.items = [];
    releaseAttachmentUrls();
    $('message-attachments-list').innerHTML = '';
    $('message-attachments-subtitle').textContent = '';
    setStatus($('message-attachments-status'), '');
}

function markAttachmentPreview(item) {
    const card = attachmentCard(item.id);
    if (!card) return;
    const preview = card.querySelector('[data-role="preview"]');
    if (!preview) return;
    preview.textContent = '';
    const label = document.createElement('span');
    label.className = 'message-attachment__type';
    label.textContent = item.contentType || '-';
    preview.appendChild(label);
}

async function loadAttachmentPreview(item, seq) {
    const messageId = attachmentState.messageId;
    if (!messageId) return;
    const card = attachmentCard(item.id);
    if (!card) return;
    try {
        const blob = await apiBlob('/messages/' + encodeURIComponent(messageId)
            + '/attachments/' + item.id + '/content');
        if (seq !== attachmentState.seq) return;
        const url = URL.createObjectURL(blob);
        const preview = card.querySelector('[data-role="preview"]');
        if (!preview) {
            URL.revokeObjectURL(url);
            return;
        }
        attachmentState.urls.push(url);
        preview.textContent = '';
        const image = document.createElement('img');
        image.className = 'message-attachment__image';
        image.alt = item.original || '';
        image.loading = 'lazy';
        image.src = url;
        image.addEventListener('error', () => {
            preview.textContent = '';
            const label = document.createElement('span');
            label.className = 'muted';
            label.textContent = t('messages.attachmentLoadFailed');
            preview.appendChild(label);
        }, {once: true});
        preview.appendChild(image);
    } catch {
        if (seq !== attachmentState.seq) return;
        const preview = card.querySelector('[data-role="preview"]');
        if (!preview) return;
        preview.textContent = '';
        const label = document.createElement('span');
        label.className = 'muted';
        label.textContent = t('messages.attachmentLoadFailed');
        preview.appendChild(label);
    }
}

function renderAttachments(seq) {
    const list = $('message-attachments-list');
    list.innerHTML = attachmentState.items.map(item => `
        <figure class="message-attachment" data-attachment-id="${item.id}">
            <div class="message-attachment__preview" data-role="preview">
                <span class="muted">${escapeHtml(t('common.loading'))}</span>
            </div>
            <figcaption class="message-attachment__info">
                <span class="message-attachment__name" title="${escapeHtml(item.original || '')}">${escapeHtml(item.original || '-')}</span>
                <span class="muted">${escapeHtml(formatBytes(item.size))} · ${escapeHtml(item.contentType || '-')}</span>
                <span class="muted">${escapeHtml(formatTime(item.createdAt))}</span>
                <div class="message-attachment__actions">
                    <button type="button" class="small" data-action="preview-attachment"
                            data-id="${item.id}">${previewIcon}<span>${escapeHtml(t('files.preview'))}</span></button>
                    <button type="button" class="small" data-action="download-attachment"
                            data-id="${item.id}">${downloadIcon}<span>${escapeHtml(t('common.download'))}</span></button>
                </div>
            </figcaption>
        </figure>`).join('');
    attachmentState.items.forEach(item => {
        if ((item.contentType || '').toLowerCase().startsWith('image/'))
            loadAttachmentPreview(item, seq);
        else
            markAttachmentPreview(item);
    });
}

async function openAttachments(message) {
    const seq = ++attachmentState.seq;
    attachmentState.messageId = message.id;
    attachmentState.items = [];
    releaseAttachmentUrls();
    const count = Number(message.attachmentCount) || 0;
    $('message-attachments-list').innerHTML = '';
    $('message-attachments-subtitle').textContent =
        t('messages.attachmentsSubtitle', {id: message.id, n: count});
    setStatus($('message-attachments-status'), t('messages.attachmentsLoading'));
    const dialog = $('message-attachments-dialog');
    if (!dialog.open) dialog.showModal();
    try {
        const items = await api('/messages/' + encodeURIComponent(message.id) + '/attachments');
        if (seq !== attachmentState.seq) return;
        attachmentState.items = Array.isArray(items) ? items : [];
        if (!attachmentState.items.length) {
            setStatus($('message-attachments-status'), t('messages.attachmentsEmpty'));
            return;
        }
        setStatus($('message-attachments-status'), '');
        renderAttachments(seq);
    } catch (e) {
        if (seq !== attachmentState.seq) return;
        setStatus($('message-attachments-status'),
            t('messages.attachmentsLoadFailed', {msg: e.message}), 'error');
    }
}

async function downloadAttachment(item) {
    const messageId = attachmentState.messageId;
    if (!messageId) return;
    try {
        const blob = await apiBlob('/messages/' + encodeURIComponent(messageId)
            + '/attachments/' + item.id + '/content');
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = item.original || ('attachment-' + item.id);
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 10000);
    } catch (e) {
        toast(t('messages.attachmentDownloadFailed', {msg: e.message}), 'error');
    }
}

function openAttachmentPreview(item) {
    const messageId = attachmentState.messageId;
    if (!messageId) return;
    openPreview({
        title: item.original || ('attachment-' + item.id),
        contentType: item.contentType,
        load: () => apiBlob('/messages/' + encodeURIComponent(messageId)
            + '/attachments/' + item.id + '/content'),
        download: () => downloadAttachment(item)
    });
}

// ═══ Binding ═════════════════════

export function bind() {
    const compose = $('messages-compose');
    $('messages-send').insertAdjacentHTML('afterbegin', sendIcon);
    $('messages-refresh').onclick = loadMessages;
    $('messages-reset').onclick = resetChat;
    compose.addEventListener('submit', sendMessage);
    $('messages-input').addEventListener('input', () => {
        resizeInput();
        renderComposer();
    });
    $('messages-input').addEventListener('keydown', event => {
        if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            if (!$('messages-send').disabled) compose.requestSubmit();
        }
    });
    $('messages-chat-list').addEventListener('click', event => {
        const button = event.target.closest('button[data-chat-id]');
        if (button) selectChat(button.dataset.chatId);
    });
    $('messages-chat-switch').addEventListener('click', event => {
        const button = event.target.closest('button[data-chat-kind]');
        if (button) switchChatKind(button.dataset.chatKind);
    });
    $('messages-timeline').addEventListener('click', event => {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        if (button.dataset.action === 'attachments') {
            const message = state.messages.find(item => item.id === button.dataset.messageId);
            if (message) openAttachments(message);
        } else if (button.dataset.action === 'retry') {
            if (state.chatId) loadChatMessages(state.chatId);
        }
    });
    $('message-attachments-list').addEventListener('click', event => {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const item = attachmentState.items.find(entry => entry.id === Number(button.dataset.id));
        if (!item) return;
        if (button.dataset.action === 'preview-attachment') openAttachmentPreview(item);
        else if (button.dataset.action === 'download-attachment') downloadAttachment(item);
    });
    $('message-attachments-dialog').addEventListener('close', resetAttachments);
}

export function onLocaleChange() {
    renderChats();
    renderHead();
    renderMessages();
}
