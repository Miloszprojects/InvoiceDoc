import { apiGet, apiRequest, HttpError } from '../../shared/httpClient.js';
import { fetchCurrentUserOrNull } from '../../shared/authApi.js';

let allProfiles = [];
let profiles = [];
let canManageProfiles = false;
let currentEditingId = null;

export async function initOrgSettings() {
    const form = document.getElementById('seller-form');
    if (!form) return;

    form.addEventListener('submit', onFormSubmit);

    try {
        const user = await fetchCurrentUserOrNull();
        if (!user) {
            window.location.href = '/auth/login';
            return;
        }

        const role =
            typeof user.role === 'string'
                ? user.role
                : user.role?.name ?? '';

        canManageProfiles = role === 'OWNER' || role === 'ADMIN';

        const ownerInfo = document.getElementById('seller-owner-info');
        const saveBtn = document.getElementById('seller-save-btn');

        if (!canManageProfiles) {
            if (ownerInfo) ownerInfo.hidden = false;
            if (saveBtn) {
                saveBtn.disabled = true;
                saveBtn.classList.add('btn--disabled');
            }
        }
    } catch (err) {
        console.error('Failed to fetch current user:', err);
        window.location.href = '/auth/login';
        return;
    }

    const searchInput = document.getElementById('seller-search');
    if (searchInput) {
        let debounceId = null;
        searchInput.addEventListener('input', () => {
            clearTimeout(debounceId);
            debounceId = setTimeout(() => {
                applySearchFilter(searchInput.value);
            }, 200);
        });
    }

    document.addEventListener('click', (e) => {
        const formEl = document.getElementById('seller-form');
        const listEl = document.querySelector('[data-seller-list]');

        const clickedInsideForm = formEl && formEl.contains(e.target);
        const clickedInsideList = listEl && listEl.contains(e.target);
        const clickedOnRow = e.target.closest('.org-list-row');

        if (clickedOnRow) return;

        if ((!clickedInsideForm && clickedInsideList) || (!clickedInsideForm && !clickedInsideList)) {
            if (currentEditingId != null) {
                enterCreateMode();
            }
        }
    });

    await loadProfiles();
    enterCreateMode();
}

async function loadProfiles() {
    const listEl = document.querySelector('[data-seller-list]');
    if (!listEl) return;

    listEl.innerHTML =
        '<p class="org-list-empty">Loading seller profiles...</p>';

    try {
        const data = await apiGet('/seller-profiles', { withAuth: true });
        allProfiles = Array.isArray(data) ? data : [];
        profiles = allProfiles.slice();
        renderProfiles(listEl);
    } catch (err) {
        if (err instanceof HttpError && (err.status === 401 || err.status === 403)) {
            window.location.href = '/auth/login';
            return;
        }
        console.error('Failed to load seller profiles:', err);
        listEl.innerHTML = `<p class="org-list-empty">
            Unable to load seller profiles. Please refresh the page.
        </p>`;
    }
}

async function createProfile(payload) {
    return apiRequest('/seller-profiles', {
        method: 'POST',
        body: payload,
        withAuth: true,
    });
}

async function updateProfile(id, payload) {
    return apiRequest(`/seller-profiles/${encodeURIComponent(id)}`, {
        method: 'PUT',
        body: payload,
        withAuth: true,
    });
}

async function deleteProfile(id) {
    return apiRequest(`/seller-profiles/${encodeURIComponent(id)}`, {
        method: 'DELETE',
        withAuth: true,
    });
}

function applySearchFilter(query) {
    const listEl = document.querySelector('[data-seller-list]');
    if (!listEl) return;

    const q = (query || '').trim().toLowerCase();

    if (!q) {
        profiles = allProfiles.slice();
    } else {
        profiles = allProfiles.filter((p) => {
            const name = (p.name || '').toLowerCase();
            const nip = (p.nip || '').toLowerCase();
            return name.includes(q) || nip.includes(q);
        });
    }

    if (!profiles.some((p) => p.id === currentEditingId)) {
        currentEditingId = null;
        resetForm();
        clearMessages();
        clearFieldErrors();
        const title = document.querySelector('[data-form-title]');
        const subtitle = document.querySelector('[data-form-subtitle]');
        const btn = document.getElementById('seller-save-btn');
        if (title) title.textContent = 'Add seller profile';
        if (subtitle) {
            subtitle.textContent =
                'Fill in the details of your company as they should appear on invoices.';
        }
        if (btn) btn.textContent = 'Save seller profile';
    }

    renderProfiles(listEl);
}

