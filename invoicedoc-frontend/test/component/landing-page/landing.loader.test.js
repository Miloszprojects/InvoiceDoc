import { describe, test, expect, vi, beforeEach } from 'vitest';

const setViewMock = vi.fn();
const initLandingMock = vi.fn();

vi.mock('../../../src/component/landing-page/landing.html?raw', () => ({
    default: '<header class="landing-hero">Landing</header>',
}));

vi.mock('../../../src/shared/mount.js', () => ({
    setView: setViewMock,
}));

vi.mock('../../../src/component/landing-page/landing.js', () => ({
    initLanding: initLandingMock,
}));

let loaderModule;

beforeEach(async () => {
    vi.resetModules();
    setViewMock.mockReset();
    initLandingMock.mockReset();

    loaderModule = await import(
        '../../../src/component/landing-page/loader.landing.js'
        );
});

describe('mountLanding', () => {
    test('renders landing HTML into view-root and calls initLanding()', () => {
        loaderModule.mountLanding();

        expect(setViewMock).toHaveBeenCalledTimes(1);

        const [html] = setViewMock.mock.calls[0];
        expect(typeof html).toBe('string');
        expect(html).toContain('class="landing"');
        expect(html).toContain('class="landing-hero"');

        expect(initLandingMock).toHaveBeenCalledTimes(1);
    });
});
