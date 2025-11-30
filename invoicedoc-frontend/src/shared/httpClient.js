import { API_ROOT, REQUEST_TIMEOUT_MS } from './config.js';
import { getToken, clearToken } from './auth/tokenStorage.js';

export class HttpError extends Error {
    constructor(message, status, body) {
        super(message);
        this.name = 'HttpError';
        this.status = status;
        this.body = body;
    }
}

export async function apiRequest(path, options = {}) {
    const {
        method = 'GET',
        body,
        headers = {},
        timeoutMs = REQUEST_TIMEOUT_MS,
        withAuth = true,
    } = options;

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    const finalHeaders = {
        'Content-Type': 'application/json',
        ...headers,
    };

    if (withAuth) {
        const token = getToken();
        if (token) {
            finalHeaders.Authorization = `Bearer ${token}`;
        }
    }

    let res;
    try {
        res = await fetch(`${API_ROOT}${path}`, {
            method,
            headers: finalHeaders,
            body: body ? JSON.stringify(body) : undefined,
            signal: controller.signal,
            credentials: 'same-origin',
        });
    } catch (err) {
        clearTimeout(timeoutId);

        if (err.name === 'AbortError') {
            throw new Error('Request timeout – please try again.');
        }
        throw new Error('Network error – please check your connection.');
    }

    clearTimeout(timeoutId);

    let parsedBody = null;
    let rawText = '';

    try {
        rawText = await res.text();
        if (rawText) {
            try {
                parsedBody = JSON.parse(rawText);
            } catch {
                parsedBody = rawText;
            }
        }
    } catch {
        // eslint error when no comment
    }

    if (!res.ok) {
        const message =
            (parsedBody && typeof parsedBody === 'object' && parsedBody.message) ||
            (typeof parsedBody === 'string' ? parsedBody : null) ||
            `Request failed with status ${res.status}`;

        if (res.status === 401) {
            clearToken();
        }

        throw new HttpError(message, res.status, parsedBody);
    }

    return parsedBody;
}

export function apiGet(path, options = {}) {
    return apiRequest(path, { ...options, method: 'GET' });
}

export function apiPost(path, body, options = {}) {
    return apiRequest(path, { ...options, method: 'POST', body });
}
