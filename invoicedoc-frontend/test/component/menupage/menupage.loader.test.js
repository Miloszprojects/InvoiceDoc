import { describe, test, expect, vi, beforeEach } from 'vitest';

const setViewMock = vi.fn();
const initMenupageMock = vi.fn();

vi.mock('../../../src/shared/mount.js', () => ({
    setView: setViewMock,
}));

vi.mock('../../../src/component/menupage/menupage.js', () => ({
    initMenupage: initMenupageMock,
}));

let loaderModule;

beforeEach(async () => {
    vi.clearAllMocks();
    loaderModule = await import(
        '../../../src/component/menupage/loader.menupage.js'
        );
});

describe('mountMenupage loader', () => {
    test('renders menupage HTML into #view-root and calls initMenupage()', () => {
        loaderModule.mountMenupage();

        expect(setViewMock).toHaveBeenCalledTimes(1);
        const [html] = setViewMock.mock.calls[0];
        expect(html).toContain('class="menupage"');
        expect(html).toContain('data-menu-grid');
        expect(initMenupageMock).toHaveBeenCalledTimes(1);
    });
});
