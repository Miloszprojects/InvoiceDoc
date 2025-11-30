import {
    describe,
    test,
    expect,
    vi,
    beforeEach,
    afterAll,
} from 'vitest';

import * as httpClient from '../../../src/shared/httpClient.js';
import * as authApi from '../../../src/shared/authApi.js';
import { initUsersAccess } from '../../../src/component/users-access/users-access.js';

const apiGetMock = vi.spyOn(httpClient, 'apiGet');
const apiRequestMock = vi.spyOn(httpClient, 'apiRequest');
const fetchCurrentUserOrNullMock = vi.spyOn(
    authApi,
    'fetchCurrentUserOrNull',
);

const { HttpError } = httpClient;

const originalLocation = window.location;

function setupDom() {
    document.body.innerHTML = `
    <div class="users-shell">
      <header class="users-header">
        <div>
          <p class="users-eyebrow">Users &amp; access</p>
          <h1 class="users-title">Manage your team</h1>
          <p class="users-subtitle">
              Approve owners and accountants who work in this organisation.
          </p>
        </div>
      </header>

      <section class="users-table-wrapper">
        <table class="users-table">
          <thead>
            <tr>
              <th>Full name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody data-users-table-body>
            <tr>
              <td colspan="5" class="users-empty">
                Loading users...
              </td>
            </tr>
          </tbody>
        </table>
      </section>

      <p id="users-error" class="users-error" hidden></p>
    </div>
  `;
}

function setupLocationStub() {
    Object.defineProperty(window, 'location', {
        configurable: true,
        enumerable: true,
        writable: true,
        value: {
            href: 'http://localhost/users',
        },
    });
}

beforeEach(() => {
    vi.clearAllMocks();
    setupDom();
    setupLocationStub();
});

afterAll(() => {
    Object.defineProperty(window, 'location', {
        configurable: true,
        enumerable: true,
        writable: true,
        value: originalLocation,
    });
});

