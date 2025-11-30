import { describe, test, expect, vi, beforeEach } from 'vitest';

const apiGetMock = vi.fn();
const apiRequestMock = vi.fn();
const getTokenMock = vi.fn();
const fetchCurrentUserOrNullMock = vi.fn();

class MockHttpError extends Error {
    constructor(message, status, body) {
        super(message);
        this.name = 'HttpError';
        this.status = status;
        this.body = body;
    }
}

vi.mock('../../../src/shared/httpClient.js', () => ({
    apiGet: apiGetMock,
    apiRequest: apiRequestMock,
    HttpError: MockHttpError,
}));

vi.mock('../../../src/shared/auth/tokenStorage.js', () => ({
    getToken: getTokenMock,
}));

vi.mock('../../../src/shared/config.js', () => ({
    API_ROOT: '/v1/api',
    REQUEST_TIMEOUT_MS: 15000,
}));

vi.mock('../../../src/shared/authApi.js', () => ({
    fetchCurrentUserOrNull: fetchCurrentUserOrNullMock,
}));

let invoicesModule;

beforeEach(async () => {
    vi.resetModules();
    apiGetMock.mockReset();
    apiRequestMock.mockReset();
    fetchCurrentUserOrNullMock.mockReset();
    getTokenMock.mockReset();
    Object.defineProperty(window, 'location', {
        writable: true,
        value: { href: 'http://localhost/' },
    });

    document.body.innerHTML = `
    <main id="app">
      <div id="view-root">
        <section class="invoices">
          <header class="invoices__header">
            <h1>Invoices</h1>
            <p>Invoices description.</p>
          </header>

          <div class="invoices__layout">
            <div class="invoices__list">
              <div class="invoices__list-header">
                <h2>Recent invoices</h2>
                <p class="invoices__list-subtitle">
                  Latest invoices issued by this organisation.
                </p>
              </div>
              <div class="invoices__list-body" data-invoices-list></div>
            </div>

            <form id="invoice-form" class="invoices__form" novalidate>
              <div class="invoices__form-header">
                <h2>Create new invoice</h2>
                <p>Form subtitle</p>
              </div>

              <div class="invoices__section">
                <h3 class="invoices__section-title">Parties</h3>
                <div class="invoices__grid">
                  <div class="inv-field inv-field--full">
                    <label for="invoice-seller">
                      Seller profile <span>*</span>
                    </label>
                    <select id="invoice-seller" class="inv-input">
                      <option value="">Select seller profile…</option>
                    </select>
                  </div>

                  <div class="inv-field inv-field--full">
                    <label for="invoice-buyer">
                      Buyer (contractor) <span>*</span>
                    </label>
                    <select id="invoice-buyer" class="inv-input">
                      <option value="">Select contractor…</option>
                    </select>
                  </div>
                </div>
              </div>

              <div class="invoices__section">
                <h3 class="invoices__section-title">Dates & payment</h3>
                <div class="invoices__grid">
                  <div class="inv-field">
                    <label for="invoice-issue-date">
                      Issue date <span>*</span>
                    </label>
                    <input id="invoice-issue-date" class="inv-input" type="date" />
                  </div>

                  <div class="inv-field">
                    <label for="invoice-sale-date">
                      Sale date <span>*</span>
                    </label>
                    <input id="invoice-sale-date" class="inv-input" type="date" />
                  </div>

                  <div class="inv-field">
                    <label for="invoice-due-date">
                      Due date <span>*</span>
                    </label>
                    <input id="invoice-due-date" class="inv-input" type="date" />
                  </div>

                  <div class="inv-field">
                    <label for="invoice-payment-method">
                      Payment method <span>*</span>
                    </label>
                    <select id="invoice-payment-method" class="inv-input">
                      <option value="BANK_TRANSFER">Bank transfer</option>
                      <option value="CASH">Cash</option>
                    </select>
                  </div>

                  <div class="inv-field">
                    <label for="invoice-currency">
                      Currency <span>*</span>
                    </label>
                    <input id="invoice-currency" class="inv-input" type="text" />
                  </div>

                  <div class="inv-field inv-field--checkbox">
                    <label>
                      <input id="invoice-reverse-charge" type="checkbox" />
                      Reverse charge
                    </label>
                  </div>

                  <div class="inv-field inv-field--checkbox">
                    <label>
                      <input id="invoice-split-payment" type="checkbox" />
                      Split payment
                    </label>
                  </div>
                </div>
              </div>

              <div class="invoices__section">
                <h3 class="invoices__section-title">Line items</h3>
                <div class="inv-items">
                  <div class="inv-items__header-row">
                    <span>Description</span>
                    <span>Qty</span>
                    <span>Unit</span>
                    <span>Net unit</span>
                    <span>VAT %</span>
                    <span>Actions</span>
                  </div>
                  <div class="inv-items__body" data-items-body></div>
                  <button
                    type="button"
                    class="inv-items__add-btn"
                    id="invoice-add-item-btn"
                  >
                    + Add item
                  </button>
                </div>
              </div>

              <div class="invoices__section">
                <h3 class="invoices__section-title">Notes & totals</h3>
                <div class="invoices__grid">
                  <div class="inv-field inv-field--full">
                    <label for="invoice-notes">Notes</label>
                    <textarea
                      id="invoice-notes"
                      class="inv-input inv-input--textarea"
                    ></textarea>
                  </div>

                  <div class="inv-summary">
                    <div class="inv-summary__row">
                      <span>Net total:</span>
                      <strong data-total-net>0.00</strong>
                    </div>
                    <div class="inv-summary__row">
                      <span>VAT total:</span>
                      <strong data-total-vat>0.00</strong>
                    </div>
                    <div class="inv-summary__row inv-summary__row--highlight">
                      <span>Gross total:</span>
                      <strong data-total-gross>0.00</strong>
                    </div>
                  </div>
                </div>
              </div>

              <div class="invoices__section">
                <h3 class="invoices__section-title">Import from JSON</h3>
                <div class="inv-import">
                  <div class="inv-import__text">
                    <p>Upload JSON</p>
                    <p class="inv-import__hint">Hint</p>
                  </div>
                  <div class="inv-import__controls">
                    <label for="invoice-import-file" class="inv-import__file-label">
                      Choose JSON file
                      <input
                        type="file"
                        id="invoice-import-file"
                        accept="application/json"
                        class="inv-import__file"
                      />
                    </label>

                    <button
                      type="button"
                      class="inv-import__btn"
                      id="invoice-import-btn"
                    >
                      Import JSON
                    </button>
                  </div>
                </div>
              </div>

              <div class="invoices__footer">
                <button
                  type="submit"
                  class="btn btn-primary"
                  id="invoice-save-btn"
                >
                  Create invoice
                </button>
              </div>

              <div
                class="inv-message inv-message--error"
                id="invoice-error"
                hidden
              ></div>
              <div
                class="inv-message inv-message--success"
                id="invoice-success"
                hidden
              ></div>
            </form>
          </div>
        </section>
      </div>
    </main>
  `;

    invoicesModule = await import(
        '../../../src/component/invoices/invoices.js'
        );
});

