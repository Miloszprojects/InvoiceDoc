import { describe, test, expect, vi, beforeEach } from 'vitest';

const loginUserMock = vi.fn();

class MockHttpError extends Error {
    constructor(message, status, body) {
        super(message);
        this.name = 'HttpError';
        this.status = status;
        this.body = body;
    }
}

vi.mock('../../../src/shared/authApi.js', () => ({
    loginUser: loginUserMock,
}));

vi.mock('../../../src/shared/httpClient.js', () => ({
    HttpError: MockHttpError,
}));

let loginModule;

function setupLoginDom() {
    document.body.innerHTML = `
    <main id="app">
      <div id="view-root">
        <section class="auth auth--login">
          <section class="auth-layout">
            <div class="auth-card">
              <div class="auth-header">
                <h1>Sign in to InvoiceApp</h1>
                <p>Access your organisations, invoices and team in one place.</p>
              </div>

              <p id="login-info" class="auth-info" hidden></p>

              <form class="auth-form" id="login-form" novalidate>
                <div class="auth-grid auth-grid--single">
                  <div class="auth-field">
                    <label for="login-email">Work email</label>
                    <input
                      id="login-email"
                      name="email"
                      type="email"
                      autocomplete="email"
                      required
                    />
                  </div>

                  <div class="auth-field">
                    <label for="login-password">Password</label>
                    <input
                      id="login-password"
                      name="password"
                      type="password"
                      autocomplete="current-password"
                      required
                    />
                  </div>
                </div>

                <div class="auth-footer">
                  <button type="submit" class="btn btn-primary">Sign in</button>
                  <p class="auth-switch">
                    New to InvoiceApp?
                    <a href="/auth/register">Create account</a>
                  </p>
                </div>

                <p class="auth-note">
                  Having trouble signing in? Check with your organisation owner or
                  accountant if your access is still active.
                </p>

                <div class="auth-error" id="login-error" hidden></div>
              </form>
            </div>
          </section>
        </section>
      </div>
    </main>
  `;
}

beforeEach(async () => {
    vi.resetModules();
    vi.clearAllMocks();
    window.history.replaceState({}, '', '/auth/login');

    setupLoginDom();
    loginModule = await import('../../../src/component/login/login.js');
});

describe('initLogin – basic behaviour', () => {
    test('does nothing if login form is missing', async () => {
        document.body.innerHTML = '<main><p>no form</p></main>';

        loginModule.initLogin();

        expect(loginUserMock).not.toHaveBeenCalled();
    });

    test('hides info box when "registered=1" is not in URL', () => {
        window.history.replaceState({}, '', '/auth/login');

        setupLoginDom();
        loginModule.initLogin();

        const infoBox = document.getElementById('login-info');
        const card = document.querySelector('.auth-card');

        expect(infoBox.hidden).toBe(true);
        expect(infoBox.textContent).toBe('');
        expect(card.classList.contains('auth-card--success')).toBe(false);
    });
});

describe('initLogin – info after registration', () => {
    test('shows correct info message for ACCOUNTANT and clears URL params', () => {
        window.history.replaceState(
            {},
            '',
            '/auth/login?registered=1&role=ACCOUNTANT',
        );

        setupLoginDom();
        loginModule.initLogin();

        const infoBox = document.getElementById('login-info');
        const card = document.querySelector('.auth-card');

        expect(infoBox.hidden).toBe(false);
        expect(infoBox.textContent).toContain(
            'accountant account has been created',
        );
        expect(card.classList.contains('auth-card--success')).toBe(true);
        expect(window.location.search).toBe('');
    });

    test('shows generic message for unknown role', () => {
        window.history.replaceState(
            {},
            '',
            '/auth/login?registered=1&role=SOMETHING_ELSE',
        );

        setupLoginDom();
        loginModule.initLogin();

        const infoBox = document.getElementById('login-info');

        expect(infoBox.hidden).toBe(false);
        expect(infoBox.textContent).toContain('Your account has been created.');
    });
});

describe('initLogin – validation & submit', () => {
    test('shows error when email or password is missing', () => {
        setupLoginDom();
        loginModule.initLogin();

        const form = document.getElementById('login-form');
        const errorBox = document.getElementById('login-error');

        document.getElementById('login-email').value = '';
        document.getElementById('login-password').value = '';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        expect(errorBox.hidden).toBe(false);
        expect(errorBox.textContent).toContain(
            'Please enter your email and password.',
        );
        expect(loginUserMock).not.toHaveBeenCalled();
    });

    test('successful login calls loginUser with form data', async () => {
        setupLoginDom();
        loginModule.initLogin();

        loginUserMock.mockResolvedValue({ token: 'dummy' });

        const form = document.getElementById('login-form');
        const emailInput = document.getElementById('login-email');
        const passInput = document.getElementById('login-password');
        const errorBox = document.getElementById('login-error');

        emailInput.value = 'test@example.com';
        passInput.value = 'secret';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(loginUserMock).toHaveBeenCalledTimes(1);
        expect(loginUserMock).toHaveBeenCalledWith({
            email: 'test@example.com',
            password: 'secret',
        });

        expect(errorBox.hidden).toBe(true);

    });

    test('shows specific message for 401 INVALID_CREDENTIALS', async () => {
        setupLoginDom();
        loginModule.initLogin();

        loginUserMock.mockRejectedValue(
            new MockHttpError('invalid', 401, { message: 'INVALID_CREDENTIALS' }),
        );

        const form = document.getElementById('login-form');
        const emailInput = document.getElementById('login-email');
        const passInput = document.getElementById('login-password');
        const errorBox = document.getElementById('login-error');

        emailInput.value = 'wrong@example.com';
        passInput.value = 'bad-pass';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(loginUserMock).toHaveBeenCalledTimes(1);
        expect(errorBox.hidden).toBe(false);
        expect(errorBox.textContent).toContain('Invalid email or password.');
    });

    test('shows specific message for 403 ACCOUNT_NOT_APPROVED', async () => {
        setupLoginDom();
        loginModule.initLogin();

        loginUserMock.mockRejectedValue(
            new MockHttpError('not approved', 403, {
                message: 'ACCOUNT_NOT_APPROVED',
            }),
        );

        const form = document.getElementById('login-form');
        const emailInput = document.getElementById('login-email');
        const passInput = document.getElementById('login-password');
        const errorBox = document.getElementById('login-error');

        emailInput.value = 'user@example.com';
        passInput.value = 'secret';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(loginUserMock).toHaveBeenCalledTimes(1);
        expect(errorBox.hidden).toBe(false);
        expect(errorBox.textContent).toContain(
            'not been approved yet',
        );
    });

    test('shows generic error message for unexpected error', async () => {
        setupLoginDom();
        loginModule.initLogin();

        loginUserMock.mockRejectedValue(new Error('Network fail'));

        const form = document.getElementById('login-form');
        const emailInput = document.getElementById('login-email');
        const passInput = document.getElementById('login-password');
        const errorBox = document.getElementById('login-error');

        emailInput.value = 'user@example.com';
        passInput.value = 'secret';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(loginUserMock).toHaveBeenCalledTimes(1);
        expect(errorBox.hidden).toBe(false);
        expect(errorBox.textContent).toContain(
            'Unable to sign in right now. Please try again in a moment.',
        );
    });
});
