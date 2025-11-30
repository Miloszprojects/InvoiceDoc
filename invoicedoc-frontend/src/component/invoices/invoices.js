import { apiGet, apiRequest, HttpError } from '../../shared/httpClient.js';
import { getToken } from '../../shared/auth/tokenStorage.js';
import { API_ROOT, REQUEST_TIMEOUT_MS } from '../../shared/config.js';
import { fetchCurrentUserOrNull } from '../../shared/authApi.js';

let sellerProfiles = [];
let contractors = [];
let invoicesPage = null;
let currentUserRole = null;

export async function initInvoices() {
    const form = document.getElementById('invoice-form');
    const addItemBtn = document.getElementById('invoice-add-item-btn');
    const importBtn = document.getElementById('invoice-import-btn');

    try {
        const user = await fetchCurrentUserOrNull();
        if (!user) {
            window.location.href = '/auth/login';
            return;
        }
        currentUserRole = user.role || 'OWNER';
    } catch (err) {
        console.error('Failed to load current user on invoices page:', err);
        window.location.href = '/auth/login';
        return;
    }

    const isAccountant = currentUserRole === 'ACCOUNTANT';

    if (isAccountant) {
        if (form) {
            form.remove();
        }
        try {
            await loadInvoices();
        } catch (err) {
            console.error('Failed to init invoices for accountant:', err);
        }
        return;
    }

    if (!form) return;

    form.addEventListener('submit', onFormSubmit);
    addItemBtn?.addEventListener('click', addItemRow);
    importBtn?.addEventListener('click', onImportJsonClick);

    initDefaultDates();

    try {
        await Promise.all([loadSellerProfiles(), loadContractors(), loadInvoices()]);
    } catch (err) {
        console.error('Failed to init invoices:', err);
    }

    if (!document.querySelector('[data-items-body] .inv-items__row')) {
        addItemRow();
    }
}

async function loadSellerProfiles() {
    try {
        const data = await apiGet('/seller-profiles', { withAuth: true });
        sellerProfiles = Array.isArray(data) ? data : [];
        fillSellerSelect();
    } catch (err) {
        console.error('Failed to load seller profiles:', err);
    }
}

async function loadContractors() {
    try {
        const data = await apiGet('/contractors', { withAuth: true });
        contractors = Array.isArray(data) ? data : [];
        fillBuyerSelect();
    } catch (err) {
        console.error('Failed to load contractors:', err);
    }
}

async function loadInvoices() {
    const listEl = document.querySelector('[data-invoices-list]');
    if (!listEl) return;

    listEl.innerHTML = '<p class="inv-list-empty">Loading invoices...</p>';

    try {
        const data = await apiGet('/invoices?page=0&size=20', {
            withAuth: true,
        });
        invoicesPage = data;
        renderInvoicesList(listEl);
    } catch (err) {
        if (err instanceof HttpError && (err.status === 401 || err.status === 403)) {
            window.location.href = '/auth/login';
            return;
        }
        console.error('Failed to load invoices:', err);
        listEl.innerHTML = `<p class="inv-list-empty">
            Unable to load invoices. Please refresh the page.
        </p>`;
    }
}

async function createInvoice(payload) {
    return apiRequest('/invoices', {
        method: 'POST',
        body: payload,
        withAuth: true,
    });
}