describe('initInvoices – role-based behaviour', () => {
    test('redirects to login when fetchCurrentUserOrNull returns null', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue(null);

        await invoicesModule.initInvoices();

        expect(window.location.href).toBe('/auth/login');
    });

    test('ACCOUNTANT: removes form and only loads invoices list', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({ id: 1, role: 'ACCOUNTANT' });

        apiGetMock.mockResolvedValueOnce({
            content: [],
            pageNumber: 0,
            pageSize: 20,
            totalElements: 0,
        });

        await invoicesModule.initInvoices();

        expect(document.getElementById('invoice-form')).toBeNull();

        expect(apiGetMock).toHaveBeenCalledTimes(1);
        expect(apiGetMock).toHaveBeenCalledWith(
            '/invoices?page=0&size=20',
            { withAuth: true },
        );

        const list = document.querySelector('[data-invoices-list]');
        expect(list.innerHTML).toContain('No invoices yet');
    });

    test('OWNER: keeps form, loads sellers, contractors and invoices, and adds initial item row', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({ id: 1, role: 'OWNER' });

        apiGetMock
            .mockResolvedValueOnce([
                {
                    id: 10,
                    name: 'Seller A',
                    nip: '1234567890',
                    defaultCurrency: 'PLN',
                    defaultPaymentTermDays: 14,
                },
            ])
            .mockResolvedValueOnce([{ id: 20, name: 'Buyer B' }])
            .mockResolvedValueOnce({
                content: [],
                pageNumber: 0,
                pageSize: 20,
                totalElements: 0,
            });

        await invoicesModule.initInvoices();

        expect(document.getElementById('invoice-form')).not.toBeNull();

        expect(apiGetMock).toHaveBeenNthCalledWith(
            1,
            '/seller-profiles',
            { withAuth: true },
        );
        expect(apiGetMock).toHaveBeenNthCalledWith(
            2,
            '/contractors',
            { withAuth: true },
        );
        expect(apiGetMock).toHaveBeenNthCalledWith(
            3,
            '/invoices?page=0&size=20',
            { withAuth: true },
        );

        const sellerSelect = document.getElementById('invoice-seller');
        const buyerSelect = document.getElementById('invoice-buyer');

        expect(sellerSelect.options.length).toBe(2);
        expect(buyerSelect.options.length).toBe(2);

        const rows = document.querySelectorAll('[data-items-body] .inv-items__row');
        expect(rows.length).toBeGreaterThanOrEqual(1);
    });
});

