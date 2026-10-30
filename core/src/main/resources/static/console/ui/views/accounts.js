import {
    $, t, api, escapeHtml, setStatus, emptyRow, renderPager,
    confirmAction, toast, PAGE_SIZE, options, accessOptions, formatTime
} from '../app-shared.js';

// ═══ Accounts ════════════════════

const userState = {page: 1, size: PAGE_SIZE, keyword: '', items: []};
const groupState = {page: 1, size: PAGE_SIZE, keyword: '', items: []};
let accountsTab = 'groups';
let currentGroup = null;
let currentUser = null;
let currentKeywords = [];
let editingKeyword = null;
let groupCmdRules = [];
let userCmdRules = [];
const KW_ARG_PRESETS = {user: '{userId}', group: '{groupId}'};
const KW_ARG_TYPES = [
    {value: 'text', labelKey: 'group.keywordArgText'},
    {value: 'user', labelKey: 'group.keywordArgUser'},
    {value: 'group', labelKey: 'group.keywordArgGroup'}
];

function loadAccounts() {
    if (accountsTab === 'users') loadUsers();
    else loadGroups();
}

function showAccountsTab(tab) {
    accountsTab = tab;
    document.querySelectorAll('[data-tab]').forEach(button =>
        button.classList.toggle('active', button.dataset.tab === tab));
    $('tab-users').hidden = tab !== 'users';
    $('tab-groups').hidden = tab !== 'groups';
}

function switchAccountsTab(tab) {
    showAccountsTab(tab);
    loadAccounts();
}

async function loadUsers() {
    const params = new URLSearchParams({
        page: String(userState.page),
        size: String(userState.size)
    });
    if (userState.keyword) params.set('keyword', userState.keyword);
    try {
        const data = await api('/users?' + params.toString());
        userState.items = data.items;
        renderUsers(data);
    } catch (e) {
        $('users-body').innerHTML = emptyRow(5, t('common.loadError', {msg: e.message}));
        $('users-total').textContent = '';
        $('users-pager').innerHTML = '';
        toast(t('accounts.loadUsersFailed', {msg: e.message}), 'error');
    }
}

function renderUsers(data) {
    $('users-total').textContent = t('accounts.usersTotal', {n: data.total});
    const body = $('users-body');
    if (!data.items.length) {
        body.innerHTML = emptyRow(5, t('accounts.usersEmpty'));
    } else {
        body.innerHTML = data.items.map(user => `
            <tr data-id="${escapeHtml(user.id)}">
                <td><code>${escapeHtml(user.id)}</code></td>
                <td><span class="name-text">${escapeHtml(user.name || '-')}</span></td>
                <td><select class="access-select">${accessOptions(user.access)}</select></td>
                <td class="private-chat-cell">
                    <div class="checks"><label><input type="checkbox" class="private-chat-toggle"
                        aria-label="${escapeHtml(t('accounts.privateChat'))}"
                        ${user.privateMessageAllowed ? 'checked' : ''}></label></div>
                </td>
                <td class="actions">
                    <button class="small primary btn-save" data-save-user>${escapeHtml(t('common.save'))}</button>
                    <button class="small danger btn-delete" data-delete-user>${escapeHtml(t('common.delete'))}</button>
                    <span class="actions-divider" aria-hidden="true"></span>
                    <button class="small btn-settings" data-user-settings>${escapeHtml(t('accounts.settings'))}</button>
                </td>
            </tr>`).join('');
    }
    renderPager($('users-pager'), data, page => {
        userState.page = page;
        loadUsers();
    }, size => {
        userState.size = size;
        userState.page = 1;
        loadUsers();
    });
}

async function loadGroups() {
    const params = new URLSearchParams({
        page: String(groupState.page),
        size: String(groupState.size)
    });
    if (groupState.keyword) params.set('keyword', groupState.keyword);
    try {
        const data = await api('/groups?' + params.toString());
        groupState.items = data.items;
        renderGroups(data);
    } catch (e) {
        $('groups-body').innerHTML = emptyRow(4, t('common.loadError', {msg: e.message}));
        $('groups-total').textContent = '';
        $('groups-pager').innerHTML = '';
        toast(t('accounts.loadGroupsFailed', {msg: e.message}), 'error');
    }
}

