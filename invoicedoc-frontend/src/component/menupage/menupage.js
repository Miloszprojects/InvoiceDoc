import { fetchCurrentUserOrNull, logout } from '../../shared/authApi.js';

const OWNER_BASE_MENU = [
    {
        title: 'Invoices',
        description:
            'Create and send invoices in seconds. Track status, due dates and payments.',
        href: '/invoices',
        disabled: false,
    },
    {
        title: 'Contractors',
        description:
            'Manage your clients and suppliers. Reuse saved details on every invoice.',
        href: '/contractors',
        disabled: false,
    },
    {
        title: 'Organisation settings',
        description:
            'Rename organisation or manage advanced settings. Admin only.',
        href: '/organisation',
        disabled: false,
    },
    {
        title: 'Users & access',
        description:
            'Invite your accountant and manage access for one or two trusted admins.',
        href: '/users',
        disabled: false,
    },
];

const MENU_BY_ROLE = {
    ADMIN: OWNER_BASE_MENU,

    OWNER: OWNER_BASE_MENU,

    ACCOUNTANT: [
        {
            title: 'Invoices',
            description:
                'View and export all invoices you have access to in this organisation.',
            href: '/invoices',
            disabled: false,
        },
    ],

    DEFAULT: OWNER_BASE_MENU,
};

function createCardElement(item) {
    const article = document.createElement('article');
    article.className = 'menupage-card';

    const h2 = document.createElement('h2');
    h2.textContent = item.title;

    const p = document.createElement('p');
    p.textContent = item.description;

    const a = document.createElement('a');
    a.textContent = item.disabled ? 'Coming soon' : 'Open →';
    a.className =
        'menupage-link' + (item.disabled ? ' menupage-link--disabled' : '');
    a.href = item.disabled ? '#' : item.href;

    article.appendChild(h2);
    article.appendChild(p);
    article.appendChild(a);

    return article;
}

export async function initMenupage() {
    const roleSummaryEl = document.querySelector('[data-role-summary]');
    const roleBadgeEl = document.querySelector('[data-role-badge]');
    const orgIdEl = document.querySelector('[data-org-id]');
    const gridEl = document.querySelector('[data-menu-grid]');
    const logoutBtn = document.querySelector('[data-logout-btn]');

    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            logout();
            window.location.href = '/auth/login';
        });
    }

    try {
        const user = await fetchCurrentUserOrNull();

        if (!user) {
            window.location.href = '/auth/login';
            return;
        }

        if (orgIdEl) {
            orgIdEl.textContent = user.organizationId ?? '–';
        }

        if (roleBadgeEl && roleSummaryEl) {
            switch (user.role) {
                case 'ADMIN':
                    roleBadgeEl.textContent = 'Admin';
                    roleSummaryEl.textContent =
                        'You can create and manage organisations, and approve Owners and Accountants.';
                    break;
                case 'OWNER':
                    roleBadgeEl.textContent = 'Owner';
                    roleSummaryEl.textContent =
                        'You see the full picture for this organisation: invoices, payments and most settings.';
                    break;
                case 'ACCOUNTANT':
                    roleBadgeEl.textContent = 'Accountant';
                    roleSummaryEl.textContent =
                        'You have focused access to accounting data and exports for this organisation.';
                    break;
                default:
                    roleBadgeEl.textContent = user.role || 'User';
                    roleSummaryEl.textContent =
                        'You are signed in to this organisation. Your permissions depend on your role.';
            }
        }

        if (gridEl) {
            gridEl.innerHTML = '';

            const roleKey = user.role || 'DEFAULT';
            const items = MENU_BY_ROLE[roleKey] || MENU_BY_ROLE.DEFAULT;

            items.forEach((item) => {
                gridEl.appendChild(createCardElement(item));
            });
        }
    } catch (err) {
        console.error('Failed to load current user on menupage:', err);
        window.location.href = '/auth/login';
    }
}
