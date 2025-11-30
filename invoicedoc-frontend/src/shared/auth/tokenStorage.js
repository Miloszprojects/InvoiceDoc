import { SESSION_STORAGE_KEY } from '../config.js';

let inMemoryToken = null;

export function loadTokenFromSession() {
    const raw = sessionStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) {
        inMemoryToken = null;
        return null;
    }

    try {
        const parsed = JSON.parse(raw);
        if (typeof parsed?.token === 'string') {
            inMemoryToken = parsed.token;
            return inMemoryToken;
        }
    } catch {
        sessionStorage.removeItem(SESSION_STORAGE_KEY);
    }

    inMemoryToken = null;
    return null;
}

export function setToken(token) {
    inMemoryToken = token || null;

    if (token) {
        const payload = JSON.stringify({ token });
        sessionStorage.setItem(SESSION_STORAGE_KEY, payload);
    } else {
        sessionStorage.removeItem(SESSION_STORAGE_KEY);
    }
}

export function getToken() {
    return inMemoryToken;
}

export function clearToken() {
    inMemoryToken = null;
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
}

export function isAuthenticated() {
    return typeof inMemoryToken === 'string' && inMemoryToken.length > 0;
}