function renderGroups(data) {
    $('groups-total').textContent = t('accounts.groupsTotal', {n: data.total});
    const body = $('groups-body');
    if (!data.items.length) {
        body.innerHTML = emptyRow(4, t('accounts.groupsEmpty'));
    } else {
        body.innerHTML = data.items.map(group => `
            <tr data-id="${escapeHtml(group.id)}">
                <td><code>${escapeHtml(group.id)}</code></td>
                <td><span class="name-text">${escapeHtml(group.name || '-')}</span></td>
                <td><select class="access-select">${accessOptions(group.access)}</select></td>
                <td class="actions">
                    <button class="small primary btn-save" data-save-group>${escapeHtml(t('common.save'))}</button>
                    <button class="small danger btn-delete" data-delete-group>${escapeHtml(t('common.delete'))}</button>
                    <span class="actions-divider" aria-hidden="true"></span>
                    <button class="small btn-settings" data-group-settings>${escapeHtml(t('accounts.settings'))}</button>
                </td>
            </tr>`).join('');
    }
    renderPager($('groups-pager'), data, page => {
        groupState.page = page;
        loadGroups();
    }, size => {
        groupState.size = size;
        groupState.page = 1;
        loadGroups();
    });
}

async function saveUser(button) {
    const row = button.closest('tr');
    const id = row.dataset.id;
    const access = Number(row.querySelector('.access-select').value);
    const privateMessageAllowed = row.querySelector('.private-chat-toggle').checked;
    button.disabled = true;
    try {
        await api('/users/' + encodeURIComponent(id), {
            method: 'PUT',
            body: JSON.stringify({access, privateMessageAllowed})
        });
        toast(t('accounts.userSaved'), 'ok');
    } catch (e) {
        toast(t('accounts.saveFailed', {msg: e.message}), 'error');
    } finally {
        button.disabled = false;
    }
}

async function saveGroup(button) {
    const row = button.closest('tr');
    const id = row.dataset.id;
    const access = Number(row.querySelector('.access-select').value);
    button.disabled = true;
    try {
        await api('/groups/' + encodeURIComponent(id), {
            method: 'PUT',
            body: JSON.stringify({access})
        });
        toast(t('accounts.groupSaved'), 'ok');
    } catch (e) {
        toast(t('accounts.saveFailed', {msg: e.message}), 'error');
    } finally {
        button.disabled = false;
    }
}

async function deleteUser(button) {
    const id = button.closest('tr').dataset.id;
    const user = userState.items.find(item => item.id === id);
    const name = user?.name || id;
    if (!await confirmAction(t('accounts.userDeleteConfirm', {name}))) return;
    button.disabled = true;
    try {
        await api('/users/' + encodeURIComponent(id), {method: 'DELETE'});
        toast(t('accounts.userDeleted'), 'ok');
        if (userState.page > 1 && userState.items.length === 1) userState.page--;
        loadUsers();
    } catch (e) {
        toast(t('accounts.deleteFailed', {msg: e.message}), 'error');
        button.disabled = false;
    }
}

async function deleteGroup(button) {
    const id = button.closest('tr').dataset.id;
    const group = groupState.items.find(item => item.id === id);
    const name = group?.name || id;
    if (!await confirmAction(t('accounts.groupDeleteConfirm', {name}))) return;
    button.disabled = true;
    try {
        await api('/groups/' + encodeURIComponent(id), {method: 'DELETE'});
        toast(t('accounts.groupDeleted'), 'ok');
        if (groupState.page > 1 && groupState.items.length === 1) groupState.page--;
        loadGroups();
    } catch (e) {
        toast(t('accounts.deleteFailed', {msg: e.message}), 'error');
        button.disabled = false;
    }
}

function openUserDialog(user) {
    currentUser = user;
    $('user-dialog-subtitle').textContent = user.name ? user.name + ' · ' + user.id : user.id;
    setStatus($('us-status'), '');
    $('us-effort').value = '3';
    $('us-guard').checked = true;
    $('us-prompt').value = '';
    resetUserCmdForm();
    switchUserTab('setting');
    $('user-dialog').showModal();
    loadUserSetting();
    loadUserCmds();
}

async function loadUserSetting() {
    if (!currentUser) return;
    try {
        const setting = await api('/users/' + encodeURIComponent(currentUser.id) + '/setting');
        $('us-effort').value = String(setting.aiEffort);
        $('us-guard').checked = setting.aiInjectionGuard;
        $('us-prompt').value = setting.aiPrompt || '';
        setStatus($('us-status'), '');
    } catch (e) {
        setStatus($('us-status'), e.message, 'error');
    }
}

