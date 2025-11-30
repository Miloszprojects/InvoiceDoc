import { apiGet, apiPost, HttpError } from './httpClient.js';
import {
    setToken,
    clearToken,
    isAuthenticated,
    loadTokenFromSession,
} from './auth/tokenStorage.js';

export function bootstrapTokenFromSession() {
    loadTokenFromSession();
}

export async function registerUser(payload) {
    const { email, password, fullName, organisation, role } = payload;

    if (!email || !password) {
        throw new Error('Email and password are required.');
    }

    const body = {
        username: email,
        email,
        password,
        fullName,
        organizationName: organisation,
        role,
    };

    return apiPost('/auth/register', body, { withAuth: false });
}

export async function loginUser({ email, password }) {
    if (!email || !password) {
        throw new Error('Email and password are required.');
    }

    const body = {
        username: email,
        password,
    };

    const data = await apiPost('/auth/login', body, { withAuth: false });

    if (!data || typeof data.token !== 'string') {
        throw new Error('Login response does not contain a token.');
    }

    setToken(data.token);
    return data.token;
}

export async function fetchCurrentUserOrNull() {
    if (!isAuthenticated()) return null;

    try {
        const user = await apiGet('/auth/me', { withAuth: true });
        return user;
    } catch (err) {
        if (err instanceof HttpError && (err.status === 401 || err.status === 403)) {
            clearToken();
            return null;
        }
        throw err;
    }
}

export function logout() {
    clearToken();
}

export { isAuthenticated } from './auth/tokenStorage.js';