async function downloadInvoicePdf(id) {
    const token = getToken();

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

    const res = await fetch(`${API_ROOT}/invoices/${encodeURIComponent(id)}/pdf`, {
        method: 'GET',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        credentials: 'same-origin',
        signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
            window.location.href = '/auth/login';
        }
        throw new Error(`PDF download failed with status ${res.status}`);
    }

    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `invoice-${id}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
}

async function deleteInvoice(id) {
    return apiRequest(`/invoices/${encodeURIComponent(id)}`, {
        method: 'DELETE',
        withAuth: true,
    });
}

async function onImportJsonClick() {
    clearFieldErrors();
    clearMessages();

    const fileInput = document.getElementById('invoice-import-file');
    const file = fileInput?.files?.[0];

    if (!file) {
        showError('Please choose a JSON file to import.');
        return;
    }

    let raw;
    try {
        raw = await file.text();
    } catch (err) {
        console.error('Failed to read file:', err);
        showError('Unable to read selected file.');
        return;
    } finally {
        fileInput.value = '';
    }

    let data;
    try {
        data = JSON.parse(raw);
    } catch (err) {
        console.error('Invalid JSON:', err);
        showError('Selected file is not a valid JSON.');
        return;
    }

    if (!data || typeof data !== 'object') {
        showError('Import JSON must be an object.');
        return;
    }

    const { sellerProfile, contractor, invoice } = data;

    if (!sellerProfile || !contractor || !invoice) {
        showError(
            'Import JSON must contain "sellerProfile", "contractor" and "invoice" objects.',
        );
        return;
    }

    try {
        const createdSeller = await apiRequest('/seller-profiles', {
            method: 'POST',
            body: sellerProfile,
            withAuth: true,
        });

        const createdContractor = await apiRequest('/contractors', {
            method: 'POST',
            body: contractor,
            withAuth: true,
        });

        if (createdSeller) {
            sellerProfiles.push(createdSeller);
        }
        if (createdContractor) {
            contractors.push(createdContractor);
        }

        fillSellerSelect();
        fillBuyerSelect();

        const sellerSelect = document.getElementById('invoice-seller');
        const buyerSelect = document.getElementById('invoice-buyer');

        if (sellerSelect && createdSeller?.id != null) {
            sellerSelect.value = String(createdSeller.id);
            onSellerChanged();
        }
        if (buyerSelect && createdContractor?.id != null) {
            buyerSelect.value = String(createdContractor.id);
        }

        applyImportedInvoice(invoice);

        showSuccess(
            'Import completed. Seller profile, contractor and invoice fields have been pre-filled.',
        );
    } catch (err) {
        console.error('Import JSON failed:', err);

        if (err instanceof HttpError) {
            if (err.status === 401 || err.status === 403) {
                window.location.href = '/auth/login';
                return;
            }
        }

        showError(
            'Unable to import JSON. Please check file contents and try again.',
        );
    }
}

function applyImportedInvoice(inv) {
    if (!inv || typeof inv !== 'object') return;

    const issueInput = document.getElementById('invoice-issue-date');
    const saleInput = document.getElementById('invoice-sale-date');
    const dueInput = document.getElementById('invoice-due-date');
    const paymentSelect = document.getElementById('invoice-payment-method');
    const currencyInput = document.getElementById('invoice-currency');
    const notesInput = document.getElementById('invoice-notes');
    const reverseCheckbox = document.getElementById('invoice-reverse-charge');
    const splitCheckbox = document.getElementById('invoice-split-payment');

    if (issueInput && inv.issueDate) issueInput.value = inv.issueDate;
    if (saleInput && inv.saleDate) saleInput.value = inv.saleDate;
    if (dueInput && inv.dueDate) dueInput.value = inv.dueDate;

    if (paymentSelect && inv.paymentMethod) {
        paymentSelect.value = inv.paymentMethod;
    }

    if (currencyInput && inv.currency) {
        currencyInput.value = inv.currency;
    }

    if (notesInput && typeof inv.notes === 'string') {
        notesInput.value = inv.notes;
    }

    if (reverseCheckbox) {
        reverseCheckbox.checked = Boolean(inv.reverseCharge);
    }
    if (splitCheckbox) {
        splitCheckbox.checked = Boolean(inv.splitPayment);
    }

    const body = document.querySelector('[data-items-body]');
    if (!body) return;

    body.innerHTML = '';

    const items = Array.isArray(inv.items) ? inv.items : [];
    if (!items.length) {
        addItemRow();
        recalcTotals();
        return;
    }

    items.forEach((item) => {
        const row = document.createElement('div');
        row.className = 'inv-items__row';

        row.innerHTML = `
            <input type="text" data-item-desc placeholder="Item description" />
            <input type="number" step="0.0001" min="0" data-item-qty />
            <input type="text" data-item-unit placeholder="pcs" />
            <input type="number" step="0.01" min="0" data-item-net />
            <input type="text" data-item-vat placeholder="23" />
            <button type="button" class="inv-items__delete-btn">Remove</button>
        `;

        const descInput = row.querySelector('[data-item-desc]');
        const qtyInput = row.querySelector('[data-item-qty]');
        const unitInput = row.querySelector('[data-item-unit]');
        const netInput = row.querySelector('[data-item-net]');
        const vatInput = row.querySelector('[data-item-vat]');
        const deleteBtn = row.querySelector('.inv-items__delete-btn');

        if (descInput) descInput.value = item.description || '';
        if (qtyInput && item.quantity != null)
            qtyInput.value = String(item.quantity);
        if (unitInput) unitInput.value = item.unit || '';
        if (netInput && item.netUnitPrice != null)
            netInput.value = String(item.netUnitPrice);
        if (vatInput) vatInput.value = item.vatRate || '';

        deleteBtn?.addEventListener('click', () => {
            row.remove();
            recalcTotals();
        });

        row.querySelectorAll('input').forEach((input) => {
            input.addEventListener('input', recalcTotals);
        });

        body.appendChild(row);
    });

    recalcTotals();
}

function renderInvoicesList(listEl) {
    if (
        !invoicesPage ||
        !Array.isArray(invoicesPage.content) ||
        !invoicesPage.content.length
    ) {
        listEl.innerHTML = `<p class="inv-list-empty">
            No invoices yet. ${
            currentUserRole === 'ACCOUNTANT'
                ? 'Ask an owner or admin to issue invoices for this organisation.'
                : 'Create your first invoice on the right.'
        }
        </p>`;
        return;
    }

    listEl.innerHTML = '';

    const isAdmin = currentUserRole === 'ADMIN';

    invoicesPage.content.forEach((inv) => {
        const row = document.createElement('div');
        row.className = 'inv-list-row';

        const number = inv.number || '-';
        const buyer = inv.buyerName || '';
        const date = inv.issueDate || '';
        const total =
            inv.totalGross != null ? inv.totalGross.toFixed(2) : '0.00';
        const currency = inv.currency || '';

        const statusText = inv.status || 'DRAFT';

        row.innerHTML = `
            <div class="inv-list-number">${escapeHtml(number)}</div>
            <div class="inv-list-buyer">${escapeHtml(buyer)}</div>
            <div class="inv-list-date">${escapeHtml(date)}</div>
            <div class="inv-list-total">${escapeHtml(total)} ${escapeHtml(
            currency,
        )}</div>
            <div class="inv-list-status">${escapeHtml(statusText)}</div>
            <div class="inv-list-actions">
                <button type="button" class="inv-list-pdf-btn">PDF</button>
                ${
            isAdmin
                ? '<button type="button" class="inv-list-delete-btn">Delete</button>'
                : ''
        }
            </div>
        `;

        const pdfBtn = row.querySelector('.inv-list-pdf-btn');
        pdfBtn?.addEventListener('click', async (e) => {
            e.stopPropagation();
            try {
                await downloadInvoicePdf(inv.id);
            } catch (err) {
                console.error('PDF download error:', err);
                showError(
                    'Unable to download invoice PDF. Please try again later.',
                );
            }
        });

        if (isAdmin) {
            const deleteBtn = row.querySelector('.inv-list-delete-btn');
            deleteBtn?.addEventListener('click', async (e) => {
                e.stopPropagation();
                if (!confirm('Are you sure you want to delete this invoice?')) {
                    return;
                }
                try {
                    await deleteInvoice(inv.id);
                    showSuccess(`Invoice ${inv.number || ''} deleted.`);
                    await loadInvoices();
                } catch (err) {
                    console.error('Delete invoice error:', err);
                    if (
                        err instanceof HttpError &&
                        (err.status === 401 || err.status === 403)
                    ) {
                        window.location.href = '/auth/login';
                        return;
                    }
                    showError(
                        'Unable to delete invoice right now. Please try again later.',
                    );
                }
            });
        }

        listEl.appendChild(row);
    });
}

function fillSellerSelect() {
    const select = document.getElementById('invoice-seller');
    if (!select) return;

    while (select.options.length > 1) {
        select.remove(1);
    }

    sellerProfiles.forEach((p) => {
        const opt = document.createElement('option');
        opt.value = String(p.id);
        opt.textContent = `${p.name} · ${p.nip}`;
        select.appendChild(opt);
    });

    select.removeEventListener('change', onSellerChanged);
    select.addEventListener('change', onSellerChanged);
}

function fillBuyerSelect() {
    const select = document.getElementById('invoice-buyer');
    if (!select) return;

    while (select.options.length > 1) {
        select.remove(1);
    }

    contractors.forEach((c) => {
        const opt = document.createElement('option');
        opt.value = String(c.id);
        opt.textContent = c.name;
        select.appendChild(opt);
    });
}

function initDefaultDates() {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    const todayStr = `${yyyy}-${mm}-${dd}`;

    const issueInput = document.getElementById('invoice-issue-date');
    const saleInput = document.getElementById('invoice-sale-date');
    const dueInput = document.getElementById('invoice-due-date');

    if (issueInput) issueInput.value = todayStr;
    if (saleInput) saleInput.value = todayStr;

    const dueDate = new Date(today);
    dueDate.setDate(dueDate.getDate() + 14);
    const dueStr = `${dueDate.getFullYear()}-${String(
        dueDate.getMonth() + 1,
    ).padStart(2, '0')}-${String(dueDate.getDate()).padStart(2, '0')}`;
    if (dueInput) dueInput.value = dueStr;
}

function onSellerChanged() {
    const sellerSelect = document.getElementById('invoice-seller');
    const currencyInput = document.getElementById('invoice-currency');
    const dueInput = document.getElementById('invoice-due-date');
    const issueInput = document.getElementById('invoice-issue-date');

    const id = sellerSelect?.value
        ? Number.parseInt(sellerSelect.value, 10)
        : null;
    const profile = sellerProfiles.find((p) => p.id === id);
    if (!profile) return;

    if (currencyInput && profile.defaultCurrency) {
        currencyInput.value = profile.defaultCurrency;
    }

    if (dueInput && issueInput && profile.defaultPaymentTermDays != null) {
        const issueDateStr = issueInput.value;
        if (issueDateStr) {
            const d = new Date(issueDateStr);
            d.setDate(d.getDate() + profile.defaultPaymentTermDays);
            const dueStr = `${d.getFullYear()}-${String(
                d.getMonth() + 1,
            ).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
            dueInput.value = dueStr;
        }
    }
}

