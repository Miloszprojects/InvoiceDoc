import { describe, test, expect, vi, beforeEach } from 'vitest';

const apiGetMock = vi.fn();
const apiPostMock = vi.fn();
class MockHttpError extends Error {
    constructor(message, status, body) {
        super(message);
        this.name = 'HttpError';
        this.status = status;
        this.body = body;
    }
}

const setTokenMock = vi.fn();
const clearTokenMock = vi.fn();
const isAuthenticatedMock = vi.fn();
const loadTokenFromSessionMock = vi.fn();

vi.mock('../../src/shared/httpClient.js', () => ({
    apiGet: apiGetMock,
    apiPost: apiPostMock,
    HttpError: MockHttpError,
}));

vi.mock('../../src/shared/auth/tokenStorage.js', () => ({
    setToken: setTokenMock,
    clearToken: clearTokenMock,
    isAuthenticated: isAuthenticatedMock,
    loadTokenFromSession: loadTokenFromSessionMock,
}));

let authApi;

beforeEach(async () => {
    vi.resetModules();

    apiGetMock.mockReset();
    apiPostMock.mockReset();
    setTokenMock.mockReset();
    clearTokenMock.mockReset();
    isAuthenticatedMock.mockReset();
    loadTokenFromSessionMock.mockReset();

    authApi = await import('../../src/shared/authApi.js');
});

describe('bootstrapTokenFromSession', () => {
    test('calls loadTokenFromSession', () => {
        authApi.bootstrapTokenFromSession();

        expect(loadTokenFromSessionMock).toHaveBeenCalledTimes(1);
    });
});

describe('registerUser', () => {
    test('throws when email or password is missing', async () => {
        await expect(
            authApi.registerUser({
                email: '',
                password: '',
                fullName: 'John Doe',
                organisation: 'Org',
                role: 'USER',
            }),
        ).rejects.toThrow('Email and password are required.');

        await expect(
            authApi.registerUser({
                email: 'john@example.com',
                fullName: 'John Doe',
                organisation: 'Org',
                role: 'USER',
            }),
        ).rejects.toThrow('Email and password are required.');
    });

    test('calls apiPost with correct payload and withAuth: false', async () => {
        apiPostMock.mockResolvedValue({ success: true });

        const payload = {
            email: 'john@example.com',
            password: 'secret',
            fullName: 'John Doe',
            organisation: 'ACME',
            role: 'ADMIN',
        };

        const result = await authApi.registerUser(payload);

        expect(apiPostMock).toHaveBeenCalledTimes(1);
        expect(apiPostMock).toHaveBeenCalledWith(
            '/auth/register',
            {
                username: payload.email,
                email: payload.email,
                password: payload.password,
                fullName: payload.fullName,
                organizationName: payload.organisation,
                role: payload.role,
            },
            { withAuth: false },
        );

        expect(result).toEqual({ success: true });
    });
});

describe('loginUser', () => {
    test('throws when email or password is missing', async () => {
        await expect(
            authApi.loginUser({ email: '', password: '' }),
        ).rejects.toThrow('Email and password are required.');

        await expect(
            authApi.loginUser({ email: 'john@example.com' }), // brak password
        ).rejects.toThrow('Email and password are required.');
    });

    test('calls apiPost with correct payload and stores token', async () => {
        apiPostMock.mockResolvedValue({ token: 'jwt-token-123' });

        const token = await authApi.loginUser({
            email: 'john@example.com',
            password: 'secret',
        });

        expect(apiPostMock).toHaveBeenCalledTimes(1);
        expect(apiPostMock).toHaveBeenCalledWith(
            '/auth/login',
            { username: 'john@example.com', password: 'secret' },
            { withAuth: false },
        );

        expect(setTokenMock).toHaveBeenCalledTimes(1);
        expect(setTokenMock).toHaveBeenCalledWith('jwt-token-123');
        expect(token).toBe('jwt-token-123');
    });

    test('throws if response does not contain a token', async () => {
        apiPostMock.mockResolvedValue({}); // brak tokena

        await expect(
            authApi.loginUser({ email: 'john@example.com', password: 'secret' }),
        ).rejects.toThrow('Login response does not contain a token.');
    });
});

describe('fetchCurrentUserOrNull', () => {
    test('returns null when not authenticated', async () => {
        isAuthenticatedMock.mockReturnValue(false);

        const result = await authApi.fetchCurrentUserOrNull();

        expect(result).toBeNull();
        expect(apiGetMock).not.toHaveBeenCalled();
    });

    test('returns user when authenticated and /auth/me succeeds', async () => {
        isAuthenticatedMock.mockReturnValue(true);
        const user = { id: 1, email: 'john@example.com' };

        apiGetMock.mockResolvedValue(user);

        const result = await authApi.fetchCurrentUserOrNull();

        expect(apiGetMock).toHaveBeenCalledTimes(1);
        expect(apiGetMock).toHaveBeenCalledWith('/auth/me', { withAuth: true });
        expect(result).toEqual(user);
    });

    test('clears token and returns null on HttpError 401/403', async () => {
        isAuthenticatedMock.mockReturnValue(true);

        apiGetMock.mockRejectedValue(new MockHttpError('Unauthorized', 401));

        const result = await authApi.fetchCurrentUserOrNull();

        expect(clearTokenMock).toHaveBeenCalledTimes(1);
        expect(result).toBeNull();
    });

    test('rethrows non-auth errors', async () => {
        isAuthenticatedMock.mockReturnValue(true);

        apiGetMock.mockRejectedValue(new MockHttpError('Server error', 500));

        await expect(authApi.fetchCurrentUserOrNull()).rejects.toBeInstanceOf(
            MockHttpError,
        );
        expect(clearTokenMock).not.toHaveBeenCalled();
    });

    test('rethrows non-HttpError errors', async () => {
        isAuthenticatedMock.mockReturnValue(true);

        apiGetMock.mockRejectedValue(new Error('boom'));

        await expect(authApi.fetchCurrentUserOrNull()).rejects.toThrow('boom');
        expect(clearTokenMock).not.toHaveBeenCalled();
    });
});

describe('logout', () => {
    test('calls clearToken', () => {
        authApi.logout();

        expect(clearTokenMock).toHaveBeenCalledTimes(1);
    });
});