async function saveUserSetting(event) {
    event.preventDefault();
    if (!currentUser) return;
    setStatus($('us-status'), t('accounts.saving'));
    try {
        await api('/users/' + encodeURIComponent(currentUser.id) + '/setting', {
            method: 'PUT',
            body: JSON.stringify({
                aiEffort: Number($('us-effort').value),
                aiPrompt: $('us-prompt').value,
                aiInjectionGuard: $('us-guard').checked
            })
        });
        $('user-dialog').close();
        toast(t('accounts.userSettingSaved'), 'ok');
    } catch (e) {
        setStatus($('us-status'), e.message, 'error');
    }
}

function openGroupDialog(group) {
    currentGroup = group;
    $('group-dialog-title').textContent = group.name || group.id;
    $('group-dialog-subtitle').textContent = t('group.id', {id: group.id});
    setStatus($('gs-status'), '');
    setStatus($('kw-status'), '');
    resetKeywordForm();
    resetGroupCmdForm();
    switchGroupTab('setting');
    $('group-dialog').showModal();
    loadGroupSetting();
    loadKeywords();
    loadGroupCmds();
}

function switchGroupTab(tab) {
    document.querySelectorAll('[data-group-tab]').forEach(button =>
        button.classList.toggle('active', button.dataset.groupTab === tab));
    $('group-tab-setting').hidden = tab !== 'setting';
    $('group-tab-keywords').hidden = tab !== 'keywords';
    $('group-tab-commands').hidden = tab !== 'commands';
}

function switchUserTab(tab) {
    document.querySelectorAll('[data-user-tab]').forEach(button =>
        button.classList.toggle('active', button.dataset.userTab === tab));
    $('user-tab-setting').hidden = tab !== 'setting';
    $('user-tab-commands').hidden = tab !== 'commands';
}

async function loadGroupSetting() {
    if (!currentGroup) return;
    try {
        const setting = await api('/groups/' + encodeURIComponent(currentGroup.id) + '/setting');
        $('gs-language').value = String(setting.language);
        $('gs-join-notice').checked = setting.joinNoticeEnabled;
        $('gs-leave-notice').checked = setting.leaveNoticeEnabled;
        $('gs-anti-recall').checked = setting.antiRecallEnabled;
        $('gs-effort').value = String(setting.aiEffort);
        $('gs-scope').value = String(setting.limitScope);
        $('gs-capacity').value = setting.limitCapacity;
        $('gs-refill').value = setting.limitRefill;
        $('gs-interval').value = setting.limitInterval;
        $('gs-chat-frequency').value = setting.aiChatFrequency;
        $('gs-limit-enabled').checked = setting.limitEnabled;
        $('gs-auto-chat').checked = setting.aiAutoChatEnabled;
        $('gs-guard').checked = setting.aiInjectionGuard;
        $('gs-prompt').value = setting.aiPrompt || '';
        setStatus($('gs-status'), '');
    } catch (e) {
        setStatus($('gs-status'), e.message, 'error');
    }
}

async function saveGroupSetting(event) {
    event.preventDefault();
    if (!currentGroup) return;
    const capacity = Number($('gs-capacity').value);
    const refill = Number($('gs-refill').value);
    const interval = Number($('gs-interval').value);
    const frequencyText = $('gs-chat-frequency').value.trim();
    const frequency = Number(frequencyText);
    if (!Number.isInteger(capacity) || capacity < 1
        || !Number.isInteger(refill) || refill < 1
        || !Number.isInteger(interval) || interval < 1) {
        setStatus($('gs-status'), t('group.positiveInts'), 'error');
        return;
    }
    if (!frequencyText || !Number.isFinite(frequency) || frequency < 0 || frequency > 1) {
        setStatus($('gs-status'), t('group.frequencyRange'), 'error');
        return;
    }
    setStatus($('gs-status'), t('group.saving'));
    try {
        await api('/groups/' + encodeURIComponent(currentGroup.id) + '/setting', {
            method: 'PUT',
            body: JSON.stringify({
                language: Number($('gs-language').value),
                joinNoticeEnabled: $('gs-join-notice').checked,
                leaveNoticeEnabled: $('gs-leave-notice').checked,
                antiRecallEnabled: $('gs-anti-recall').checked,
                aiEffort: Number($('gs-effort').value),
                limitScope: Number($('gs-scope').value),
                limitCapacity: capacity,
                limitRefill: refill,
                limitInterval: interval,
                limitEnabled: $('gs-limit-enabled').checked,
                aiInjectionGuard: $('gs-guard').checked,
                aiAutoChatEnabled: $('gs-auto-chat').checked,
                aiChatFrequency: frequency,
                aiPrompt: $('gs-prompt').value
            })
        });
        toast(t('group.saved'), 'ok');
        loadGroupSetting();
    } catch (e) {
        setStatus($('gs-status'), e.message, 'error');
    }
}

