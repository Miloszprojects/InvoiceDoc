import { describe, test, expect, vi, beforeEach } from 'vitest';

const setViewMock = vi.fn();
const initRegisterMock = vi.fn();

vi.mock('../../../src/shared/mount.js', () => ({
    setView: setViewMock,
}));

vi.mock('../../../src/component/register/register.js', () => ({
    initRegister: initRegisterMock,
}));

let loaderModule;

beforeEach(async () => {
    vi.clearAllMocks();
    loaderModule = await import(
        '../../../src/component/register/loader.register.js'
        );
});

describe('mountRegister loader', () => {
    test('renders register HTML and calls initRegister()', () => {
        loaderModule.mountRegister();

        expect(setViewMock).toHaveBeenCalledTimes(1);

        const [html] = setViewMock.mock.calls[0];
        expect(html).toContain('class="auth auth--register"');
        expect(html).toContain('id="register-form"');

        expect(initRegisterMock).toHaveBeenCalledTimes(1);
    });
});
