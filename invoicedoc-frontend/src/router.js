import { clearView } from './shared/mount.js';
import {
    bootstrapTokenFromSession,
    fetchCurrentUserOrNull,
} from './shared/authApi.js';


let currentUser = null;

async function bootstrapApp() {
    bootstrapTokenFromSession();

    currentUser = await fetchCurrentUserOrNull();
    console.log('Current user:', currentUser);
}

export function getCurrentUser() {
    return currentUser;
}

const routes = {
    '/': () =>
        import('./component/landing-page/loader.landing.js')
            .then((m) => m.mountLanding()),
        '/auth/login': () =>
            import('./component/login/loader.login.js')
                .then((m) => m.mountLogin()),
        '/auth/register': () =>
            import('./component/register/loader.register.js')
                .then((m) => m.mountRegister()),
        '/app': () =>
            import('./component/menupage/loader.menupage.js')
                .then((m) => m.mountMenupage()),
        '/users': () =>
            import('./component/users-access/loader.users-access.js')
                .then((m) => m.mountUsersAccess()),
        '/invoices': () =>
            import('./component/invoices/loader.invoices.js')
                .then((m) => m.mountInvoices()),
        '/contractors': () =>
            import('./component/contractors/loader.contractors.js')
                .then((m) => m.mountContractors()),
        '/organisation': () =>
            import('./component/org-settings/loader.org-settings.js')
                .then((m) => m.mountOrgSettings()),

};
const FALLBACK = '/';

export async function render() {
    clearView();

    const path = Object.prototype.hasOwnProperty.call(routes, location.pathname)
        ? location.pathname
        : FALLBACK;

    try {
        await routes[path]();
    } catch (err) {
        console.error('Route error:', err);
        if (path !== FALLBACK) {
            await routes[FALLBACK]();
        }
    }
}

export async function navigate(path) {
    if (location.pathname === path) return;
    history.pushState({}, '', path);
    await render();
}

document.addEventListener('click', async (e) => {
    const a = e.target.closest('a[href^="/"]');
    if (!a) return;

    const url = new URL(a.href);
    const sameOrigin = url.origin === location.origin;
    const handled = Object.prototype.hasOwnProperty.call(routes, url.pathname);

    if (sameOrigin && handled) {
        e.preventDefault();
        await navigate(url.pathname);
    }
});

window.addEventListener('popstate', () => {
    render().catch((err) => console.error(err));
});

async function main() {
    try {
        await bootstrapApp();
        await render();
    } catch (err) {
        console.error('Failed to bootstrap app:', err);
    }
}
main();
