import { apiGet, apiRequest, HttpError } from '../../shared/httpClient.js';
import { fetchCurrentUserOrNull } from '../../shared/authApi.js';

export function initUsersAccess() {
    const tbody = document.querySelector('[data-users-table-body]');
    const errorBox = document.getElementById('users-error');

    if (!tbody) return;

    renderLoading(tbody);
    hideError(errorBox);

    loadAndRenderUsers(tbody, errorBox).catch((err) => {
        console.error('Failed to load users:', err);
        renderErrorState(
            'Unable to load users. Please try again later.',
            tbody,
            errorBox,
        );
    });
}

async function loadAndRenderUsers(tbody, errorBox) {
    hideError(errorBox);
    let currentUser;
    try {
        currentUser = await fetchCurrentUserOrNull();
    } catch (err) {
        console.error('Failed to load current user for users-access:', err);
    }

    if (!currentUser) {
        redirectToLogin();
        return;
    }

    const isAdmin = currentUser.role === 'ADMIN';
    const isOwner = currentUser.role === 'OWNER';

    let users;
    try {
        users = await apiGet('/users', { withAuth: true });
    } catch (err) {
        if (err instanceof HttpError && (err.status === 401 || err.status === 403)) {
            redirectToLogin();
            return;
        }
        throw err;
    }

    if (!Array.isArray(users) || users.length === 0) {
        renderEmpty(tbody);
        return;
    }

    tbody.innerHTML = '';

    for (const user of users) {
        const tr = document.createElement('tr');
        const role = (user.role || '').toString().toUpperCase();

        const statusPending = role !== 'ADMIN' && !user.approvedByOwner;

        let pillClass = 'users-pill--accountant';
        if (role === 'OWNER') {
            pillClass = 'users-pill--owner';
        } else if (role === 'ADMIN') {
            pillClass = 'users-pill--admin';
        }

        tr.innerHTML = `
            <td>${escapeHtml(user.fullName) || '—'}</td>
            <td>${escapeHtml(user.email)}</td>
            <td>
                <span class="users-pill ${pillClass}">
                    ${escapeHtml(role)}
                </span>
            </td>
            <td>
                <span class="users-status ${
            statusPending
                ? 'users-status--pending'
                : 'users-status--active'
        }">
                    ${statusPending ? 'Pending approval' : 'Active'}
                </span>
            </td>
            <td></td>
        `;

        const actionCell = tr.lastElementChild;

        if (statusPending && role !== 'ADMIN' && (isAdmin || isOwner)) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'users-btn-approve';
            btn.textContent = 'Approve';

            btn.addEventListener('click', async () => {
                btn.disabled = true;
                try {
                    await approveUser(user.id);
                    await loadAndRenderUsers(tbody, errorBox);
                } catch (err) {
                    console.error('Approve user failed:', err);
                    btn.disabled = false;

                    let message = 'Failed to approve user. Please try again.';
                    if (err instanceof HttpError && err.status === 403) {
                        message =
                            'You do not have permission to approve this user.';
                    }
                    if (
                        err instanceof HttpError &&
                        err.status === 400 &&
                        err.body?.message === 'CANNOT_APPROVE_SELF'
                    ) {
                        message = 'You cannot approve your own account.';
                    }
                    if (
                        err instanceof HttpError &&
                        err.status === 400 &&
                        err.body?.message === 'ADMIN_ALWAYS_ACTIVE'
                    ) {
                        message = 'Admin accounts do not require approval.';
                    }

                    showError(message, errorBox);
                }
            });

            actionCell.appendChild(btn);
        }

        const isActiveNonAdmin = !statusPending && role !== 'ADMIN';
        if (isAdmin && isActiveNonAdmin) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'users-btn-deactivate';
            btn.textContent = 'Deactivate';

            btn.addEventListener('click', async () => {
                btn.disabled = true;
                try {
                    await deactivateUser(user.id);
                    await loadAndRenderUsers(tbody, errorBox);
                } catch (err) {
                    console.error('Deactivate user failed:', err);
                    btn.disabled = false;

                    let message =
                        'Failed to change user status. Please try again.';
                    if (err instanceof HttpError && err.status === 403) {
                        message =
                            'You do not have permission to deactivate this user.';
                    }
                    if (
                        err instanceof HttpError &&
                        err.status === 400 &&
                        err.body?.message === 'CANNOT_CHANGE_SELF_STATUS'
                    ) {
                        message = 'You cannot deactivate your own account.';
                    }
                    if (
                        err instanceof HttpError &&
                        err.status === 400 &&
                        err.body?.message === 'ADMIN_ALWAYS_ACTIVE'
                    ) {
                        message = 'Admin accounts cannot be deactivated.';
                    }

                    showError(message, errorBox);
                }
            });

            actionCell.appendChild(btn);
        }

        tbody.appendChild(tr);
    }
}

async function approveUser(id) {
    await apiRequest(`/users/${encodeURIComponent(id)}/approve`, {
        method: 'PATCH',
        withAuth: true,
    });
}

async function deactivateUser(id) {
    await apiRequest(`/users/${encodeURIComponent(id)}/deactivate`, {
        method: 'PATCH',
        withAuth: true,
    });
}

function renderLoading(tbody) {
    tbody.innerHTML = `
        <tr>
            <td colspan="5" class="users-empty">
                Loading users...
            </td>
        </tr>`;
}

function renderEmpty(tbody) {
    tbody.innerHTML = `
        <tr>
            <td colspan="5" class="users-empty">
                No users in this organisation yet.
            </td>
        </tr>`;
}

function renderErrorState(message, tbody, errorBox) {
    showError(message, errorBox);
    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="users-empty">
                    ${escapeHtml(message)}
                </td>
            </tr>`;
    }
}

function showError(message, box) {
    if (!box) return;
    box.textContent = message;
    box.hidden = false;
}

function hideError(box) {
    if (!box) return;
    box.textContent = '';
    box.hidden = true;
}

function redirectToLogin() {
    window.location.href = '/auth/login';
}

function escapeHtml(value) {
    if (value == null) return '';
    return value
        .toString()
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
