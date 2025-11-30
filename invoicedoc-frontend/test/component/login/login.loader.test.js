import { describe, test, expect, vi, beforeEach } from 'vitest';

const setViewMock = vi.fn();
const initLoginMock = vi.fn();

vi.mock('../../../src/component/login/login.html?raw', () => ({
    default: '<section class="auth-layout">Login markup</section>',
}));

vi.mock('../../../src/shared/mount.js', () => ({
    setView: setViewMock,
}));

vi.mock('../../../src/component/login/login.js', () => ({
    initLogin: initLoginMock,
}));

let loaderModule;

beforeEach(async () => {
    vi.resetModules();
    setViewMock.mockReset();
    initLoginMock.mockReset();

    loaderModule = await import(
        '../../../src/component/login/loader.login.js'
        );
});

describe('mountLogin', () => {
    test('wraps login HTML in <section class="auth auth--login"> and calls initLogin()', () => {
        loaderModule.mountLogin();

        expect(setViewMock).toHaveBeenCalledTimes(1);

        const [html] = setViewMock.mock.calls[0];
        expect(typeof html).toBe('string');
        expect(html).toContain('class="auth auth--login"');
        expect(html).toContain('class="auth-layout"'); // z mockowanego html

        expect(initLoginMock).toHaveBeenCalledTimes(1);
    });
});