function renderProfiles(listEl) {
    if (!profiles.length) {
        listEl.innerHTML = `<p class="org-list-empty">
            No seller profiles match your search.
        </p>`;
        return;
    }

    listEl.innerHTML = '';

    profiles.forEach((p) => {
        const row = document.createElement('div');
        row.className = 'org-list-row';

        if (p.id === currentEditingId) {
            row.classList.add('org-list-row--selected');
        }

        const city = p.address?.city ?? '';
        const nip = p.nip ?? '';
        const bankName = p.bankName ?? '';

        const metaParts = [];
        if (p.defaultCurrency) {
            metaParts.push(escapeHtml(p.defaultCurrency));
        }
        if (bankName) {
            metaParts.push(escapeHtml(bankName));
        }
        const metaText = metaParts.join(' · ');

        row.innerHTML = `
            <div class="org-list-name">${escapeHtml(p.name || '')}</div>
            <div class="org-list-nip">${escapeHtml(nip)}</div>
            <div class="org-list-city">${escapeHtml(city)}</div>
            <div class="org-list-meta">
                <span class="org-list-meta-text">
                    ${metaText}
                </span>
                ${
            canManageProfiles
                ? '<button type="button" class="org-list-delete-btn">Delete</button>'
                : ''
        }
            </div>
        `;

        if (canManageProfiles) {
            const deleteBtn = row.querySelector('.org-list-delete-btn');
            if (deleteBtn) {
                deleteBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    onDeleteProfile(p.id);
                });
            }

            row.addEventListener('click', () => {
                enterEditMode(p);
            });
        }

        listEl.appendChild(row);
    });
}

function readForm() {
    const paymentTermStr = getVal('seller-payment-term').trim();
    const paymentTerm = paymentTermStr ? Number.parseInt(paymentTermStr, 10) : null;

    return {
        name: getVal('seller-name').trim(),
        nip: getVal('seller-nip').trim(),
        regon: getVal('seller-regon').trim() || null,
        krs: getVal('seller-krs').trim() || null,
        bankName: getVal('seller-bank-name').trim(),
        bankAccount: getVal('seller-bank').trim(),
        address: {
            street: getVal('seller-street').trim(),
            buildingNumber: getVal('seller-building').trim() || null,
            apartmentNumber: null,
            postalCode: getVal('seller-postal').trim(),
            city: getVal('seller-city').trim(),
            country: getVal('seller-country').trim(),
        },
        defaultCurrency: getVal('seller-currency').trim() || 'PLN',
        defaultPaymentTermDays: Number.isNaN(paymentTerm) ? null : paymentTerm,
    };
}

function fillFormFromProfile(p) {
    setVal('seller-name', p.name || '');
    setVal('seller-nip', p.nip || '');
    setVal('seller-regon', p.regon || '');
    setVal('seller-krs', p.krs || '');
    setVal('seller-bank-name', p.bankName || '');
    setVal('seller-bank', p.bankAccount || '');
    setVal('seller-street', p.address?.street || '');
    setVal('seller-building', p.address?.buildingNumber || '');
    setVal('seller-postal', p.address?.postalCode || '');
    setVal('seller-city', p.address?.city || '');
    setVal('seller-country', p.address?.country || '');
    setVal('seller-currency', p.defaultCurrency || 'PLN');
    setVal(
        'seller-payment-term',
        p.defaultPaymentTermDays != null ? String(p.defaultPaymentTermDays) : '',
    );
}

function enterCreateMode() {
    currentEditingId = null;
    resetForm();
    clearFieldErrors();
    clearMessages();

    const title = document.querySelector('[data-form-title]');
    const subtitle = document.querySelector('[data-form-subtitle]');
    const btn = document.getElementById('seller-save-btn');

    if (title) title.textContent = 'Add seller profile';
    if (subtitle) {
        subtitle.textContent =
            'Fill in the details of your company as they should appear on invoices.';
    }
    if (btn) btn.textContent = 'Save seller profile';

    document
        .querySelectorAll('.org-list-row--selected')
        .forEach((el) => el.classList.remove('org-list-row--selected'));
}

function enterEditMode(profile) {
    currentEditingId = profile.id;
    fillFormFromProfile(profile);
    clearFieldErrors();
    clearMessages();

    const title = document.querySelector('[data-form-title]');
    const subtitle = document.querySelector('[data-form-subtitle]');
    const btn = document.getElementById('seller-save-btn');

    if (title) title.textContent = 'Edit seller profile';
    if (subtitle) {
        subtitle.textContent =
            'Update details for this seller profile. It will be used on future invoices.';
    }
    if (btn) btn.textContent = 'Save changes';

    const listEl = document.querySelector('[data-seller-list]');
    if (listEl) {
        listEl.querySelectorAll('.org-list-row').forEach((row, index) => {
            const p = profiles[index];
            if (p?.id === profile.id) {
                row.classList.add('org-list-row--selected');
            } else {
                row.classList.remove('org-list-row--selected');
            }
        });
    }
}

