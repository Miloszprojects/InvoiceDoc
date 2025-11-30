import { apiGet, apiRequest, HttpError } from '../../shared/httpClient.js';

let contractors = [];
let currentEditingId = null;
let isLoading = false;


export function initContractors() {
    const form = document.getElementById('contractor-form');
    const searchInput = document.getElementById('contractors-search');
    const addBtn = document.getElementById('contractors-add-btn');
    const typeSelect = document.getElementById('contractor-type');
    const cancelBtn = document.getElementById('contractor-cancel-btn');
    const deleteBtn = document.getElementById('contractor-delete-btn');

    if (!form) return;

    form.addEventListener('submit', onFormSubmit);
    addBtn?.addEventListener('click', () => enterCreateMode());
    cancelBtn?.addEventListener('click', () => enterCreateMode());
    deleteBtn?.addEventListener('click', onDeleteClicked);
    typeSelect?.addEventListener('change', updateTypeSpecificUi);

    let searchTimeout = null;
    searchInput?.addEventListener('input', () => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            loadContractors(searchInput.value.trim()).catch(console.error);
        }, 250);
    });

    enterCreateMode(false);
    updateTypeSpecificUi();
    loadContractors().catch(console.error);
}

async function loadContractors(query) {
    if (isLoading) return;
    isLoading = true;

    const listEl = document.querySelector('[data-contractors-list]');
    renderListLoading(listEl);

    try {
        const url = query
            ? `/contractors?q=${encodeURIComponent(query)}`
            : '/contractors';

        const data = await apiGet(url, { withAuth: true });
        contractors = Array.isArray(data) ? data : [];
        renderContractorsList(listEl);
    } catch (err) {
        if (err instanceof HttpError) {
            if (err.status === 401) {
                window.location.href = '/auth/login';
                return;
            }
        }
        console.error('Failed to load contractors:', err);
        renderListError(listEl);
    } finally {
        isLoading = false;
    }
}

async function createContractor(payload) {
    return apiRequest('/contractors', {
        method: 'POST',
        body: payload,
        withAuth: true,
    });
}

async function updateContractor(id, payload) {
    return apiRequest(`/contractors/${encodeURIComponent(id)}`, {
        method: 'PUT',
        body: payload,
        withAuth: true,
    });
}

async function deleteContractor(id) {
    return apiRequest(`/contractors/${encodeURIComponent(id)}`, {
        method: 'DELETE',
        withAuth: true,
    });
}

function renderListLoading(listEl) {
    if (!listEl) return;
    listEl.innerHTML =
        '<p class="contractors-empty">Loading contractors...</p>';
}

function renderListError(listEl) {
    if (!listEl) return;
    listEl.innerHTML = `<p class="contractors-empty">
        Unable to load contractors. Please refresh the page.
    </p>`;
}

function renderContractorsList(listEl) {
    if (!listEl) return;

    if (!contractors.length) {
        listEl.innerHTML = `<p class="contractors-empty">
            No contractors yet. Add your first client or supplier on the right.
        </p>`;
        return;
    }

    listEl.innerHTML = '';

    contractors.forEach((c) => {
        const row = document.createElement('div');
        row.className = 'contractors-row';
        if (c.id === currentEditingId) {
            row.classList.add('contractors-row--selected');
        }

        const typeLabel =
            c.type === 'PERSON'
                ? '<span class="contractors-pill contractors-pill--person">Person</span>'
                : '<span class="contractors-pill contractors-pill--company">Company</span>';

        const idValue = c.nip || c.pesel || '';

        row.innerHTML = `
            <div class="contractors-row-name">${escapeHtml(c.name)}</div>
            <div>${typeLabel}</div>
            <div class="contractors-row-id">${escapeHtml(idValue)}</div>
            <div class="contractors-row-city">${escapeHtml(c.address?.city ?? '')}</div>
            <div class="contractors-row-fav">
                <span class="contractors-star ${
            c.favorite ? 'contractors-star--active' : ''
        }">★</span>
            </div>
        `;

        row.addEventListener('click', () => onRowSelected(c.id));

        listEl.appendChild(row);
    });
}

function enterCreateMode(clearMessages = true) {
    currentEditingId = null;

    const titleEl = document.querySelector('[data-form-title]');
    const subtitleEl = document.querySelector('[data-form-subtitle]');
    const saveBtn = document.getElementById('contractor-save-btn');
    const cancelBtn = document.getElementById('contractor-cancel-btn');
    const deleteBtn = document.getElementById('contractor-delete-btn');

    if (titleEl) titleEl.textContent = 'Add contractor';
    if (subtitleEl) {
        subtitleEl.textContent =
            'Fill in the details to add a new client or supplier to this organisation.';
    }

    if (saveBtn) saveBtn.textContent = 'Save contractor';
    if (cancelBtn) cancelBtn.hidden = true;
    if (deleteBtn) deleteBtn.hidden = true;

    clearForm(clearMessages);

    const listEl = document.querySelector('[data-contractors-list]');
    listEl
        ?.querySelectorAll('.contractors-row--selected')
        .forEach((el) => el.classList.remove('contractors-row--selected'));
}

