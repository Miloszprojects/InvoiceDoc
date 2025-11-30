import { describe, test, expect, vi, beforeEach } from 'vitest';

const apiGetMock = vi.fn();
const apiRequestMock = vi.fn();

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

let contractorsModule;

beforeEach(async () => {
    vi.resetModules();
    apiGetMock.mockReset();
    apiRequestMock.mockReset();
    apiGetMock.mockResolvedValue([]);

    document.body.innerHTML = `
    <main id="app">
      <div id="view-root">
        <div class="contractors-shell">
          <header class="contractors-header">
            <div>
              <p class="contractors-eyebrow">Contractors</p>
              <h1 class="contractors-title">Your clients & suppliers</h1>
              <p class="contractors-subtitle">
                Save contractor details once and reuse them on every invoice.
              </p>
            </div>

            <div class="contractors-header-actions">
              <div class="contractors-search">
                <input id="contractors-search" type="search" />
              </div>
              <button type="button" id="contractors-add-btn" class="btn btn-primary">
                + Add contractor
              </button>
            </div>
          </header>

          <div class="contractors-layout">
            <section class="contractors-list-card">
              <header class="contractors-list-header">
                <span class="col-name">Name</span>
                <span class="col-type">Type</span>
                <span class="col-id">NIP / PESEL</span>
                <span class="col-city">City</span>
                <span class="col-fav">Favourite</span>
              </header>

              <div class="contractors-list-body" data-contractors-list>
                <p class="contractors-empty">Loading contractors...</p>
              </div>
            </section>

            <section class="contractors-form-card">
              <header class="contractors-form-header">
                <h2 class="contractors-form-title" data-form-title>Edit contractor</h2>
                <p class="contractors-form-subtitle" data-form-subtitle>
                  Select a contractor from the list or add a new one.
                </p>
              </header>

              <form id="contractor-form" novalidate>
                <fieldset class="contractors-fieldset">
                  <legend>Basic details</legend>

                  <div class="field-row">
                    <div class="field">
                      <label for="contractor-name">
                        Name / company name
                        <span class="field-required">*</span>
                      </label>
                      <input
                        id="contractor-name"
                        class="contractors-input"
                        type="text"
                      />
                    </div>

                    <div class="field">
                      <label for="contractor-type">
                        Type
                        <span class="field-required">*</span>
                      </label>
                      <select
                        id="contractor-type"
                        class="contractors-input"
                      >
                        <option value="COMPANY">Company</option>
                        <option value="PERSON">Person</option>
                      </select>
                    </div>
                  </div>

                  <div class="field-row">
                    <div class="field">
                      <label for="contractor-nip">
                        NIP
                        <span
                          class="field-required"
                          data-nip-required-indicator
                        >*</span>
                        <span
                          class="field-optional"
                          data-nip-optional-indicator
                        >(optional)</span>
                      </label>
                      <input
                        id="contractor-nip"
                        class="contractors-input"
                        type="text"
                      />
                    </div>

                    <div class="field">
                      <label for="contractor-pesel">
                        PESEL
                        <span class="field-optional">(optional)</span>
                      </label>
                      <input
                        id="contractor-pesel"
                        class="contractors-input"
                        type="text"
                      />
                    </div>
                  </div>
                </fieldset>

                <fieldset class="contractors-fieldset">
                  <legend>Address</legend>

                  <div class="field-row">
                    <div class="field field-grow">
                      <label for="contractor-street">
                        Street <span class="field-required">*</span>
                      </label>
                      <input
                        id="contractor-street"
                        class="contractors-input"
                        type="text"
                      />
                    </div>

                    <div class="field">
                      <label for="contractor-building">
                        Building / flat
                      </label>
                      <input
                        id="contractor-building"
                        class="contractors-input"
                        type="text"
                      />
                    </div>
                  </div>

                  <div class="field-row">
                    <div class="field">
                      <label for="contractor-postal">
                        Postal code <span class="field-required">*</span>
                      </label>
                      <input
                        id="contractor-postal"
                        class="contractors-input"
                        type="text"
                      />
                    </div>

                    <div class="field">
                      <label for="contractor-city">
                        City <span class="field-required">*</span>
                      </label>
                      <input
                        id="contractor-city"
                        class="contractors-input"
                        type="text"
                      />
                    </div>

                    <div class="field">
                      <label for="contractor-country">
                        Country <span class="field-required">*</span>
                      </label>
                      <input
                        id="contractor-country"
                        class="contractors-input"
                        type="text"
                      />
                    </div>
                  </div>
                </fieldset>

                <fieldset class="contractors-fieldset">
                  <legend>Contact & flags</legend>

                  <div class="field-row">
                    <div class="field">
                      <label for="contractor-email">
                        Email <span class="field-optional">(optional)</span>
                      </label>
                      <input
                        id="contractor-email"
                        class="contractors-input"
                        type="email"
                      />
                    </div>

                    <div class="field">
                      <label for="contractor-phone">
                        Phone <span class="field-optional">(optional)</span>
                      </label>
                      <input
                        id="contractor-phone"
                        class="contractors-input"
                        type="tel"
                      />
                    </div>
                  </div>

                  <div class="field-row field-row--tight">
                    <label class="checkbox">
                      <input
                        id="contractor-favorite"
                        type="checkbox"
                      />
                      <span>Mark as favourite</span>
                    </label>
                  </div>
                </fieldset>

                <div class="contractors-form-footer">
                  <div class="contractors-messages">
                    <p
                      id="contractor-error"
                      class="contractors-message contractors-message--error"
                      hidden
                    ></p>
                    <p
                      id="contractor-success"
                      class="contractors-message contractors-message--success"
                      hidden
                    ></p>
                  </div>

                  <div class="contractors-buttons">
                    <button
                      type="button"
                      id="contractor-delete-btn"
                      class="btn btn-ghost"
                      hidden
                    >
                      Delete
                    </button>

                    <div class="contractors-buttons-main">
                      <button
                        type="button"
                        id="contractor-cancel-btn"
                        class="btn btn-secondary"
                        hidden
                      >
                        Cancel
                      </button>
                      <button
                        type="submit"
                        id="contractor-save-btn"
                        class="btn btn-primary"
                      >
                        Save contractor
                      </button>
                    </div>
                  </div>
                </div>
              </form>
            </section>
          </div>
        </div>
      </div>
    </main>
  `;

    contractorsModule = await import(
        '../../../src/component/contractors/contractors.js'
        );
});

