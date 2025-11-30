import { describe, test, expect, vi, beforeEach } from 'vitest';

const clearViewMock = vi.fn();
const bootstrapTokenFromSessionMock = vi.fn();
const fetchCurrentUserOrNullMock = vi.fn();

const mountLandingMock = vi.fn();

const mountMenupageMock = vi.fn();

vi.mock('../src/shared/mount.js', () => ({
    clearView: clearViewMock,
}));

vi.mock('../src/shared/authApi.js', () => ({
    bootstrapTokenFromSession: bootstrapTokenFromSessionMock,
    fetchCurrentUserOrNull: fetchCurrentUserOrNullMock,
}));

vi.mock('../src/component/landing-page/loader.landing.js', () => ({
    mountLanding: mountLandingMock,
}));

vi.mock('../src/component/menupage/loader.menupage.js', () => ({
    mountMenupage: mountMenupageMock,
}));

let routerModule;

beforeEach(async () => {
    vi.resetModules();
    clearViewMock.mockReset();
    bootstrapTokenFromSessionMock.mockReset();
    fetchCurrentUserOrNullMock.mockReset();
    mountLandingMock.mockReset();
    mountMenupageMock.mockReset();

    Object.defineProperty(window, 'location', {
        writable: true,
        value: {
            pathname: '/',
            origin: 'http://localhost',
        },
    });

    window.history.pushState = vi.fn();

    fetchCurrentUserOrNullMock.mockResolvedValue({
        id: 123,
        email: 'test@example.com',
    });

    routerModule = await import('../src/router.js');

    await Promise.resolve();
});

describe('router', () => {
    test('bootstraps current user on startup', () => {
        expect(bootstrapTokenFromSessionMock).toHaveBeenCalledTimes(1);
        expect(fetchCurrentUserOrNullMock).toHaveBeenCalledTimes(1);

        expect(routerModule.getCurrentUser()).toEqual({
            id: 123,
            email: 'test@example.com',
        });
    });

    test('render() clears the view and uses fallback "/" for an unknown path', async () => {
        const prevClear = clearViewMock.mock.calls.length;
        const prevLanding = mountLandingMock.mock.calls.length;

        window.location.pathname = '/unknown';

        await routerModule.render();

        expect(clearViewMock.mock.calls.length).toBe(prevClear + 1);
        expect(mountLandingMock.mock.calls.length).toBe(prevLanding + 1);
    });

    test('navigate() does nothing if path is the same', async () => {
        window.location.pathname = '/app';

        const prevPush = window.history.pushState.mock.calls.length;
        const prevClear = clearViewMock.mock.calls.length;
        const prevMenu = mountMenupageMock.mock.calls.length;

        await routerModule.navigate('/app');

        expect(window.history.pushState.mock.calls.length).toBe(prevPush);
        expect(clearViewMock.mock.calls.length).toBe(prevClear);
        expect(mountMenupageMock.mock.calls.length).toBe(prevMenu);
    });
});