async function loadKeywords() {
    if (!currentGroup) return;
    try {
        currentKeywords = await api('/groups/' + encodeURIComponent(currentGroup.id) + '/keywords');
        renderKeywords();
        setStatus($('kw-status'), '');
    } catch (e) {
        setStatus($('kw-status'), e.message, 'error');
    }
}

function renderKeywords() {
    const body = $('kw-body');
    if (!currentKeywords.length) {
        body.innerHTML = emptyRow(6, t('group.keywordEmpty'));
        return;
    }
    body.innerHTML = currentKeywords.map(rule => `
        <tr data-id="${rule.id}">
            <td>${rule.id}</td>
            <td>${escapeHtml(rule.keyword)}</td>
            <td><code>${escapeHtml(rule.cmd)}</code></td>
            <td>${escapeHtml((rule.args || []).join(' ')) || '<span class="muted">-</span>'}</td>
            <td><span class="badge${rule.enabled ? '' : ' off'}">${escapeHtml(rule.enabled ? t('group.enabled') : t('group.disabled'))}</span></td>
            <td class="actions">
                <button class="small btn-edit" data-kw-action="edit" data-id="${rule.id}">${escapeHtml(t('common.edit'))}</button>
                <button class="small btn-toggle ${rule.enabled ? 'btn-disable' : 'btn-enable'}" data-kw-action="toggle" data-id="${rule.id}">${escapeHtml(rule.enabled ? t('group.disabled') : t('group.enabled'))}</button>
                <button class="small danger btn-delete" data-kw-action="delete" data-id="${rule.id}">${escapeHtml(t('common.delete'))}</button>
            </td>
        </tr>`).join('');
}

function kwArgType(value) {
    const text = String(value ?? '').trim();
    if (text === KW_ARG_PRESETS.user) return 'user';
    if (text === KW_ARG_PRESETS.group) return 'group';
    return 'text';
}

function kwArgTypeOptions(selected) {
    return options(KW_ARG_TYPES.map(item => ({value: item.value, label: t(item.labelKey)})), selected);
}

function kwArgRow(value, type) {
    const preset = KW_ARG_PRESETS[type] || '';
    const row = document.createElement('div');
    row.className = 'kw-arg-row';
    row.innerHTML = `
        <select class="kw-arg-type" aria-label="${escapeHtml(t('group.keywordArgType'))}">
            ${kwArgTypeOptions(type)}
        </select>
        <input class="kw-arg-value" value="${escapeHtml(value ?? '')}"
               ${preset ? 'disabled' : ''}
               placeholder="${escapeHtml(preset || t('group.keywordArgValue'))}"
               aria-label="${escapeHtml(t('group.keywordArgValue'))}">
        <div class="kw-arg-btns">
            <button type="button" class="small kw-arg-btn kw-arg-add"
                    title="${escapeHtml(t('group.keywordArgAdd'))}"
                    aria-label="${escapeHtml(t('group.keywordArgAdd'))}">+</button>
            <button type="button" class="small kw-arg-btn kw-arg-remove"
                    title="${escapeHtml(t('group.keywordArgRemove'))}"
                    aria-label="${escapeHtml(t('group.keywordArgRemove'))}">−</button>
        </div>`;
    return row;
}

function renderKeywordArgs(args) {
    const list = $('kw-args-list');
    list.innerHTML = '';
    const values = Array.isArray(args) && args.length ? args : [''];
    values.forEach(value => list.appendChild(kwArgRow(value, kwArgType(value))));
}

function applyKwArgType(row, type) {
    const select = row.querySelector('.kw-arg-type');
    const input = row.querySelector('.kw-arg-value');
    select.value = type;
    const preset = KW_ARG_PRESETS[type];
    input.disabled = Boolean(preset);
    if (preset) input.value = preset;
    else if (Object.values(KW_ARG_PRESETS).includes(input.value.trim())) input.value = '';
    input.placeholder = preset || t('group.keywordArgValue');
}

