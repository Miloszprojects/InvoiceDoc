import { describe, test, expect, vi, beforeEach } from 'vitest';

const setViewMock = vi.fn();
const initContractorsMock = vi.fn();

vi.mock('../../../src/shared/mount.js', () => ({
    setView: setViewMock,
}));

vi.mock('../../../src/component/contractors/contractors.js', () => ({
    initContractors: initContractorsMock,
}));

let loader;

beforeEach(async () => {
    vi.resetModules();
    setViewMock.mockReset();
    initContractorsMock.mockReset();

    loader = await import('../../../src/component/contractors/loader.contractors.js');
});

describe('mountContractors', () => {
    test('renders contractors HTML into view-root and initializes contractors feature', () => {
        loader.mountContractors();

        expect(setViewMock).toHaveBeenCalledTimes(1);
        const [html] = setViewMock.mock.calls[0];

        expect(typeof html).toBe('string');
        expect(html).toContain('class="contractors-page"');

        expect(initContractorsMock).toHaveBeenCalledTimes(1);
    });
});
