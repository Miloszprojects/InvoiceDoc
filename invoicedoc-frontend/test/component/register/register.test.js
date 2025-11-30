import { describe, test, expect, vi, beforeEach, afterAll } from 'vitest';

const registerUserMock = vi.fn();

class HttpErrorMock extends Error {
    constructor(status, body) {
        super('HttpError');
        this.status = status;
        this.body = body;
    }
}

vi.mock('../../../src/shared/authApi.js', () => ({
    registerUser: (...args) => registerUserMock(...args),
}));

vi.mock('../../../src/shared/httpClient.js', () => ({
    HttpError: HttpErrorMock,
}));

let registerModule;
const originalLocation = window.location;

function setupDom() {
    document.body.innerHTML = `
    <section class="auth auth--register">
      <section class="auth-layout">
        <div class="auth-card">
          <form class="auth-form" id="register-form" novalidate>
            <div class="auth-grid">
              <div class="auth-field">
                <label for="fullName">Full name</label>
                <input id="fullName" name="fullName" type="text" />
              </div>
              <div class="auth-field">
                <label for="email">Work email</label>
                <input id="email" name="email" type="email" />
              </div>
              <div class="auth-field">
                <label for="organisation">Organisation name</label>
                <input id="organisation" name="organisation" type="text" />
              </div>
              <div class="auth-field">
                <label for="password">Password</label>
                <input id="password" name="password" type="password" />
              </div>
              <div class="auth-field">
                <label for="confirmPassword">Confirm password</label>
                <input id="confirmPassword" name="confirmPassword" type="password" />
              </div>
            </div>

            <fieldset class="role-group">
              <label class="role-option">
                <input type="radio" name="role" value="ADMIN" checked />
              </label>
              <label class="role-option">
                <input type="radio" name="role" value="OWNER" />
              </label>
              <label class="role-option">
                <input type="radio" name="role" value="ACCOUNTANT" />
              </label>
            </fieldset>

            <div class="auth-footer">
              <button type="submit" class="btn btn-primary">Create account</button>
            </div>

            <div class="auth-error" id="register-error" hidden></div>
          </form>
        </div>
      </section>
    </section>
  `;
}

beforeEach(async () => {
    vi.clearAllMocks();
    delete window.location;
    window.location = {
        href: 'http://localhost/register',
    };

    setupDom();

    registerModule = await import(
        '../../../src/component/register/register.js'
        );
    registerModule.initRegister();
});

afterAll(() => {
    delete window.location;
    window.location = originalLocation;
});

describe('initRegister – validation & submit', () => {
    test('shows error when required fields are missing', () => {
        const form = document.getElementById('register-form');
        const errorBox = document.getElementById('register-error');

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        expect(registerUserMock).not.toHaveBeenCalled();
        expect(errorBox.hidden).toBe(false);
        expect(errorBox.textContent).toBe('Please fill in all required fields.');
    });

    test('shows error when password too short', () => {
        const form = document.getElementById('register-form');
        const errorBox = document.getElementById('register-error');

        document.getElementById('fullName').value = 'John Doe';
        document.getElementById('email').value = 'john@example.com';
        document.getElementById('organisation').value = 'ACME';
        document.getElementById('password').value = 'short';
        document.getElementById('confirmPassword').value = 'short';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        expect(registerUserMock).not.toHaveBeenCalled();
        expect(errorBox.hidden).toBe(false);
        expect(errorBox.textContent).toBe(
            'Password should be at least 8 characters long.',
        );
    });

    test('shows error when passwords do not match', () => {
        const form = document.getElementById('register-form');
        const errorBox = document.getElementById('register-error');

        document.getElementById('fullName').value = 'John Doe';
        document.getElementById('email').value = 'john@example.com';
        document.getElementById('organisation').value = 'ACME';
        document.getElementById('password').value = 'verystrong';
        document.getElementById('confirmPassword').value = 'different';

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        expect(registerUserMock).not.toHaveBeenCalled();
        expect(errorBox.hidden).toBe(false);
        expect(errorBox.textContent).toBe('Passwords do not match.');
    });

    test('successful register calls registerUser and sets window.location.href with role', async () => {
        const form = document.getElementById('register-form');
        const errorBox = document.getElementById('register-error');

        document.getElementById('fullName').value = 'John Doe';
        document.getElementById('email').value = 'john@example.com';
        document.getElementById('organisation').value = 'ACME';
        document.getElementById('password').value = 'verystrong';
        document.getElementById('confirmPassword').value = 'verystrong';

        const ownerRadio = document.querySelector('input[name="role"][value="OWNER"]');
        ownerRadio.checked = true;

        registerUserMock.mockResolvedValueOnce({ role: 'OWNER' });

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        await vi.waitFor(() => {
            expect(registerUserMock).toHaveBeenCalledTimes(1);
        });

        expect(registerUserMock).toHaveBeenCalledWith(
            expect.objectContaining({
                fullName: 'John Doe',
                email: 'john@example.com',
                organisation: 'ACME',
                password: 'verystrong',
                confirmPassword: 'verystrong',
                role: 'OWNER',
            }),
        );

        expect(errorBox.hidden).toBe(true);
        expect(window.location.href.startsWith('/auth/login?')).toBe(true);
        const url = new URL(`http://dummy${window.location.href}`);
        expect(url.searchParams.get('registered')).toBe('1');
        expect(url.searchParams.get('role')).toBe('OWNER');
    });

    test('EMAIL_EXISTS maps to a friendly error', async () => {
        const form = document.getElementById('register-form');
        const errorBox = document.getElementById('register-error');

        document.getElementById('fullName').value = 'John Doe';
        document.getElementById('email').value = 'john@example.com';
        document.getElementById('organisation').value = 'ACME';
        document.getElementById('password').value = 'verystrong';
        document.getElementById('confirmPassword').value = 'verystrong';

        registerUserMock.mockRejectedValueOnce(
            new HttpErrorMock(409, { message: 'EMAIL_EXISTS' }),
        );

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        await vi.waitFor(() => {
            expect(errorBox.hidden).toBe(false);
        });

        expect(errorBox.textContent).toBe(
            'An account with this email already exists. You can sign in or use a different address.',
        );
    });

    test('ORG_ALREADY_HAS_ADMIN error', async () => {
        const form = document.getElementById('register-form');
        const errorBox = document.getElementById('register-error');

        document.getElementById('fullName').value = 'John Doe';
        document.getElementById('email').value = 'john@example.com';
        document.getElementById('organisation').value = 'ACME';
        document.getElementById('password').value = 'verystrong';
        document.getElementById('confirmPassword').value = 'verystrong';

        registerUserMock.mockRejectedValueOnce(
            new HttpErrorMock(403, { message: 'ORG_ALREADY_HAS_ADMIN' }),
        );

        form.dispatchEvent(
            new Event('submit', { bubbles: true, cancelable: true }),
        );

        await vi.waitFor(() => {
            expect(errorBox.hidden).toBe(false);
        });

        expect(errorBox.textContent).toBe(
            'This organisation already has an Admin. Ask them to add you as an Owner or Accountant, or choose a different organisation name.',
        );
    });
});