function addKeywordArgRow(after) {
    const row = kwArgRow('', 'text');
    if (after) after.after(row);
    else $('kw-args-list').appendChild(row);
    row.querySelector('.kw-arg-value').focus();
}

function removeKeywordArgRow(row) {
    const list = $('kw-args-list');
    if (list.children.length <= 1) {
        applyKwArgType(row, 'text');
        const input = row.querySelector('.kw-arg-value');
        input.value = '';
        input.focus();
        return;
    }
    const index = Array.prototype.indexOf.call(list.children, row);
    row.remove();
    const next = list.children[Math.min(index, list.children.length - 1)];
    if (next) next.querySelector('.kw-arg-value').focus();
}

function collectKeywordArgs() {
    return Array.from(document.querySelectorAll('#kw-args-list .kw-arg-row'))
        .map(row => {
            const type = row.querySelector('.kw-arg-type').value;
            const value = row.querySelector('.kw-arg-value').value.trim();
            return value || KW_ARG_PRESETS[type] || '';
        })
        .filter(value => value !== '');
}

function editKeyword(rule) {
    editingKeyword = rule;
    $('kw-keyword').value = rule.keyword || '';
    $('kw-cmd').value = rule.cmd || '';
    renderKeywordArgs(rule.args || []);
    $('kw-save').textContent = t('group.keywordSave');
    $('kw-cancel').hidden = false;
    $('kw-keyword').focus();
}

function resetKeywordForm() {
    editingKeyword = null;
    $('kw-keyword').value = '';
    $('kw-cmd').value = '';
    renderKeywordArgs([]);
    $('kw-save').textContent = t('group.keywordAdd');
    $('kw-cancel').hidden = true;
    setStatus($('kw-status'), '');
}

async function saveKeyword() {
    if (!currentGroup) return;
    const keyword = $('kw-keyword').value.trim();
    const cmd = $('kw-cmd').value.trim();
    if (!keyword || !cmd) {
        setStatus($('kw-status'), t('group.keywordRequired'), 'error');
        return;
    }
    const body = {
        keyword,
        cmd,
        args: collectKeywordArgs()
    };
    const base = '/groups/' + encodeURIComponent(currentGroup.id) + '/keywords';
    try {
        if (editingKeyword) {
            await api(base + '/' + editingKeyword.id, {method: 'PUT', body: JSON.stringify(body)});
            toast(t('group.keywordUpdated'), 'ok');
        } else {
            await api(base, {method: 'POST', body: JSON.stringify(body)});
            toast(t('group.keywordAdded'), 'ok');
        }
        resetKeywordForm();
        loadKeywords();
    } catch (e) {
        setStatus($('kw-status'), e.message, 'error');
    }
}

async function toggleKeyword(rule) {
    if (!currentGroup) return;
    try {
        await api('/groups/' + encodeURIComponent(currentGroup.id) + '/keywords/'
            + rule.id + '/' + (rule.enabled ? 'disable' : 'enable'), {method: 'POST'});
        loadKeywords();
    } catch (e) {
        setStatus($('kw-status'), e.message, 'error');
    }
}

async function deleteKeyword(rule) {
    if (!currentGroup) return;
    if (!await confirmAction(t('group.keywordDeleteConfirm', {keyword: rule.keyword}))) return;
    try {
        await api('/groups/' + encodeURIComponent(currentGroup.id) + '/keywords/' + rule.id,
            {method: 'DELETE'});
        toast(t('group.keywordDeleted'), 'ok');
        loadKeywords();
    } catch (e) {
        setStatus($('kw-status'), e.message, 'error');
    }
}

// ════ Command disables ═════════

function syncDurationInput(prefix) {
    const unit = $(prefix + '-duration-unit');
    const amount = $(prefix + '-duration-value');
    const permanent = !unit.value;
    amount.disabled = permanent;
    if (permanent) {
        amount.value = '';
    } else {
        amount.focus();
    }
}

function readDuration(prefix, status) {
    const unit = $(prefix + '-duration-unit').value;
    if (!unit) return '';
    const amount = $(prefix + '-duration-value').valueAsNumber;
    if (!Number.isSafeInteger(amount) || amount < 1) {
        setStatus(status, t('cmdDisable.durationInvalid'), 'error');
        return null;
    }
    return amount + unit;
}