describe('initUsersAccess', () => {
    test('renders empty state when no users', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 1,
            role: 'ADMIN',
        });

        apiGetMock.mockResolvedValueOnce([]);

        await initUsersAccess();

        const tbody = document.querySelector('[data-users-table-body]');

        await vi.waitFor(() => {
            expect(tbody.textContent).toContain(
                'No users in this organisation yet.',
            );
            expect(tbody.querySelector('.users-empty')).not.toBeNull();
        });
    });

    test('renders users and shows badges / statuses', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 999,
            role: 'ADMIN',
        });

        apiGetMock.mockResolvedValueOnce([
            {
                id: 1,
                fullName: 'Admin User',
                email: 'admin@example.com',
                role: 'ADMIN',
                approvedByOwner: true,
            },
            {
                id: 2,
                fullName: 'Owner Pending',
                email: 'owner.pending@example.com',
                role: 'OWNER',
                approvedByOwner: false,
            },
            {
                id: 3,
                fullName: 'Accountant Active',
                email: 'acct@example.com',
                role: 'ACCOUNTANT',
                approvedByOwner: true,
            },
        ]);

        await initUsersAccess();

        const tbody = document.querySelector('[data-users-table-body]');

        await vi.waitFor(() => {
            const text = tbody.textContent;

            expect(text).toContain('Admin User');
            expect(text).toContain('Owner Pending');
            expect(text).toContain('Accountant Active');

            expect(text).toContain('ADMIN');
            expect(text).toContain('OWNER');
            expect(text).toContain('ACCOUNTANT');

            expect(text).toContain('Pending approval');
            expect(text).toContain('Active');
        });
    });

    test('clicking Approve calls API and reloads list', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 1,
            role: 'ADMIN',
        });

        apiGetMock
            .mockResolvedValueOnce([
                {
                    id: 10,
                    fullName: 'Owner Pending',
                    email: 'owner@example.com',
                    role: 'OWNER',
                    approvedByOwner: false,
                },
            ])
            .mockResolvedValueOnce([
                {
                    id: 10,
                    fullName: 'Owner Pending',
                    email: 'owner@example.com',
                    role: 'OWNER',
                    approvedByOwner: true,
                },
            ]);

        apiRequestMock.mockResolvedValueOnce(undefined);

        await initUsersAccess();

        const tbody = document.querySelector('[data-users-table-body]');

        await vi.waitFor(() => {
            const btn = tbody.querySelector('.users-btn-approve');
            expect(btn).not.toBeNull();
        });

        let approveBtn = tbody.querySelector('.users-btn-approve');
        approveBtn.click();

        await vi.waitFor(() => {
            expect(apiRequestMock).toHaveBeenCalledTimes(1);
        });

        const [path, options] = apiRequestMock.mock.calls[0];
        expect(path).toBe('/users/10/approve');
        expect(options.method).toBe('PATCH');
        expect(options.withAuth).toBe(true);

        await vi.waitFor(() => {
            expect(tbody.textContent).toContain('Active');
        });

        approveBtn = tbody.querySelector('.users-btn-approve');
        expect(approveBtn).toBeNull();
    });

    test('shows Deactivate button for active non-admin when current user is ADMIN', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 1,
            role: 'ADMIN',
        });

        apiGetMock.mockResolvedValueOnce([
            {
                id: 20,
                fullName: 'Owner Active',
                email: 'owner.active@example.com',
                role: 'OWNER',
                approvedByOwner: true,
            },
        ]);

        await initUsersAccess();

        await vi.waitFor(() => {
            const tbody = document.querySelector('[data-users-table-body]');
            const deactivateBtn = tbody.querySelector('.users-btn-deactivate');

            expect(tbody.textContent).toContain('Owner Active');
            expect(tbody.textContent).toContain('Active');
            expect(deactivateBtn).not.toBeNull();
            expect(deactivateBtn.disabled).toBe(false);
        });
    });

    test('clicking Deactivate calls API and reloads list', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 1,
            role: 'ADMIN',
        });

        apiGetMock
            .mockResolvedValueOnce([
                {
                    id: 20,
                    fullName: 'Owner Active',
                    email: 'owner.active@example.com',
                    role: 'OWNER',
                    approvedByOwner: true,
                },
            ])
            .mockResolvedValueOnce([]);

        apiRequestMock.mockResolvedValueOnce(undefined);

        await initUsersAccess();

        const tbody = document.querySelector('[data-users-table-body]');

        await vi.waitFor(() => {
            const btn = tbody.querySelector('.users-btn-deactivate');
            expect(btn).not.toBeNull();
        });

        let deactivateBtn = tbody.querySelector('.users-btn-deactivate');
        deactivateBtn.click();

        await vi.waitFor(() => {
            expect(apiRequestMock).toHaveBeenCalledTimes(1);
        });

        const [path, options] = apiRequestMock.mock.calls[0];
        expect(path).toBe('/users/20/deactivate');
        expect(options.method).toBe('PATCH');
        expect(options.withAuth).toBe(true);

        await vi.waitFor(() => {
            expect(tbody.textContent).toContain(
                'No users in this organisation yet.',
            );
        });

        deactivateBtn = tbody.querySelector('.users-btn-deactivate');
        expect(deactivateBtn).toBeNull();
    });

    test('ACCOUNTANT cannot see Approve or Deactivate buttons', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 1,
            role: 'ACCOUNTANT',
        });

        apiGetMock.mockResolvedValueOnce([
            {
                id: 30,
                fullName: 'Owner Pending',
                email: 'owner.pending@example.com',
                role: 'OWNER',
                approvedByOwner: false,
            },
        ]);

        await initUsersAccess();

        await vi.waitFor(() => {
            const tbody = document.querySelector('[data-users-table-body]');
            const approveBtn = tbody.querySelector('.users-btn-approve');
            const deactivateBtn = tbody.querySelector('.users-btn-deactivate');

            expect(tbody.textContent).toContain('Pending approval');
            expect(approveBtn).toBeNull();
            expect(deactivateBtn).toBeNull();
        });
    });

    test('shows global error when loadAndRenderUsers throws', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 1,
            role: 'ADMIN',
        });

        apiGetMock.mockRejectedValueOnce(new Error('boom'));

        await initUsersAccess();

        const tbody = document.querySelector('[data-users-table-body]');
        const errorBox = document.getElementById('users-error');

        await vi.waitFor(() => {
            expect(errorBox.hidden).toBe(false);
            expect(errorBox.textContent).toContain(
                'Unable to load users. Please try again later.',
            );
            expect(tbody.textContent).toContain(
                'Unable to load users. Please try again later.',
            );
        });
    });
});
