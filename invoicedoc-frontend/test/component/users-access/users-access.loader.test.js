import { describe, test, expect, vi, beforeEach } from 'vitest';

const setViewMock = vi.fn();
const initUsersAccessMock = vi.fn();

vi.mock('../../../src/shared/mount.js', () => ({
    setView: setViewMock,
}));

vi.mock('../../../src/component/users-access/users-access.js', () => ({
    initUsersAccess: initUsersAccessMock,
}));

let loaderModule;

beforeEach(async () => {
    vi.clearAllMocks();
    loaderModule = await import(
        '../../../src/component/users-access/loader.users-access.js'
        );
});

describe('mountUsersAccess loader', () => {
    test('renders users-access HTML and calls initUsersAccess()', () => {
        loaderModule.mountUsersAccess();

        expect(setViewMock).toHaveBeenCalledTimes(1);

        const [html] = setViewMock.mock.calls[0];

        expect(html).toContain('class="users-page"');
        expect(html).toContain('data-users-table-body');
        expect(html).toContain('id="users-error"');

        expect(initUsersAccessMock).toHaveBeenCalledTimes(1);
    });
});