function addItemRow() {
    const body = document.querySelector('[data-items-body]');
    if (!body) return;

    const row = document.createElement('div');
    row.className = 'inv-items__row';

    row.innerHTML = `
        <input type="text" data-item-desc placeholder="Item description" />
        <input type="number" step="0.0001" min="0" data-item-qty />
        <input type="text" data-item-unit placeholder="pcs" />
        <input type="number" step="0.01" min="0" data-item-net />
        <input type="text" data-item-vat placeholder="23" />
        <button type="button" class="inv-items__delete-btn">Remove</button>
    `;

    const deleteBtn = row.querySelector('.inv-items__delete-btn');
    deleteBtn?.addEventListener('click', () => {
        row.remove();
        recalcTotals();
    });

    row.querySelectorAll('input').forEach((input) => {
        input.addEventListener('input', recalcTotals);
    });

    body.appendChild(row);
}

function readItems() {
    const rows = document.querySelectorAll('[data-items-body] .inv-items__row');
    const items = [];

    rows.forEach((row) => {
        const desc = row.querySelector('[data-item-desc]')?.value.trim() || '';
        const qtyStr = row.querySelector('[data-item-qty]')?.value || '';
        const unit = row.querySelector('[data-item-unit]')?.value.trim() || null;
        const netStr = row.querySelector('[data-item-net]')?.value || '';
        const vatStr = row.querySelector('[data-item-vat]')?.value.trim() || '';

        if (!desc && !qtyStr && !netStr) {
            return;
        }

        const quantity = qtyStr ? Number.parseFloat(qtyStr) : 0;
        const netUnit = netStr ? Number.parseFloat(netStr) : 0;

        items.push({
            description: desc,
            quantity: isNaN(quantity) ? 0 : quantity,
            unit,
            netUnitPrice: isNaN(netUnit) ? 0 : netUnit,
            vatRate: vatStr || '0',
        });
    });

    return items;
}

