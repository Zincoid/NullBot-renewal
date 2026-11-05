import {
    $, t, api, escapeHtml, formatTime, emptyRow, renderPager,
    confirmAction, toast, PAGE_SIZE
} from '../app-shared.js';

// ═══ Quotes ══════════════════════

const quoteState = {page: 1, size: PAGE_SIZE, keyword: '', items: []};

async function loadQuotes() {
    const params = new URLSearchParams({
        page: String(quoteState.page),
        size: String(quoteState.size)
    });
    if (quoteState.keyword) params.set('keyword', quoteState.keyword);
    try {
        const data = await api('/quotes?' + params.toString());
        quoteState.items = data.items;
        renderQuotes(data);
    } catch (e) {
        $('quotes-body').innerHTML = emptyRow(6, t('common.loadError', {msg: e.message}));
        $('quotes-total').textContent = '';
        $('quotes-pager').innerHTML = '';
        toast(t('quotes.loadFailed', {msg: e.message}), 'error');
    }
}

function renderQuotes(data) {
    $('quotes-total').textContent = t('quotes.total', {n: data.total});
    const body = $('quotes-body');
    if (!data.items.length) {
        body.innerHTML = emptyRow(6, t('quotes.empty'));
    } else {
        body.innerHTML = data.items.map(quote => `
            <tr>
                <td>${quote.id}</td>
                <td title="${escapeHtml(quote.nickname || '-')}">${escapeHtml(quote.nickname || '-')}</td>
                <td class="nowrap" title="${escapeHtml(quote.userId || '-')}"><code>${escapeHtml(quote.userId || '-')}</code></td>
                <td class="quote-cell">
                    <div class="quote-cell-inner">
                        <span class="quote-text" title="${escapeHtml(quote.text || '')}">${escapeHtml(quote.text || '')}</span>
                        <button class="quote-copy" data-copy-quote data-id="${quote.id}"
                                title="${escapeHtml(t('common.copy'))}" aria-label="${escapeHtml(t('common.copy'))}">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="9" y="9" width="13" height="13" rx="2"/>
                                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                            </svg>
                        </button>
                    </div>
                </td>
                <td class="nowrap">${formatTime(quote.time)}</td>
                <td class="actions"><button class="small danger btn-delete" data-delete-quote data-id="${quote.id}">${escapeHtml(t('common.delete'))}</button></td>
            </tr>`).join('');
    }
    renderPager($('quotes-pager'), data, page => {
        quoteState.page = page;
        loadQuotes();
    }, size => {
        quoteState.size = size;
        quoteState.page = 1;
        loadQuotes();
    });
}

async function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
        return;
    }
    const area = document.createElement('textarea');
    area.value = text;
    area.style.position = 'fixed';
    area.style.opacity = '0';
    document.body.appendChild(area);
    area.select();
    const ok = document.execCommand('copy');
    area.remove();
    if (!ok) throw new Error('Copy failed');
}

async function deleteQuote(quote) {
    if (!await confirmAction(t('quotes.deleteConfirm', {id: quote.id}))) return;
    try {
        await api('/quotes/' + quote.id, {method: 'DELETE'});
        toast(t('quotes.deleted'), 'ok');
        if (quoteState.page > 1 && quoteState.items.length === 1) quoteState.page--;
        loadQuotes();
    } catch (e) {
        toast(t('quotes.deleteFailed', {msg: e.message}), 'error');
    }
}

export function bind() {
    $('quotes-refresh').onclick = loadQuotes;
    $('quote-search').onclick = () => {
        quoteState.keyword = $('quote-keyword').value.trim();
        quoteState.page = 1;
        loadQuotes();
    };
    $('quote-reset').onclick = () => {
        quoteState.keyword = '';
        quoteState.page = 1;
        $('quote-keyword').value = '';
        loadQuotes();
    };
    $('quote-keyword').addEventListener('keydown', event => {
        if (event.key === 'Enter') $('quote-search').click();
    });
    $('quotes-body').addEventListener('click', async event => {
        const copy = event.target.closest('[data-copy-quote]');
        if (copy) {
            const quote = quoteState.items.find(item => item.id === Number(copy.dataset.id));
            if (!quote) return;
            try {
                await copyText(quote.text || '');
                toast(t('common.copied'), 'ok');
            } catch {
                toast(t('common.copyFailed'), 'error');
            }
            return;
        }
        const button = event.target.closest('[data-delete-quote]');
        if (!button) return;
        const quote = quoteState.items.find(item => item.id === Number(button.dataset.id));
        if (quote) deleteQuote(quote);
    });
}

export {loadQuotes};
