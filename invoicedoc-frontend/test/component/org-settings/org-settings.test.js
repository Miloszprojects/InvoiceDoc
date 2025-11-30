import { describe, test, expect, beforeEach, vi } from 'vitest';

const apiGetMock = vi.fn();
const apiRequestMock = vi.fn();
class HttpError extends Error {
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
    HttpError,
}));

const fetchCurrentUserOrNullMock = vi.fn();

vi.mock('../../../src/shared/authApi.js', () => ({
    fetchCurrentUserOrNull: fetchCurrentUserOrNullMock,
}));

let orgSettingsModule;

function setupDom() {
    document.body.innerHTML = `
    <section class="org-settings-root">
      <section class="org-settings">
        <header class="org-settings__header">
          <h1>Organisation settings</h1>
          <p>Set up seller profiles used on invoices issued by this organisation.</p>
        </header>

        <div class="org-settings__layout">
          <div class="org-settings__list">
            <div class="org-settings__list-header">
              <div>
                <h2>Seller profiles</h2>
                <p class="org-settings__list-subtitle">
                  You can create multiple profiles e.g. for different branches or bank accounts.
                </p>
              </div>
              <div class="org-settings__search">
                <input
                  id="seller-search"
                  class="org-input org-input--search"
                  type="text"
                  placeholder="Search by name or NIP..."
                  autocomplete="off"
                />
              </div>
            </div>

            <div class="org-settings__list-body" data-seller-list></div>
          </div>

          <form id="seller-form" class="org-settings__form" novalidate>
            <div class="org-settings__form-header">
              <h2 data-form-title>Add seller profile</h2>
              <p data-form-subtitle>
                Fill in the details of your company as they should appear on invoices.
              </p>
            </div>

            <div class="org-settings__section">
              <h3 class="org-settings__section-title">Basic data</h3>
              <div class="org-settings__grid">
                <div class="org-field org-field--full">
                  <label for="seller-name">Company name <span>*</span></label>
                  <input id="seller-name" class="org-input" type="text" />
                </div>

                <div class="org-field">
                  <label for="seller-nip">NIP <span>*</span></label>
                  <input id="seller-nip" class="org-input" type="text" />
                </div>

                <div class="org-field">
                  <label for="seller-regon">REGON (optional)</label>
                  <input id="seller-regon" class="org-input" type="text" />
                </div>

                <div class="org-field">
                  <label for="seller-krs">KRS (optional)</label>
                  <input id="seller-krs" class="org-input" type="text" />
                </div>

                <div class="org-field">
                  <label for="seller-bank-name">Bank name <span>*</span></label>
                  <input id="seller-bank-name" class="org-input" type="text" />
                </div>

                <div class="org-field">
                  <label for="seller-bank">Bank account (IBAN) <span>*</span></label>
                  <input id="seller-bank" class="org-input" type="text" />
                </div>
              </div>
            </div>

            <div class="org-settings__section">
              <h3 class="org-settings__section-title">Address</h3>
              <div class="org-settings__grid">
                <div class="org-field org-field--full">
                  <label for="seller-street">Street <span>*</span></label>
                  <input id="seller-street" class="org-input" type="text" />
                </div>

                <div class="org-field">
                  <label for="seller-building">Building / flat</label>
                  <input id="seller-building" class="org-input" type="text" />
                </div>

                <div class="org-field">
                  <label for="seller-postal">Postal code <span>*</span></label>
                  <input id="seller-postal" class="org-input" type="text" />
                </div>

                <div class="org-field">
                  <label for="seller-city">City <span>*</span></label>
                  <input id="seller-city" class="org-input" type="text" />
                </div>

                <div class="org-field">
                  <label for="seller-country">Country <span>*</span></label>
                  <input id="seller-country" class="org-input" type="text" />
                </div>
              </div>
            </div>

            <div class="org-settings__section">
              <h3 class="org-settings__section-title">Defaults for invoices</h3>
              <div class="org-settings__grid">
                <div class="org-field">
                  <label for="seller-currency">Default currency <span>*</span></label>
                  <input
                    id="seller-currency"
                    class="org-input"
                    type="text"
                    placeholder="PLN"
                  />
                </div>

                <div class="org-field">
                  <label for="seller-payment-term">
                    Default payment term (days) <span>*</span>
                  </label>
                  <input
                    id="seller-payment-term"
                    class="org-input"
                    type="number"
                    min="1"
                    step="1"
                  />
                </div>
              </div>
            </div>

            <div
              class="org-info-banner org-info-banner--owner-only"
              id="seller-owner-info"
              hidden
            >
              Only <strong>Owners and Admins</strong> of this organisation can add or
              delete seller profiles. Existing profiles are still visible to
              Accountants.
            </div>

            <div class="org-settings__footer">
              <button
                type="submit"
                class="btn btn-primary"
                id="seller-save-btn"
              >
                Save seller profile
              </button>
            </div>

            <div class="org-message org-message--error" id="seller-error" hidden></div>
            <div class="org-message org-message--success" id="seller-success" hidden></div>
          </form>
        </div>
      </section>
    </section>
  `;
}

beforeEach(async () => {
    vi.resetModules();
    vi.clearAllMocks();

    setupDom();

    orgSettingsModule = await import(
        '../../../src/component/org-settings/org-settings.js'
        );
});

