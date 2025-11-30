import { describe, test, expect, vi, beforeEach } from 'vitest';

const setViewMock = vi.fn();
const initOrgSettingsMock = vi.fn();

vi.mock('../../../src/shared/mount.js', () => ({
    setView: setViewMock,
}));

vi.mock('../../../src/component/org-settings/org-settings.js', () => ({
    initOrgSettings: initOrgSettingsMock,
}));

let loaderModule;

beforeEach(async () => {
    vi.clearAllMocks();
    loaderModule = await import(
        '../../../src/component/org-settings/loader.org-settings.js'
        );
});

describe('mountOrgSettings loader', () => {
    test('renders org-settings HTML and calls initOrgSettings()', () => {
        loaderModule.mountOrgSettings();

        expect(setViewMock).toHaveBeenCalledTimes(1);

        const [html] = setViewMock.mock.calls[0];
        expect(html).toContain('class="org-settings-root"');
        expect(html).toContain('id="seller-form"');
        expect(html).toContain('data-seller-list');

        expect(initOrgSettingsMock).toHaveBeenCalledTimes(1);
    });
});