function recalcTotals() {
    const items = readItems();

    let totalNet = 0;
    let totalVat = 0;
    let totalGross = 0;

    items.forEach((item) => {
        const netTotal = item.netUnitPrice * item.quantity;
        const rateNum = Number.parseFloat(item.vatRate);
        const vatAmount = isNaN(rateNum) ? 0 : (netTotal * rateNum) / 100;
        const grossTotal = netTotal + vatAmount;

        totalNet += netTotal;
        totalVat += vatAmount;
        totalGross += grossTotal;
    });

    const netEl = document.querySelector('[data-total-net]');
    const vatEl = document.querySelector('[data-total-vat]');
    const grossEl = document.querySelector('[data-total-gross]');

    if (netEl) netEl.textContent = totalNet.toFixed(2);
    if (vatEl) vatEl.textContent = totalVat.toFixed(2);
    if (grossEl) grossEl.textContent = totalGross.toFixed(2);
}

function readForm() {
    const sellerIdStr =
        document.getElementById('invoice-seller')?.value || '';
    const buyerIdStr = document.getElementById('invoice-buyer')?.value || '';

    const issueDate =
        document.getElementById('invoice-issue-date')?.value || null;
    const saleDate =
        document.getElementById('invoice-sale-date')?.value || null;
    const dueDate = document.getElementById('invoice-due-date')?.value || null;

    const paymentMethod =
        document.getElementById('invoice-payment-method')?.value ||
        'BANK_TRANSFER';
    const currency =
        document.getElementById('invoice-currency')?.value.trim() || 'PLN';

    const notes =
        document.getElementById('invoice-notes')?.value.trim() || null;
    const reverseCharge =
        document.getElementById('invoice-reverse-charge')?.checked || false;
    const splitPayment =
        document.getElementById('invoice-split-payment')?.checked || false;

    const items = readItems();

    return {
        sellerProfileId: sellerIdStr
            ? Number.parseInt(sellerIdStr, 10)
            : null,
        contractorId: buyerIdStr ? Number.parseInt(buyerIdStr, 10) : null,
        buyerNameOverride: null,
        buyerNipOverride: null,
        buyerPeselOverride: null,
        issueDate: issueDate || null,
        saleDate: saleDate || null,
        dueDate: dueDate || null,
        paymentMethod,
        currency,
        notes,
        reverseCharge,
        splitPayment,
        items: items.map((i) => ({
            description: i.description,
            quantity: i.quantity,
            unit: i.unit,
            netUnitPrice: i.netUnitPrice,
            vatRate: i.vatRate,
        })),
    };
}

