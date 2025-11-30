import { describe, test, expect, vi, beforeEach } from 'vitest';

const fetchCurrentUserOrNullMock = vi.fn();
const logoutMock = vi.fn();

vi.mock('../../../src/shared/authApi.js', () => ({
    fetchCurrentUserOrNull: fetchCurrentUserOrNullMock,
    logout: logoutMock,
}));

let menupageModule;

function setupMenupageDom() {
    document.body.innerHTML = `
    <main id="app">
      <div id="view-root">
        <section class="menupage">
          <div class="menupage-shell">
            <header class="menupage-header">
              <div>
                <p class="menupage-eyebrow">Workspace</p>
                <h1 class="menupage-title">
                  Good to see you again
                </h1>
                <p class="menupage-subtitle" data-role-summary>
                  Loading your workspace...
                </p>
              </div>

              <div class="menupage-user-badge">
                <span class="badge-label" data-role-badge>Role</span>
                <p class="badge-text">
                  Organisation ID: <span data-org-id>–</span>
                </p>

                <button type="button" class="menupage-logout" data-logout-btn>
                  Log out
                </button>
              </div>
            </header>

            <section class="menupage-grid" data-menu-grid></section>
          </div>
        </section>
      </div>
    </main>
  `;
}

beforeEach(async () => {
    vi.clearAllMocks();
    setupMenupageDom();
    menupageModule = await import(
        '../../../src/component/menupage/menupage.js'
        );
});

describe('initMenupage – OWNER view', () => {
    test('renders correct role info, org id and menu cards for OWNER', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 1,
            role: 'OWNER',
            organizationId: 42,
        });

        await menupageModule.initMenupage();

        const roleBadge = document.querySelector('[data-role-badge]');
        const roleSummary = document.querySelector('[data-role-summary]');
        const orgId = document.querySelector('[data-org-id]');
        const cards = document.querySelectorAll('.menupage-card');
        const links = Array.from(
            document.querySelectorAll('.menupage-card a'),
        ).map((a) => a.getAttribute('href'));

        expect(roleBadge.textContent).toBe('Owner');
        expect(roleSummary.textContent).toContain('full picture');
        expect(orgId.textContent).toBe('42');
        expect(cards.length).toBe(4);
        expect(links).toContain('/invoices');
        expect(links).toContain('/contractors');
        expect(links).toContain('/organisation');
        expect(links).toContain('/users');
    });
});

describe('initMenupage – ACCOUNTANT view', () => {
    test('renders accountant role and only invoices card', async () => {
        fetchCurrentUserOrNullMock.mockResolvedValue({
            id: 2,
            role: 'ACCOUNTANT',
            organizationId: 777,
        });

        await menupageModule.initMenupage();

        const roleBadge = document.querySelector('[data-role-badge]');
        const roleSummary = document.querySelector('[data-role-summary]');
        const orgId = document.querySelector('[data-org-id]');
        const cards = document.querySelectorAll('.menupage-card');
        const links = Array.from(
            document.querySelectorAll('.menupage-card a'),
        ).map((a) => a.getAttribute('href'));

        expect(roleBadge.textContent).toBe('Accountant');
        expect(roleSummary.textContent).toContain('accounting data');
        expect(orgId.textContent).toBe('777');

        expect(cards.length).toBe(1);
        expect(links).toEqual(['/invoices']);
    });
});
