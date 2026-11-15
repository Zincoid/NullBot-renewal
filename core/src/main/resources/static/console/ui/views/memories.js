import {
    $, t, api, escapeHtml, formatTime, toast, confirmAction
} from '../app-shared.js';

// ═══ Memories ═════════════════════

const state = {
    chats: [],
    chatId: null,
    chatKind: 'group',
    chatKindChosen: false,
    detail: null,
    search: '',
    chatsLoading: false,
    chatsError: '',
    detailLoading: false,
    detailError: '',
    busy: false
};
let chatsSeq = 0;
let detailSeq = 0;

function kindKey(type) {
    return type === 'group' || type === 'private' ? type : 'unknown';
}

function kindLabel(type) {
    return t('messages.kind.' + kindKey(type));
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
    return `<div class="memories-state">${escapeHtml(text)}${button}</div>`;
}

function filteredFacts() {
    const facts = state.detail && Array.isArray(state.detail.semantic)
        ? state.detail.semantic : [];
    const keyword = state.search.trim().toLowerCase();
    if (!keyword) return facts;
    return facts.filter(fact => String(fact.content || '').toLowerCase().includes(keyword));
}

function memoryTime(memory) {
    const created = formatTime(memory.createdAt);
    const updated = formatTime(memory.updatedAt);
    return updated !== '-' && updated !== created
        ? `${created} – ${updated}` : created;
}

// ═══ Render ═══════════════════════

function renderChats() {
    const list = $('memories-chat-list');
    const switcher = $('memories-chat-switch');
    $('memories-group-count').textContent = chatsOf('group').length;
    $('memories-private-count').textContent = chatsOf('private').length;
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
            ? 'memories.emptyPrivateChats'
            : 'memories.emptyGroupChats'));
        return;
    }
    list.innerHTML = chats.map(chat => {
        const active = chat.chatId === state.chatId;
        const name = chat.name || chat.chatId;
        const secondary = name === chat.chatId ? '' : chat.chatId;
        const badges = [];
        if (Number(chat.episodic) > 0) {
            badges.push(`<span class="memories-badge"
                    title="${escapeHtml(t('memories.episodic'))}">${escapeHtml(t('memories.episodicShort'))}</span>`);
        }
        if (Number(chat.semantic) > 0) {
            badges.push(`<span class="memories-badge"
                    title="${escapeHtml(t('memories.semanticCount', {n: chat.semantic}))}">${escapeHtml(t('memories.semanticCount', {n: chat.semantic}))}</span>`);
        }
        const badgeHtml = badges.length
            ? `<span class="memories-chat__badges">${badges.join('')}</span>`
            : '';
        const timeHtml = chat.lastAt
            ? `<time class="memories-chat__time">${escapeHtml(formatTime(chat.lastAt))}</time>`
            : '';
        const sideHtml = badgeHtml || timeHtml
            ? `<span class="memories-chat__side">${timeHtml}${badgeHtml}</span>`
            : '';
        return `
            <button type="button" class="memories-chat${active ? ' active' : ''}"
                    data-chat-id="${escapeHtml(chat.chatId)}" aria-pressed="${active}">
                <span class="memories-chat__main">
                    <span class="memories-chat__name">${escapeHtml(name)}</span>
                    ${secondary ? `<span class="memories-chat__id">${escapeHtml(secondary)}</span>` : ''}
                </span>
                ${sideHtml}
            </button>`;
    }).join('');
}

function renderHead() {
    const chat = currentChat();
    const head = $('memories-head');
    if (!chat) {
        head.hidden = true;
        return;
    }
    head.hidden = false;
    $('memories-chat-name').textContent = chat.name || chat.chatId;
    const chatIdElement = $('memories-chat-id');
    chatIdElement.textContent = chat.chatId;
    chatIdElement.title = chat.chatId;
    const meta = [];
    if (Number(chat.episodic) > 0) meta.push(t('memories.episodic'));
    meta.push(t('memories.semanticCount', {n: chat.semantic}));
    $('memories-chat-meta').textContent = meta.join(' · ');
    const kind = $('memories-chat-kind');
    kind.textContent = kindLabel(chat.type);
    kind.className = 'memories-kind memories-kind--' + kindKey(chat.type);
}