describe('initContractors', () => {
    test('starts in "Add contractor" mode and calls initial /contractors load', async () => {
        apiGetMock.mockResolvedValueOnce([
            {
                id: 1,
                name: 'Acme Sp. z o.o.',
                type: 'COMPANY',
                nip: '1234567890',
                pesel: null,
                address: { city: 'Warsaw' },
                favorite: true,
            },
        ]);

        contractorsModule.initContractors();

        await Promise.resolve();
        await Promise.resolve();

        const title = document.querySelector('[data-form-title]');
        const subtitle = document.querySelector('[data-form-subtitle]');
        const saveBtn = document.getElementById('contractor-save-btn');
        const cancelBtn = document.getElementById('contractor-cancel-btn');
        const deleteBtn = document.getElementById('contractor-delete-btn');

        expect(title.textContent).toBe('Add contractor');
        expect(subtitle.textContent).toContain('Fill in the details');
        expect(saveBtn.textContent).toBe('Save contractor');
        expect(cancelBtn.hidden).toBe(true);
        expect(deleteBtn.hidden).toBe(true);

        const rows = document.querySelectorAll('.contractors-row');
        expect(rows.length).toBe(1);
        expect(rows[0].textContent).toContain('Acme Sp. z o.o.');
        expect(rows[0].textContent).toContain('Warsaw');

        expect(apiGetMock).toHaveBeenCalledWith('/contractors', {
            withAuth: true,
        });
    });

    test('shows validation errors when required fields are missing', async () => {
        apiGetMock.mockResolvedValueOnce([]);

        contractorsModule.initContractors();
        await Promise.resolve();

        const form = document.getElementById('contractor-form');
        const errorBox = document.getElementById('contractor-error');

        document.getElementById('contractor-name').value = '';
        document.getElementById('contractor-street').value = '';
        document.getElementById('contractor-postal').value = '';
        document.getElementById('contractor-city').value = '';
        document.getElementById('contractor-country').value = '';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        expect(errorBox.hidden).toBe(false);
        const msg = errorBox.textContent;

        expect(msg).toContain('Name / company name is required.');
        expect(msg).toContain('Street is required.');
        expect(msg).toContain('Postal code is required.');
        expect(msg).toContain('City is required.');
        expect(msg).toContain('Country is required.');

        expect(
            document
                .getElementById('contractor-name')
                .classList.contains('field--invalid'),
        ).toBe(true);
        expect(
            document
                .getElementById('contractor-street')
                .classList.contains('field--invalid'),
        ).toBe(true);
    });

    test('NIP validation: COMPANY without NIP shows error, PERSON without NIP is allowed', async () => {
        apiGetMock.mockResolvedValueOnce([]);

        contractorsModule.initContractors();
        await Promise.resolve();

        const form = document.getElementById('contractor-form');
        const errorBox = document.getElementById('contractor-error');

        document.getElementById('contractor-name').value = 'Test Co';
        document.getElementById('contractor-street').value = 'Street 1';
        document.getElementById('contractor-postal').value = '00-001';
        document.getElementById('contractor-city').value = 'Warsaw';
        document.getElementById('contractor-country').value = 'Poland';
        document.getElementById('contractor-type').value = 'COMPANY';
        document.getElementById('contractor-nip').value = '';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        expect(errorBox.hidden).toBe(false);
        expect(errorBox.textContent).toContain('NIP is required for companies.');
        expect(
            document
                .getElementById('contractor-nip')
                .classList.contains('field--invalid'),
        ).toBe(true);

        errorBox.hidden = true;
        errorBox.textContent = '';
        document
            .querySelectorAll('.contractors-input.field--invalid')
            .forEach((el) => el.classList.remove('field--invalid'));

        document.getElementById('contractor-type').value = 'PERSON';
        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        expect(errorBox.textContent).not.toContain(
            'NIP is required for companies.',
        );
    });

    test('updateTypeSpecificUi toggles NIP required/optional indicators', async () => {
        apiGetMock.mockResolvedValueOnce([]);

        contractorsModule.initContractors();
        await Promise.resolve();

        const typeSelect = document.getElementById('contractor-type');
        const nipRequired = document.querySelector(
            '[data-nip-required-indicator]',
        );
        const nipOptional = document.querySelector(
            '[data-nip-optional-indicator]',
        );

        typeSelect.value = 'COMPANY';
        typeSelect.dispatchEvent(new Event('change', { bubbles: true }));

        expect(nipRequired.style.display === '' || nipRequired.style.display === 'inline').toBe(
            true,
        );
        expect(nipOptional.style.display).toBe('none');

        typeSelect.value = 'PERSON';
        typeSelect.dispatchEvent(new Event('change', { bubbles: true }));

        expect(nipRequired.style.display).toBe('none');
        expect(nipOptional.style.display === '' || nipOptional.style.display === 'inline').toBe(
            true,
        );
    });
});