function enterEditMode(contractor) {
    currentEditingId = contractor.id;

    const titleEl = document.querySelector('[data-form-title]');
    const subtitleEl = document.querySelector('[data-form-subtitle]');
    const saveBtn = document.getElementById('contractor-save-btn');
    const cancelBtn = document.getElementById('contractor-cancel-btn');
    const deleteBtn = document.getElementById('contractor-delete-btn');

    if (titleEl) titleEl.textContent = 'Edit contractor';
    if (subtitleEl) {
        subtitleEl.textContent =
            'Update details for this contractor or mark them as favourite.';
    }

    if (saveBtn) saveBtn.textContent = 'Save changes';
    if (cancelBtn) cancelBtn.hidden = false;
    if (deleteBtn) deleteBtn.hidden = false;

    fillForm(contractor, false);

    const listEl = document.querySelector('[data-contractors-list]');
    listEl?.querySelectorAll('.contractors-row').forEach((row, index) => {
        if (contractors[index]?.id === contractor.id) {
            row.classList.add('contractors-row--selected');
        } else {
            row.classList.remove('contractors-row--selected');
        }
    });
}

function fillForm(contractor, clearMessages = true) {
    const {
        type,
        name,
        nip,
        pesel,
        address = {},
        email,
        phone,
        favorite,
    } = contractor;

    document.getElementById('contractor-type').value = type || 'COMPANY';
    document.getElementById('contractor-name').value = name || '';
    document.getElementById('contractor-nip').value = nip || '';
    document.getElementById('contractor-pesel').value = pesel || '';
    document.getElementById('contractor-street').value = address.street || '';
    document.getElementById('contractor-building').value =
        address.buildingNumber || ''; // <--- tu
    document.getElementById('contractor-postal').value =
        address.postalCode || '';
    document.getElementById('contractor-city').value = address.city || '';
    document.getElementById('contractor-country').value =
        address.country || '';
    document.getElementById('contractor-email').value = email || '';
    document.getElementById('contractor-phone').value = phone || '';
    document.getElementById('contractor-favorite').checked =
        Boolean(favorite);

    updateTypeSpecificUi();
    clearFieldErrors();
    if (clearMessages) clearMessagesBox();
}

function clearForm(clearMessages = true) {
    fillForm(
        {
            type: 'COMPANY',
            name: '',
            nip: '',
            pesel: '',
            address: {
                street: '',
                buildingNumber: '',
                apartmentNumber: '',
                postalCode: '',
                city: '',
                country: '',
            },
            email: '',
            phone: '',
            favorite: false,
        },
        clearMessages,
    );
}

function readFormValue() {
    const type = document.getElementById('contractor-type').value;

    return {
        type,
        name: document.getElementById('contractor-name').value.trim(),
        nip: document.getElementById('contractor-nip').value.trim() || null,
        pesel: document.getElementById('contractor-pesel').value.trim() || null,
        address: {
            street: document.getElementById('contractor-street').value.trim(),
            buildingNumber:
                document.getElementById('contractor-building').value.trim() ||
                null,
            apartmentNumber: null,
            postalCode: document
                .getElementById('contractor-postal')
                .value.trim(),
            city: document.getElementById('contractor-city').value.trim(),
            country: document
                .getElementById('contractor-country')
                .value.trim(),
        },
        email:
            document.getElementById('contractor-email').value.trim() || null,
        phone:
            document.getElementById('contractor-phone').value.trim() || null,
        favorite: document.getElementById('contractor-favorite').checked,
    };
}

function validateContractor(c) {
    const errors = [];
    const invalidIds = new Set();

    if (!c.name) {
        errors.push('Name / company name is required.');
        invalidIds.add('contractor-name');
    }

    if (!c.address.street) {
        errors.push('Street is required.');
        invalidIds.add('contractor-street');
    }
    if (!c.address.postalCode) {
        errors.push('Postal code is required.');
        invalidIds.add('contractor-postal');
    }
    if (!c.address.city) {
        errors.push('City is required.');
        invalidIds.add('contractor-city');
    }
    if (!c.address.country) {
        errors.push('Country is required.');
        invalidIds.add('contractor-country');
    }

    const nipValue = c.nip || '';
    const nipDigits = nipValue.replace(/\D/g, '');

    if (c.type === 'COMPANY') {
        if (!nipValue) {
            errors.push('NIP is required for companies.');
            invalidIds.add('contractor-nip');
        } else if (nipDigits.length !== 10) {
            errors.push('NIP must contain exactly 10 digits.');
            invalidIds.add('contractor-nip');
        }
    } else {
        if (nipValue && nipDigits.length !== 10) {
            errors.push('If provided, NIP must contain exactly 10 digits.');
            invalidIds.add('contractor-nip');
        }
    }

    return { errors, invalidIds };
}