function validate(payload) {
    const errors = [];
    const invalid = new Set();

    if (!payload.name) {
        errors.push('Company name is required.');
        invalid.add('seller-name');
    }

    const nipValue = payload.nip || '';
    const digits = nipValue.replace(/\D/g, '');
    if (!nipValue) {
        errors.push('NIP is required.');
        invalid.add('seller-nip');
    } else if (digits.length !== 10) {
        errors.push('NIP must contain exactly 10 digits.');
        invalid.add('seller-nip');
    }

    if (!payload.bankName) {
        errors.push('Bank name is required.');
        invalid.add('seller-bank-name');
    }

    if (!payload.bankAccount) {
        errors.push('Bank account (IBAN) is required.');
        invalid.add('seller-bank');
    }

    if (!payload.address.street) {
        errors.push('Street is required.');
        invalid.add('seller-street');
    }
    if (!payload.address.postalCode) {
        errors.push('Postal code is required.');
        invalid.add('seller-postal');
    }
    if (!payload.address.city) {
        errors.push('City is required.');
        invalid.add('seller-city');
    }
    if (!payload.address.country) {
        errors.push('Country is required.');
        invalid.add('seller-country');
    }

    if (!payload.defaultCurrency) {
        errors.push('Default currency is required.');
        invalid.add('seller-currency');
    }

    if (
        payload.defaultPaymentTermDays == null ||
        Number.isNaN(payload.defaultPaymentTermDays) ||
        payload.defaultPaymentTermDays <= 0
    ) {
        errors.push('Default payment term must be a positive number of days.');
        invalid.add('seller-payment-term');
    }

    return { errors, invalid };
}

function clearFieldErrors() {
    document
        .querySelectorAll('.org-input.org-input--invalid')
        .forEach((el) => el.classList.remove('org-input--invalid'));
}

function clearMessages() {
    const err = document.getElementById('seller-error');
    const ok = document.getElementById('seller-success');
    if (err) {
        err.hidden = true;
        err.textContent = '';
    }
    if (ok) {
        ok.hidden = true;
        ok.textContent = '';
    }
}

function showError(msg) {
    const err = document.getElementById('seller-error');
    const ok = document.getElementById('seller-success');
    if (ok) {
        ok.hidden = true;
        ok.textContent = '';
    }
    if (err) {
        err.hidden = false;
        err.textContent = msg;
    }
}

function showSuccess(msg) {
    const err = document.getElementById('seller-error');
    const ok = document.getElementById('seller-success');
    if (err) {
        err.hidden = true;
        err.textContent = '';
    }
    if (ok) {
        ok.hidden = false;
        ok.textContent = msg;
    }
}

function resetForm() {
    [
        'seller-name',
        'seller-nip',
        'seller-regon',
        'seller-krs',
        'seller-bank-name',
        'seller-bank',
        'seller-street',
        'seller-building',
        'seller-postal',
        'seller-city',
        'seller-country',
        'seller-currency',
        'seller-payment-term',
    ].forEach((id) => {
        const el = document.getElementById(id);
        if (!el) return;
        if (id === 'seller-currency') el.value = 'PLN';
        else el.value = '';
    });
}

async function onFormSubmit(e) {
    e.preventDefault();
    clearFieldErrors();
    clearMessages();

    if (!canManageProfiles) {
        showError('Only Owners or Admins of this organisation can add seller profiles.');
        return;
    }

    const payload = readForm();
    const { errors, invalid } = validate(payload);

    if (errors.length) {
        invalid.forEach((id) =>
            document.getElementById(id)?.classList.add('org-input--invalid'),
        );
        showError(errors.join(' '));
        return;
    }

    try {
        if (currentEditingId == null) {
            await createProfile(payload);
            showSuccess('Seller profile saved.');
        } else {
            await updateProfile(currentEditingId, payload);
            showSuccess('Changes saved.');
        }

        await loadProfiles();

        if (currentEditingId == null) {
            resetForm();
            enterCreateMode();
        } else {
            const updated = profiles.find((p) => p.id === currentEditingId);
            if (updated) {
                enterEditMode(updated);
            } else {
                enterCreateMode();
            }
        }
    } catch (err) {
        console.error('Create/update seller profile failed:', err);

        if (err instanceof HttpError) {
            if (err.status === 400) {
                showError(
                    'Some of the seller data is invalid. Please check the fields and try again.',
                );
                return;
            }
            if (err.status === 403) {
                showError(
                    'Only Owners or Admins of this organisation can add or edit seller profiles.',
                );
                return;
            }
            if (err.status === 401) {
                window.location.href = '/auth/login';
                return;
            }
        }

        showError(
            'Unable to save seller profile right now. Please try again later.',
        );
    }
}

async function onDeleteProfile(id) {
    if (!canManageProfiles) {
        showError('Only Owners or Admins of this organisation can delete seller profiles.');
        return;
    }

    const confirmed = window.confirm(
        'Are you sure you want to delete this seller profile? This cannot be undone.',
    );
    if (!confirmed) return;

    clearMessages();

    try {
        await deleteProfile(id);
        showSuccess('Seller profile deleted.');
        currentEditingId = null;
        await loadProfiles();
        enterCreateMode();
    } catch (err) {
        console.error('Delete seller profile failed:', err);

        if (err instanceof HttpError) {
            if (err.status === 403) {
                showError(
                    'Only Owners or Admins of this organisation can delete seller profiles.',
                );
                return;
            }
            if (err.status === 401) {
                window.location.href = '/auth/login';
                return;
            }
        }

        showError(
            'Unable to delete seller profile right now. Please try again later.',
        );
    }
}

function getVal(id) {
    return document.getElementById(id)?.value || '';
}

function setVal(id, value) {
    const el = document.getElementById(id);
    if (el) el.value = value ?? '';
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