function renderFacts() {
    const container = $('memories-facts');
    if (!container) return;
    const facts = filteredFacts();
    if (!facts.length) {
        container.innerHTML = `<div class="memories-empty">${escapeHtml(t(
            state.search.trim() ? 'memories.searchEmpty' : 'memories.semanticEmpty'))}</div>`;
        return;
    }
    container.innerHTML = facts.map(fact => `
        <article class="memories-fact">
            <div class="memories-fact__head">
                <span class="memories-fact__id">#${escapeHtml(String(fact.id))}</span>
                <time class="memories-fact__time">${escapeHtml(formatTime(fact.createdAt))}</time>
                <button type="button" class="memories-fact__delete btn-delete"
                        data-action="delete-semantic" data-id="${escapeHtml(String(fact.id))}"
                        title="${escapeHtml(t('common.delete'))}"
                        aria-label="${escapeHtml(t('common.delete'))}"></button>
            </div>
            <p class="memories-fact__content">${escapeHtml(fact.content || '')}</p>
        </article>`).join('');
}

function renderDetail() {
    const list = $('memories-list');
    if (!state.chatId) {
        list.innerHTML = emptyState(t('memories.selectChat'));
        return;
    }
    if (state.detailLoading) {
        list.innerHTML = emptyState(t('common.loading'));
        return;
    }
    if (state.detailError) {
        list.innerHTML = emptyState(state.detailError, true);
        return;
    }
    const detail = state.detail;
    if (!detail) {
        list.innerHTML = emptyState(t('memories.selectChat'));
        return;
    }
    const chat = currentChat();
    const episodic = detail.episodic;
    const semantic = Array.isArray(detail.semantic) ? detail.semantic : [];
    const total = chat ? Number(chat.semantic) || 0 : semantic.length;
    const truncated = total > semantic.length;

    const episodicHtml = episodic
        ? `<section class="memories-block memories-block--episodic">
               <header class="memories-block__head">
                   <h3>${escapeHtml(t('memories.episodic'))}</h3>
                   <time>${escapeHtml(memoryTime(episodic))}</time>
                   <button type="button" class="small danger btn-delete"
                           data-action="delete-episodic">${escapeHtml(t('common.delete'))}</button>
               </header>
               <div class="memories-episodic">${escapeHtml(episodic.content || '')}</div>
           </section>`
        : `<section class="memories-block memories-block--episodic is-empty">
               <header class="memories-block__head">
                   <h3>${escapeHtml(t('memories.episodic'))}</h3>
                   <button type="button" class="small danger btn-delete"
                           data-action="delete-episodic" disabled>${escapeHtml(t('common.delete'))}</button>
               </header>
               <div class="memories-empty">${escapeHtml(t('memories.episodicEmpty'))}</div>
           </section>`;

    const semanticHtml = `
        <section class="memories-block memories-block--semantic">
            <header class="memories-block__head">
                <h3>${escapeHtml(t('memories.semantic'))}
                    <span class="memories-count">${escapeHtml(String(total))}</span>
                </h3>
                <div class="memories-tools">
                    <input type="search" id="memories-search" class="memories-search"
                           value="${escapeHtml(state.search)}"
                           placeholder="${escapeHtml(t('memories.searchPlaceholder'))}"
                           aria-label="${escapeHtml(t('memories.searchPlaceholder'))}"
                           ${total ? '' : 'disabled'}>
                    <span class="memories-tools__divider" aria-hidden="true"></span>
                    <button type="button" class="small danger btn-delete"
                            data-action="clear-semantic" ${total ? '' : 'disabled'}>
                        ${escapeHtml(t('memories.clear'))}
                    </button>
                </div>
            </header>
            ${truncated ? `<p class="memories-hint">${escapeHtml(t('memories.truncated', {n: semantic.length}))}</p>` : ''}
            <div class="memories-facts" id="memories-facts"></div>
        </section>`;

    list.innerHTML = episodicHtml + semanticHtml;
    renderFacts();
}

// ═══ Load ═════════════════════════

async function loadDetail(chatId, seq = ++detailSeq) {
    state.detailLoading = true;
    state.detailError = '';
    state.detail = null;
    renderDetail();
    try {
        const params = new URLSearchParams({chatId});
        const detail = await api('/memories?' + params.toString());
        if (seq !== detailSeq) return;
        state.detail = detail;
        state.detailLoading = false;
        renderDetail();
    } catch (e) {
        if (seq !== detailSeq) return;
        state.detailLoading = false;
        state.detailError = t('memories.loadDetailFailed', {msg: e.message});
        renderDetail();
        toast(state.detailError, 'error');
    }
}

function selectChat(chatId) {
    if (!chatId || chatId === state.chatId) return;
    state.chatId = chatId;
    state.search = '';
    state.detail = null;
    state.detailError = '';
    renderChats();
    renderHead();
    loadDetail(chatId);
}

