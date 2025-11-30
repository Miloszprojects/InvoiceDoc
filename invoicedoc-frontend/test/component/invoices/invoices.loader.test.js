import { describe, test, expect, vi, beforeEach } from 'vitest';

const setViewMock = vi.fn();
const initInvoicesMock = vi.fn();

vi.mock('../../../src/shared/mount.js', () => ({
    setView: setViewMock,
}));

vi.mock('../../../src/component/invoices/invoices.js', () => ({
    initInvoices: initInvoicesMock,
}));

let loaderModule;

beforeEach(async () => {
    vi.resetModules();
    setViewMock.mockReset();
    initInvoicesMock.mockReset();

    loaderModule = await import(
        '../../../src/component/invoices/loader.invoices.js'
        );
});

describe('mountInvoices', () => {
    test('renders invoices HTML into view-root and initializes invoices feature', () => {
        loaderModule.mountInvoices();

        expect(setViewMock).toHaveBeenCalledTimes(1);
        const [html] = setViewMock.mock.calls[0];

        expect(typeof html).toBe('string');
        expect(html).toContain('class="invoices-page"');
        expect(html).toContain('<section class="invoices">');

        expect(initInvoicesMock).toHaveBeenCalledTimes(1);
    });
});