function resetGroupCmdForm() {
    groupCmdRules = [];
    $('gcd-target').value = '*';
    $('gcd-user').value = '';
    $('gcd-user').hidden = true;
    $('gcd-cmd').value = '';
    $('gcd-duration-unit').value = '';
    syncDurationInput('gcd');
    setStatus($('gcd-status'), '');
    renderGroupCmds();
}

function resetUserCmdForm() {
    userCmdRules = [];
    $('ucd-cmd').value = '';
    $('ucd-duration-unit').value = '';
    syncDurationInput('ucd');
    setStatus($('ucd-status'), '');
    renderUserCmds();
}

function cmdTarget(rule) {
    return rule.userId === '*'
        ? `<span class="badge">${escapeHtml(t('cmdDisable.wholeGroup'))}</span>`
        : `<code>${escapeHtml(rule.userId)}</code>`;
}

function cmdExpiry(rule) {
    return escapeHtml(rule.expiresAt ? formatTime(rule.expiresAt) : t('cmdDisable.permanent'));
}

async function loadGroupCmds() {
    if (!currentGroup) return;
    const groupId = currentGroup.id;
    try {
        const rules = await api('/groups/' + encodeURIComponent(groupId) + '/cmd-disables');
        if (!currentGroup || currentGroup.id !== groupId) return;
        groupCmdRules = rules;
        renderGroupCmds();
        setStatus($('gcd-status'), '');
    } catch (e) {
        if (!currentGroup || currentGroup.id !== groupId) return;
        setStatus($('gcd-status'), t('cmdDisable.loadFailed', {msg: e.message}), 'error');
    }
}

async function loadUserCmds() {
    if (!currentUser) return;
    const userId = currentUser.id;
    try {
        const rules = await api('/users/' + encodeURIComponent(userId) + '/cmd-disables');
        if (!currentUser || currentUser.id !== userId) return;
        userCmdRules = rules;
        renderUserCmds();
        setStatus($('ucd-status'), '');
    } catch (e) {
        if (!currentUser || currentUser.id !== userId) return;
        setStatus($('ucd-status'), t('cmdDisable.loadFailed', {msg: e.message}), 'error');
    }
}

function renderGroupCmds() {
    const body = $('gcd-body');
    if (!groupCmdRules.length) {
        body.innerHTML = emptyRow(4, t('cmdDisable.empty'));
        return;
    }
    body.innerHTML = groupCmdRules.map(rule => `
        <tr data-id="${rule.id}">
            <td>${cmdTarget(rule)}</td>
            <td><code>${escapeHtml(rule.cmd)}</code></td>
            <td>${cmdExpiry(rule)}</td>
            <td class="actions">
                <button class="small btn-toggle btn-enable" data-cmd-remove>${escapeHtml(t('cmdDisable.enable'))}</button>
            </td>
        </tr>`).join('');
}

function renderUserCmds() {
    const body = $('ucd-body');
    if (!userCmdRules.length) {
        body.innerHTML = emptyRow(3, t('cmdDisable.empty'));
        return;
    }
    body.innerHTML = userCmdRules.map(rule => `
        <tr data-id="${rule.id}">
            <td><code>${escapeHtml(rule.cmd)}</code></td>
            <td>${cmdExpiry(rule)}</td>
            <td class="actions">
                <button class="small btn-toggle btn-enable" data-cmd-remove>${escapeHtml(t('cmdDisable.enable'))}</button>
            </td>
        </tr>`).join('');
}

function updateGroupTarget() {
    const specific = $('gcd-target').value === 'user';
    $('gcd-user').hidden = !specific;
    if (specific) $('gcd-user').focus();
}

async function addGroupCmd() {
    if (!currentGroup) return;
    const target = $('gcd-target').value;
    const userId = target === 'user' ? $('gcd-user').value.trim() : '*';
    const cmd = $('gcd-cmd').value.trim();
    if (target === 'user' && !userId) {
        setStatus($('gcd-status'), t('cmdDisable.userRequired'), 'error');
        return;
    }
    if (!cmd) {
        setStatus($('gcd-status'), t('cmdDisable.cmdRequired'), 'error');
        return;
    }
    const duration = readDuration('gcd', $('gcd-status'));
    if (duration === null) return;
    const button = $('gcd-add');
    button.disabled = true;
    try {
        await api('/groups/' + encodeURIComponent(currentGroup.id) + '/cmd-disables', {
            method: 'POST',
            body: JSON.stringify({userId, cmd, duration})
        });
        $('gcd-cmd').value = '';
        toast(t('cmdDisable.added'), 'ok');
        await loadGroupCmds();
    } catch (e) {
        setStatus($('gcd-status'), t('cmdDisable.addFailed', {msg: e.message}), 'error');
    } finally {
        button.disabled = false;
    }
}