function clearFieldErrors() {
    document
        .querySelectorAll('.contractors-input.field--invalid')
        .forEach((el) => el.classList.remove('field--invalid'));
}

function showValidationErrors(invalidIds) {
    invalidIds.forEach((id) => {
        const el = document.getElementById(id);
        if (el) el.classList.add('field--invalid');
    });
}

function clearMessagesBox() {
    const err = document.getElementById('contractor-error');
    const ok = document.getElementById('contractor-success');
    if (err) {
        err.hidden = true;
        err.textContent = '';
    }
    if (ok) {
        ok.hidden = true;
        ok.textContent = '';
    }
}

function showErrorMessage(msg) {
    const err = document.getElementById('contractor-error');
    const ok = document.getElementById('contractor-success');
    if (ok) {
        ok.hidden = true;
        ok.textContent = '';
    }
    if (err) {
        err.hidden = false;
        err.textContent = msg;
    }
}

function showSuccessMessage(msg) {
    const err = document.getElementById('contractor-error');
    const ok = document.getElementById('contractor-success');
    if (err) {
        err.hidden = true;
        err.textContent = '';
    }
    if (ok) {
        ok.hidden = false;
        ok.textContent = msg;
    }
}

async function onFormSubmit(e) {
    e.preventDefault();
    clearFieldErrors();
    clearMessagesBox();

    const payload = readFormValue();
    const { errors, invalidIds } = validateContractor(payload);

    if (errors.length) {
        showValidationErrors(invalidIds);
        showErrorMessage(errors.join(' '));
        return;
    }

    try {
        if (currentEditingId == null) {
            const created = await createContractor(payload);
            showSuccessMessage('Contractor saved.');
            await loadContractors();

            const createdId = created?.id;
            if (createdId != null) {
                const found = contractors.find((c) => c.id === createdId);
                if (found) {
                    enterEditMode(found);
                } else {
                    enterCreateMode(false);
                }
            } else {
                enterCreateMode(false);
            }
        } else {
            await updateContractor(currentEditingId, payload);
            showSuccessMessage('Changes saved.');
            await loadContractors();

            const updated = contractors.find(
                (c) => c.id === currentEditingId,
            );
            if (updated) {
                enterEditMode(updated);
            } else {
                enterCreateMode(false);
            }
        }
    } catch (err) {
        console.error('Save contractor failed:', err);

        if (err instanceof HttpError) {
            if (err.status === 400) {
                showErrorMessage(
                    'Some of the contractor data is invalid. Please check the fields and try again.',
                );
                return;
            }

            if (err.status === 401) {
                window.location.href = '/auth/login';
                return;
            }

            if (err.status === 403) {
                showErrorMessage(
                    'Only Owners or Admins can create or edit contractors in this organisation.',
                );
                return;
            }
        }

        showErrorMessage(
            'Unable to save contractor right now. Please try again in a moment.',
        );
    }
}

async function onDeleteClicked() {
    if (currentEditingId == null) return;

    const confirmed = window.confirm(
        'Are you sure you want to delete this contractor? This cannot be undone.',
    );
    if (!confirmed) return;

    try {
        await deleteContractor(currentEditingId);
        showSuccessMessage('Contractor removed.');
        currentEditingId = null;
        await loadContractors();
        enterCreateMode(false);
    } catch (err) {
        console.error('Delete contractor failed:', err);

        if (err instanceof HttpError) {
            if (err.status === 401) {
                window.location.href = '/auth/login';
                return;
            }

            if (err.status === 403) {
                showErrorMessage(
                    'Only Owners or Admins can delete contractors in this organisation.',
                );
                return;
            }
        }

        showErrorMessage(
            'Unable to delete this contractor right now. Please try again later.',
        );
    }
}

function onRowSelected(id) {
    const contractor = contractors.find((c) => c.id === id);
    if (!contractor) return;
    enterEditMode(contractor);
}

function updateTypeSpecificUi() {
    const type = document.getElementById('contractor-type')?.value || 'COMPANY';
    const nipRequired = document.querySelector(
        '[data-nip-required-indicator]',
    );
    const nipOptional = document.querySelector(
        '[data-nip-optional-indicator]',
    );

    if (type === 'COMPANY') {
        if (nipRequired) nipRequired.style.display = 'inline';
        if (nipOptional) nipOptional.style.display = 'none';
    } else {
        if (nipRequired) nipRequired.style.display = 'none';
        if (nipOptional) nipOptional.style.display = 'inline';
    }
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
