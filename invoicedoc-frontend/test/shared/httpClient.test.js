import { describe, test, expect, vi, beforeEach } from 'vitest';

const getTokenMock = vi.fn();
const clearTokenMock = vi.fn();

vi.mock('../../src/shared/config.js', () => ({
    API_ROOT: 'https://api.example.com',
    REQUEST_TIMEOUT_MS: 15000,
}));

vi.mock('../../src/shared/auth/tokenStorage.js', () => ({
    getToken: getTokenMock,
    clearToken: clearTokenMock,
}));

let httpClient;
let HttpError;

beforeEach(async () => {
    vi.resetModules();

    getTokenMock.mockReset();
    clearTokenMock.mockReset();

    globalThis.fetch = vi.fn();
    httpClient = await import('../../src/shared/httpClient.js');
    HttpError = httpClient.HttpError;
});

describe('httpClient apiRequest', () => {
    test('sends request with JSON body and Authorization header when token exists', async () => {
        getTokenMock.mockReturnValue('test-token');

        globalThis.fetch.mockResolvedValue({
            ok: true,
            status: 200,
            text: vi.fn().mockResolvedValue(JSON.stringify({ ok: true })),
        });

        const result = await httpClient.apiRequest('/foo', {
            method: 'POST',
            body: { hello: 'world' },
        });

        expect(result).toEqual({ ok: true });

        expect(globalThis.fetch).toHaveBeenCalledTimes(1);
        const [url, options] = globalThis.fetch.mock.calls[0];

        expect(url).toBe('https://api.example.com/foo');
        expect(options.method).toBe('POST');
        expect(options.headers['Content-Type']).toBe('application/json');
        expect(options.headers.Authorization).toBe('Bearer test-token');
        expect(options.body).toBe(JSON.stringify({ hello: 'world' }));
    });

    test('does not add Authorization header when withAuth is false', async () => {
        getTokenMock.mockReturnValue('test-token'); // nie powinno być użyte

        globalThis.fetch.mockResolvedValue({
            ok: true,
            status: 200,
            text: vi.fn().mockResolvedValue(''),
        });

        await httpClient.apiRequest('/no-auth', { withAuth: false });

        const [, options] = globalThis.fetch.mock.calls[0];

        expect(getTokenMock).not.toHaveBeenCalled();
        expect(options.headers.Authorization).toBeUndefined();
    });

    test('throws HttpError with parsed message and body on non-OK response', async () => {
        globalThis.fetch.mockResolvedValue({
            ok: false,
            status: 500,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({ message: 'Server exploded' }),
            ),
        });

        await expect(httpClient.apiRequest('/error')).rejects.toBeInstanceOf(
            HttpError,
        );

        await expect(httpClient.apiRequest('/error')).rejects.toMatchObject({
            name: 'HttpError',
            status: 500,
            message: 'Server exploded',
            body: { message: 'Server exploded' },
        });
    });

    test('on 401 clears token and throws HttpError', async () => {
        globalThis.fetch.mockResolvedValue({
            ok: false,
            status: 401,
            text: vi.fn().mockResolvedValue(
                JSON.stringify({ message: 'Unauthorized' }),
            ),
        });

        await expect(httpClient.apiRequest('/me')).rejects.toBeInstanceOf(
            HttpError,
        );

        expect(clearTokenMock).toHaveBeenCalledTimes(1);
    });

    test('maps AbortError to timeout message', async () => {
        globalThis.fetch.mockRejectedValue({ name: 'AbortError' });

        await expect(httpClient.apiRequest('/timeout')).rejects.toThrow(
            'Request timeout – please try again.',
        );
    });

    test('maps other fetch errors to network error message', async () => {
        globalThis.fetch.mockRejectedValue(new Error('connection lost'));

        await expect(httpClient.apiRequest('/network')).rejects.toThrow(
            'Network error – please check your connection.',
        );
    });
});

describe('httpClient apiGet/apiPost helpers', () => {
    test('apiGet performs a GET request via apiRequest', async () => {
        globalThis.fetch.mockResolvedValue({
            ok: true,
            status: 200,
            text: vi.fn().mockResolvedValue(JSON.stringify({ hello: 'get' })),
        });

        const result = await httpClient.apiGet('/hello');

        expect(result).toEqual({ hello: 'get' });

        expect(globalThis.fetch).toHaveBeenCalledTimes(1);
        const [url, options] = globalThis.fetch.mock.calls[0];

        expect(url).toBe('https://api.example.com/hello');
        expect(options.method).toBe('GET');
    });

    test('apiPost performs a POST request with body via apiRequest', async () => {
        globalThis.fetch.mockResolvedValue({
            ok: true,
            status: 200,
            text: vi.fn().mockResolvedValue(JSON.stringify({ hello: 'post' })),
        });

        const payload = { foo: 'bar' };

        const result = await httpClient.apiPost('/hello', payload);

        expect(result).toEqual({ hello: 'post' });

        expect(globalThis.fetch).toHaveBeenCalledTimes(1);
        const [url, options] = globalThis.fetch.mock.calls[0];

        expect(url).toBe('https://api.example.com/hello');
        expect(options.method).toBe('POST');
        expect(options.body).toBe(JSON.stringify(payload));
    });
});