function validate(payload) {
    const errors = [];
    const invalid = new Set();

    if (!payload.sellerProfileId) {
        errors.push('Seller profile is required.');
        invalid.add('invoice-seller');
    }

    if (!payload.contractorId) {
        errors.push('Buyer (contractor) is required.');
        invalid.add('invoice-buyer');
    }

    if (!payload.issueDate) {
        errors.push('Issue date is required.');
        invalid.add('invoice-issue-date');
    }
    if (!payload.saleDate) {
        errors.push('Sale date is required.');
        invalid.add('invoice-sale-date');
    }
    if (!payload.dueDate) {
        errors.push('Due date is required.');
        invalid.add('invoice-due-date');
    }

    if (!payload.currency) {
        errors.push('Currency is required.');
        invalid.add('invoice-currency');
    }

    if (!payload.items || payload.items.length === 0) {
        errors.push('At least one line item is required.');
    } else {
        let firstItemInvalid = false;
        payload.items.forEach((item) => {
            if (!item.description) firstItemInvalid = true;
            if (!item.quantity || item.quantity <= 0) firstItemInvalid = true;
            if (!item.netUnitPrice || item.netUnitPrice <= 0)
                firstItemInvalid = true;
        });
        if (firstItemInvalid) {
            errors.push(
                'Each item must have description, quantity and net unit price.',
            );
        }
    }

    return { errors, invalid };
}

function clearFieldErrors() {
    document
        .querySelectorAll('.inv-input.inv-input--invalid')
        .forEach((el) => el.classList.remove('inv-input--invalid'));
}

function clearMessages() {
    const err = document.getElementById('invoice-error');
    const ok = document.getElementById('invoice-success');
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
    const err = document.getElementById('invoice-error');
    const ok = document.getElementById('invoice-success');
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
    const err = document.getElementById('invoice-error');
    const ok = document.getElementById('invoice-success');
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
    clearMessages();

    const payload = readForm();
    const { errors, invalid } = validate(payload);

    if (errors.length) {
        invalid.forEach((id) =>
            document.getElementById(id)?.classList.add('inv-input--invalid'),
        );
        showError(errors.join(' '));
        return;
    }

    try {
        const created = await createInvoice(payload);
        showSuccess(`Invoice ${created.number || ''} created.`);
        await loadInvoices();
    } catch (err) {
        console.error('Create invoice failed:', err);

        if (err instanceof HttpError) {
            if (err.status === 400) {
                showError(
                    'Some of the invoice data is invalid. Please check the fields and try again.',
                );
                return;
            }
            if (err.status === 401 || err.status === 403) {
                window.location.href = '/auth/login';
                return;
            }
        }

        showError('Unable to create invoice right now. Please try again later.');
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