describe('initInvoices – validation and submit', () => {
    test('shows validation errors when required fields are missing', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({ id: 1, role: 'OWNER' });

        apiGetMock
            .mockResolvedValueOnce([])
            .mockResolvedValueOnce([])
            .mockResolvedValueOnce({
                content: [],
                pageNumber: 0,
                pageSize: 20,
                totalElements: 0,
            });

        await invoicesModule.initInvoices();

        const form = document.getElementById('invoice-form');
        const errorBox = document.getElementById('invoice-error');

        document.getElementById('invoice-seller').value = '';
        document.getElementById('invoice-buyer').value = '';
        document.getElementById('invoice-issue-date').value = '';
        document.getElementById('invoice-sale-date').value = '';
        document.getElementById('invoice-due-date').value = '';
        document.getElementById('invoice-currency').value = '';

        const body = document.querySelector('[data-items-body]');
        body.innerHTML = '';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        expect(errorBox.hidden).toBe(false);
        const msg = errorBox.textContent;

        expect(msg).toContain('Seller profile is required.');
        expect(msg).toContain('Buyer (contractor) is required.');
        expect(msg).toContain('Issue date is required.');
        expect(msg).toContain('Sale date is required.');
        expect(msg).toContain('Due date is required.');
        expect(msg).toContain('At least one line item is required.');
    });

    test('submits invoice when form is valid and calls createInvoice API', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({ id: 1, role: 'OWNER' });

        apiGetMock
            .mockResolvedValueOnce([
                {
                    id: 10,
                    name: 'Seller A',
                    nip: '1234567890',
                    defaultCurrency: 'PLN',
                    defaultPaymentTermDays: 14,
                },
            ])
            .mockResolvedValueOnce([{ id: 20, name: 'Buyer B' }])
            .mockResolvedValueOnce({
                content: [],
                pageNumber: 0,
                pageSize: 20,
                totalElements: 0,
            });

        apiRequestMock.mockResolvedValueOnce({ id: 99, number: 'FV/1/2025' });

        await invoicesModule.initInvoices();

        const form = document.getElementById('invoice-form');
        const errorBox = document.getElementById('invoice-error');
        const successBox = document.getElementById('invoice-success');
        const sellerSelect = document.getElementById('invoice-seller');
        const buyerSelect = document.getElementById('invoice-buyer');
        const issueInput = document.getElementById('invoice-issue-date');
        const saleInput = document.getElementById('invoice-sale-date');
        const dueInput = document.getElementById('invoice-due-date');
        const currencyInput = document.getElementById('invoice-currency');

        sellerSelect.value = '10';
        buyerSelect.value = '20';
        issueInput.value = '2025-01-01';
        saleInput.value = '2025-01-01';
        dueInput.value = '2025-01-14';
        currencyInput.value = 'PLN';

        const body = document.querySelector('[data-items-body]');
        body.innerHTML = `
      <div class="inv-items__row">
        <input type="text" data-item-desc value="Service A" />
        <input type="number" data-item-qty value="2" />
        <input type="text" data-item-unit value="h" />
        <input type="number" data-item-net value="100" />
        <input type="text" data-item-vat value="23" />
        <button type="button" class="inv-items__delete-btn">Remove</button>
      </div>
    `;

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(apiRequestMock).toHaveBeenCalledTimes(1);
        const [path, options] = apiRequestMock.mock.calls[0];

        expect(path).toBe('/invoices');
        expect(options.method).toBe('POST');
        expect(options.withAuth).toBe(true);
        expect(options.body).toEqual(
            expect.objectContaining({
                sellerProfileId: 10,
                contractorId: 20,
                currency: 'PLN',
                items: [
                    expect.objectContaining({
                        description: 'Service A',
                        quantity: 2,
                        netUnitPrice: 100,
                        vatRate: '23',
                    }),
                ],
            }),
        );

        expect(errorBox.hidden).toBe(true);
        expect(successBox.hidden).toBe(false);
        expect(successBox.textContent).toContain('Invoice FV/1/2025 created.');
    });
});
