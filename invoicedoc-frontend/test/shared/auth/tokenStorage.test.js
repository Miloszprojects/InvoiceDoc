import { describe, test, expect, beforeEach, vi } from 'vitest';

vi.mock('../../../src/shared/config.js', () => ({
    SESSION_STORAGE_KEY: 'invoicedoc.jwt',
}));

let tokenStorage;

let getItemMock;
let setItemMock;
let removeItemMock;

beforeEach(async () => {
    vi.resetModules();

    getItemMock = vi.fn();
    setItemMock = vi.fn();
    removeItemMock = vi.fn();

    globalThis.sessionStorage = {
        getItem: getItemMock,
        setItem: setItemMock,
        removeItem: removeItemMock,
    };

    tokenStorage = await import('../../../src/shared/auth/tokenStorage.js');
});

describe('loadTokenFromSession', () => {
    test('returns null and clears inMemoryToken when there is no item', () => {
        getItemMock.mockReturnValue(null);

        const result = tokenStorage.loadTokenFromSession();

        expect(result).toBeNull();
        expect(getItemMock).toHaveBeenCalledWith('invoicedoc.jwt');
        expect(removeItemMock).not.toHaveBeenCalled();
        expect(tokenStorage.getToken()).toBeNull();
    });

    test('loads valid token from sessionStorage', () => {
        const stored = JSON.stringify({ token: 'abc123' });
        getItemMock.mockReturnValue(stored);

        const result = tokenStorage.loadTokenFromSession();

        expect(result).toBe('abc123');
        expect(tokenStorage.getToken()).toBe('abc123');
        expect(removeItemMock).not.toHaveBeenCalled();
    });

    test('ignores item if JSON is invalid and removes it', () => {
        getItemMock.mockReturnValue('not-json');

        const result = tokenStorage.loadTokenFromSession();

        expect(result).toBeNull();
        expect(removeItemMock).toHaveBeenCalledWith('invoicedoc.jwt');
        expect(tokenStorage.getToken()).toBeNull();
    });

    test('ignores item if JSON has no string token', () => {
        const stored = JSON.stringify({ token: 123 });
        getItemMock.mockReturnValue(stored);

        const result = tokenStorage.loadTokenFromSession();

        expect(result).toBeNull();
        expect(removeItemMock).not.toHaveBeenCalled();
        expect(tokenStorage.getToken()).toBeNull();
    });
});

describe('setToken', () => {
    test('stores token in memory and in sessionStorage', () => {
        tokenStorage.setToken('xyz789');

        expect(tokenStorage.getToken()).toBe('xyz789');
        expect(setItemMock).toHaveBeenCalledWith(
            'invoicedoc.jwt',
            JSON.stringify({ token: 'xyz789' }),
        );
        expect(removeItemMock).not.toHaveBeenCalled();
    });

    test('clears storage when token is falsy', () => {
        tokenStorage.setToken(null);

        expect(tokenStorage.getToken()).toBeNull();
        expect(removeItemMock).toHaveBeenCalledWith('invoicedoc.jwt');
        expect(setItemMock).not.toHaveBeenCalled();
    });
});

describe('clearToken', () => {
    test('clears inMemoryToken and removes from sessionStorage', () => {
        tokenStorage.setToken('something');
        tokenStorage.clearToken();

        expect(tokenStorage.getToken()).toBeNull();
        expect(removeItemMock).toHaveBeenCalledWith('invoicedoc.jwt');
    });
});

describe('isAuthenticated', () => {
    test('returns true when inMemoryToken is a non-empty string', () => {
        tokenStorage.setToken('token');
        expect(tokenStorage.isAuthenticated()).toBe(true);
    });

    test('returns false when there is no token', () => {
        tokenStorage.setToken(null);
        expect(tokenStorage.isAuthenticated()).toBe(false);
    });

    test('returns false when token is empty string', () => {
        tokenStorage.setToken('');
        expect(tokenStorage.isAuthenticated()).toBe(false);
    });
});