async function addUserCmd() {
    if (!currentUser) return;
    const cmd = $('ucd-cmd').value.trim();
    if (!cmd) {
        setStatus($('ucd-status'), t('cmdDisable.cmdRequired'), 'error');
        return;
    }
    const duration = readDuration('ucd', $('ucd-status'));
    if (duration === null) return;
    const button = $('ucd-add');
    button.disabled = true;
    try {
        await api('/users/' + encodeURIComponent(currentUser.id) + '/cmd-disables', {
            method: 'POST',
            body: JSON.stringify({cmd, duration})
        });
        $('ucd-cmd').value = '';
        toast(t('cmdDisable.added'), 'ok');
        await loadUserCmds();
    } catch (e) {
        setStatus($('ucd-status'), t('cmdDisable.addFailed', {msg: e.message}), 'error');
    } finally {
        button.disabled = false;
    }
}

async function removeGroupCmd(button) {
    if (!currentGroup) return;
    const rule = groupCmdRules.find(item => item.id === Number(button.closest('tr').dataset.id));
    if (!rule) return;
    const target = rule.userId === '*' ? t('cmdDisable.wholeGroup') : rule.userId;
    if (!await confirmAction(t('cmdDisable.removeConfirm', {target, cmd: rule.cmd}))) return;
    if (!currentGroup) return;
    button.disabled = true;
    try {
        const query = new URLSearchParams({userId: rule.userId});
        await api('/groups/' + encodeURIComponent(currentGroup.id) + '/cmd-disables/'
            + encodeURIComponent(rule.cmd) + '?' + query.toString(), {method: 'DELETE'});
        toast(t('cmdDisable.removed'), 'ok');
        await loadGroupCmds();
    } catch (e) {
        setStatus($('gcd-status'), t('cmdDisable.removeFailed', {msg: e.message}), 'error');
        if (e.status === 404) await loadGroupCmds();
        else button.disabled = false;
    }
}

async function removeUserCmd(button) {
    if (!currentUser) return;
    const rule = userCmdRules.find(item => item.id === Number(button.closest('tr').dataset.id));
    if (!rule) return;
    if (!await confirmAction(t('cmdDisable.removeConfirm', {target: currentUser.id, cmd: rule.cmd}))) return;
    if (!currentUser) return;
    button.disabled = true;
    try {
        await api('/users/' + encodeURIComponent(currentUser.id) + '/cmd-disables/'
            + encodeURIComponent(rule.cmd), {method: 'DELETE'});
        toast(t('cmdDisable.removed'), 'ok');
        await loadUserCmds();
    } catch (e) {
        setStatus($('ucd-status'), t('cmdDisable.removeFailed', {msg: e.message}), 'error');
        if (e.status === 404) await loadUserCmds();
        else button.disabled = false;
    }
}