describe('initOrgSettings – role handling', () => {
    test('OWNER can manage profiles: banner hidden, save enabled', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 1,
            role: 'OWNER',
        });

        apiGetMock.mockResolvedValueOnce([]);
        await orgSettingsModule.initOrgSettings();

        const ownerInfo = document.getElementById('seller-owner-info');
        const saveBtn = document.getElementById('seller-save-btn');

        expect(ownerInfo.hidden).toBe(true);
        expect(saveBtn.disabled).toBe(false);
    });

    test('ACCOUNTANT cannot manage: banner visible, save disabled', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 2,
            role: 'ACCOUNTANT',
        });

        apiGetMock.mockResolvedValueOnce([]);

        await orgSettingsModule.initOrgSettings();

        const ownerInfo = document.getElementById('seller-owner-info');
        const saveBtn = document.getElementById('seller-save-btn');

        expect(ownerInfo.hidden).toBe(false);
        expect(saveBtn.disabled).toBe(true);
    });
});

describe('initOrgSettings – loading list', () => {
    test('loads profiles and renders list rows', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 1,
            role: 'OWNER',
        });

        apiGetMock.mockResolvedValueOnce([
            {
                id: 10,
                name: 'Main company',
                nip: '1234567890',
                bankName: 'mBank',
                defaultCurrency: 'PLN',
                address: { city: 'Warsaw' },
            },
            {
                id: 11,
                name: 'Branch',
                nip: '0987654321',
                bankName: 'PKO',
                defaultCurrency: 'EUR',
                address: { city: 'Krakow' },
            },
        ]);

        await orgSettingsModule.initOrgSettings();

        const list = document.querySelector('[data-seller-list]');
        const rows = list.querySelectorAll('.org-list-row');

        expect(rows.length).toBe(2);
        expect(list.textContent).toContain('Main company');
        expect(list.textContent).toContain('Branch');
    });
});

describe('org-settings – validation and submit', () => {
    test('shows validation errors when required fields are missing', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({ id: 1, role: 'OWNER' });

        apiGetMock.mockResolvedValueOnce([]);

        await orgSettingsModule.initOrgSettings();

        const form = document.getElementById('seller-form');
        const errorBox = document.getElementById('seller-error');

        document.getElementById('seller-name').value = '';
        document.getElementById('seller-nip').value = '';
        document.getElementById('seller-bank-name').value = '';
        document.getElementById('seller-bank').value = '';
        document.getElementById('seller-street').value = '';
        document.getElementById('seller-postal').value = '';
        document.getElementById('seller-city').value = '';
        document.getElementById('seller-country').value = '';
        document.getElementById('seller-currency').value = '';
        document.getElementById('seller-payment-term').value = '';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        expect(errorBox.hidden).toBe(false);

        const msg = errorBox.textContent;
        expect(msg).toContain('Company name is required.');
        expect(msg).toContain('NIP is required.');
        expect(msg).toContain('Bank name is required.');
        expect(msg).toContain('Bank account (IBAN) is required.');
        expect(msg).toContain('Street is required.');
        expect(msg).toContain('Postal code is required.');
        expect(msg).toContain('City is required.');
        expect(msg).toContain('Country is required.');
        expect(msg).toContain(
            'Default payment term must be a positive number of days.',
        );
    });

    test('ACCOUNTANT submit: shows permission error and does not call API', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({ id: 2, role: 'ACCOUNTANT' });

        apiGetMock.mockResolvedValueOnce([]);

        await orgSettingsModule.initOrgSettings();

        const form = document.getElementById('seller-form');
        const errorBox = document.getElementById('seller-error');

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        expect(apiRequestMock).not.toHaveBeenCalled();
        expect(errorBox.textContent).toContain(
            'Only Owners or Admins of this organisation can add seller profiles.',
        );
    });

    test('OWNER submit: valid payload calls API and does not show error', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({ id: 1, role: 'OWNER' });

        apiGetMock.mockResolvedValueOnce([]);
        apiRequestMock.mockResolvedValueOnce({ id: 99 });

        await orgSettingsModule.initOrgSettings();

        const form = document.getElementById('seller-form');
        const errorBox = document.getElementById('seller-error');

        document.getElementById('seller-name').value = 'ACME Sp. z o.o.';
        document.getElementById('seller-nip').value = '1234567890';
        document.getElementById('seller-regon').value = '123456789';
        document.getElementById('seller-krs').value = '0000000000';
        document.getElementById('seller-bank-name').value = 'mBank';
        document.getElementById('seller-bank').value = 'PL00123456789012345678901234';
        document.getElementById('seller-street').value = 'Testowa 1';
        document.getElementById('seller-building').value = '1A';
        document.getElementById('seller-postal').value = '00-000';
        document.getElementById('seller-city').value = 'Warszawa';
        document.getElementById('seller-country').value = 'Poland';
        document.getElementById('seller-currency').value = 'PLN';
        document.getElementById('seller-payment-term').value = '14';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        await vi.waitFor(() => {
            expect(apiRequestMock).toHaveBeenCalledTimes(1);
        });

        const [path, options] = apiRequestMock.mock.calls[0];

        expect(path).toBe('/seller-profiles');
        expect(options.method).toBe('POST');
        expect(options.withAuth).toBe(true);

        expect(options.body).toEqual(
            expect.objectContaining({
                name: 'ACME Sp. z o.o.',
                nip: '1234567890',
                defaultCurrency: 'PLN',
                defaultPaymentTermDays: 14,
            }),
        );
        expect(errorBox.hidden).toBe(true);
    });

});