function switchChatKind(kind) {
    if (kind !== 'group' && kind !== 'private') return;
    if (kind === state.chatKind) return;
    state.chatKind = kind;
    state.chatKindChosen = true;
    state.search = '';
    ensureChatSelection();
    renderChats();
    renderHead();
    if (!state.chatId) {
        state.detail = null;
        state.detailError = '';
        state.detailLoading = false;
        renderDetail();
        return;
    }
    loadDetail(state.chatId);
}

export async function loadMemories() {
    const seq = ++chatsSeq;
    state.chatsLoading = true;
    state.chatsError = '';
    renderChats();
    try {
        const chats = await api('/memories/chats');
        if (seq !== chatsSeq) return;
        state.chats = Array.isArray(chats) ? chats : [];
        state.chatsLoading = false;
        if (!state.chatKindChosen) {
            state.chatKindChosen = true;
            if (!chatsOf('group').length && chatsOf('private').length)
                state.chatKind = 'private';
        }
        const previousChatId = state.chatId;
        ensureChatSelection();
        if (previousChatId !== state.chatId) state.search = '';
        renderChats();
        renderHead();
        if (state.chatId) {
            await loadDetail(state.chatId);
        } else {
            state.detail = null;
            state.detailError = '';
            state.detailLoading = false;
            renderDetail();
        }
    } catch (e) {
        if (seq !== chatsSeq) return;
        state.chatsLoading = false;
        state.chatsError = t('memories.loadChatsFailed', {msg: e.message});
        state.chats = [];
        state.chatId = null;
        state.detail = null;
        state.detailError = '';
        state.detailLoading = false;
        renderChats();
        renderHead();
        renderDetail();
        toast(state.chatsError, 'error');
    }
}

// ═══ Delete ═══════════════════════

async function deleteEpisodic() {
    if (state.busy || !state.chatId) return;
    const chat = currentChat();
    const name = chat ? (chat.name || chat.chatId) : state.chatId;
    if (!await confirmAction(t('memories.deleteEpisodicConfirm', {name}))) return;
    state.busy = true;
    try {
        await api('/memories/episodic?' + new URLSearchParams({chatId: state.chatId}),
            {method: 'DELETE'});
        toast(t('memories.deleted'), 'ok');
        await loadMemories();
    } catch (e) {
        toast(t('memories.actionFailed', {msg: e.message}), 'error');
    } finally {
        state.busy = false;
    }
}

async function deleteSemantic(id) {
    if (state.busy || !state.chatId || !Number.isFinite(id)) return;
    if (!await confirmAction(t('memories.deleteSemanticConfirm', {id}))) return;
    state.busy = true;
    try {
        await api('/memories/semantic/' + id
            + '?' + new URLSearchParams({chatId: state.chatId}), {method: 'DELETE'});
        toast(t('memories.deleted'), 'ok');
        await loadMemories();
    } catch (e) {
        toast(t('memories.actionFailed', {msg: e.message}), 'error');
    } finally {
        state.busy = false;
    }
}

async function clearSemantic() {
    if (state.busy || !state.chatId) return;
    const chat = currentChat();
    const name = chat ? (chat.name || chat.chatId) : state.chatId;
    if (!await confirmAction(t('memories.clearConfirm', {name}))) return;
    state.busy = true;
    try {
        await api('/memories/semantic?' + new URLSearchParams({chatId: state.chatId}),
            {method: 'DELETE'});
        toast(t('memories.cleared'), 'ok');
        await loadMemories();
    } catch (e) {
        toast(t('memories.actionFailed', {msg: e.message}), 'error');
    } finally {
        state.busy = false;
    }
}

// ═══ Binding ══════════════════════

export function bind() {
    $('memories-refresh').onclick = loadMemories;
    $('memories-chat-list').addEventListener('click', event => {
        const button = event.target.closest('button[data-chat-id]');
        if (button) selectChat(button.dataset.chatId);
    });
    $('memories-chat-switch').addEventListener('click', event => {
        const button = event.target.closest('button[data-chat-kind]');
        if (button) switchChatKind(button.dataset.chatKind);
    });
    $('memories-list').addEventListener('click', event => {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const action = button.dataset.action;
        if (action === 'delete-episodic') deleteEpisodic();
        else if (action === 'delete-semantic') deleteSemantic(Number(button.dataset.id));
        else if (action === 'clear-semantic') clearSemantic();
        else if (action === 'retry') {
            if (state.chatId) loadDetail(state.chatId);
            else loadMemories();
        }
    });
    $('memories-list').addEventListener('input', event => {
        if (event.target.id !== 'memories-search') return;
        state.search = event.target.value;
        renderFacts();
    });
}

export function onLocaleChange() {
    renderChats();
    renderHead();
    renderDetail();
}