export function bind() {
    $('accounts-refresh').onclick = loadAccounts;
    document.querySelectorAll('[data-tab]').forEach(button =>
        button.addEventListener('click', () => switchAccountsTab(button.dataset.tab)));
    $('user-search').onclick = () => {
        userState.keyword = $('user-keyword').value.trim();
        userState.page = 1;
        loadUsers();
    };
    $('user-reset').onclick = () => {
        userState.keyword = '';
        userState.page = 1;
        $('user-keyword').value = '';
        loadUsers();
    };
    $('user-keyword').addEventListener('keydown', event => {
        if (event.key === 'Enter') $('user-search').click();
    });
    $('group-search').onclick = () => {
        groupState.keyword = $('group-keyword').value.trim();
        groupState.page = 1;
        loadGroups();
    };
    $('group-reset').onclick = () => {
        groupState.keyword = '';
        groupState.page = 1;
        $('group-keyword').value = '';
        loadGroups();
    };
    $('group-keyword').addEventListener('keydown', event => {
        if (event.key === 'Enter') $('group-search').click();
    });

    $('users-body').addEventListener('click', event => {
        const save = event.target.closest('[data-save-user]');
        if (save) {
            saveUser(save);
            return;
        }
        const settings = event.target.closest('[data-user-settings]');
        if (settings) {
            const row = settings.closest('tr');
            const user = userState.items.find(item => item.id === row.dataset.id);
            if (user) openUserDialog(user);
            return;
        }
        const del = event.target.closest('[data-delete-user]');
        if (del) deleteUser(del);
    });
    $('groups-body').addEventListener('click', event => {
        const save = event.target.closest('[data-save-group]');
        if (save) {
            saveGroup(save);
            return;
        }
        const settings = event.target.closest('[data-group-settings]');
        if (settings) {
            const row = settings.closest('tr');
            const group = groupState.items.find(item => item.id === row.dataset.id);
            if (group) openGroupDialog(group);
            return;
        }
        const del = event.target.closest('[data-delete-group]');
        if (del) deleteGroup(del);
    });

    document.querySelectorAll('[data-group-tab]').forEach(button =>
        button.addEventListener('click', () => switchGroupTab(button.dataset.groupTab)));
    document.querySelectorAll('[data-user-tab]').forEach(button =>
        button.addEventListener('click', () => switchUserTab(button.dataset.userTab)));
    $('gcd-target').addEventListener('change', updateGroupTarget);
    $('gcd-duration-unit').addEventListener('change', () => syncDurationInput('gcd'));
    $('ucd-duration-unit').addEventListener('change', () => syncDurationInput('ucd'));
    $('gcd-add').onclick = addGroupCmd;
    $('ucd-add').onclick = addUserCmd;
    $('gcd-cmd').addEventListener('keydown', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            addGroupCmd();
        }
    });
    $('ucd-cmd').addEventListener('keydown', event => {
        if (event.key === 'Enter') {
            event.preventDefault();
            addUserCmd();
        }
    });
    $('gcd-body').addEventListener('click', event => {
        const button = event.target.closest('[data-cmd-remove]');
        if (button) removeGroupCmd(button);
    });
    $('ucd-body').addEventListener('click', event => {
        const button = event.target.closest('[data-cmd-remove]');
        if (button) removeUserCmd(button);
    });
    $('group-setting-form').addEventListener('submit', saveGroupSetting);
    $('user-setting-form').addEventListener('submit', saveUserSetting);
    $('kw-save').onclick = saveKeyword;
    $('kw-cancel').onclick = resetKeywordForm;
    $('kw-args-list').addEventListener('click', event => {
        const add = event.target.closest('.kw-arg-add');
        if (add) {
            addKeywordArgRow(add.closest('.kw-arg-row'));
            return;
        }
        const remove = event.target.closest('.kw-arg-remove');
        if (remove) removeKeywordArgRow(remove.closest('.kw-arg-row'));
    });
    $('kw-args-list').addEventListener('change', event => {
        const select = event.target.closest('.kw-arg-type');
        if (select) applyKwArgType(select.closest('.kw-arg-row'), select.value);
    });
    $('kw-body').addEventListener('click', event => {
        const button = event.target.closest('button[data-kw-action]');
        if (!button) return;
        const rule = currentKeywords.find(item => item.id === Number(button.dataset.id));
        if (!rule) return;
        if (button.dataset.kwAction === 'edit') editKeyword(rule);
        else if (button.dataset.kwAction === 'toggle') toggleKeyword(rule);
        else if (button.dataset.kwAction === 'delete') deleteKeyword(rule);
    });

    $('user-dialog').addEventListener('close', () => {
        currentUser = null;
        userCmdRules = [];
    });
    $('group-dialog').addEventListener('close', () => {
        currentGroup = null;
        currentKeywords = [];
        groupCmdRules = [];
        resetKeywordForm();
    });
}

export function onLocaleChange() {
    setStatus($('gs-status'), '');
    setStatus($('kw-status'), '');
    setStatus($('gcd-status'), '');
    setStatus($('ucd-status'), '');
    if (currentGroup && $('group-dialog').open) {
        $('group-dialog-subtitle').textContent = t('group.id', {id: currentGroup.id});
        resetKeywordForm();
        renderKeywords();
        renderGroupCmds();
    }
    if (currentUser && $('user-dialog').open) {
        renderUserCmds();
    }
}

export {loadAccounts, showAccountsTab};
