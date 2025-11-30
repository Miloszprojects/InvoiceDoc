import { loginUser } from '../../shared/authApi.js';
import { HttpError } from '../../shared/httpClient.js';

export function initLogin() {
    const form = document.getElementById('login-form');
    const errorBox = document.getElementById('login-error');
    const infoBox = document.getElementById('login-info');

    if (!form) return;

    showInfoIfJustRegistered(infoBox);
    hideError(errorBox);

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideError(errorBox);

        const formData = new FormData(form);
        const payload = {
            email: formData.get('email')?.toString().trim(),
            password: formData.get('password')?.toString() ?? '',
        };

        if (!payload.email || !payload.password) {
            return showError(
                'Please enter your email and password.',
                errorBox,
            );
        }

        try {
            await loginUser(payload);
            window.location.href = '/app';
        } catch (err) {
            if (err instanceof HttpError) {
                const code =
                    err.body && typeof err.body === 'object'
                        ? err.body.message
                        : null;

                if (err.status === 403 && code === 'ACCOUNT_NOT_APPROVED') {
                    return showError(
                        'Your account exists but has not been approved yet. Please contact your organisation owner or admin.',
                        errorBox,
                    );
                }

                if (err.status === 401 && code === 'INVALID_CREDENTIALS') {
                    return showError(
                        'Invalid email or password.',
                        errorBox,
                    );
                }
            }

            console.error('Login error', err);
            showError(
                'Unable to sign in right now. Please try again in a moment.',
                errorBox,
            );
        }
    });
}

function showInfoIfJustRegistered(infoBox) {
    if (!infoBox) return;

    const params = new URLSearchParams(window.location.search);
    const card = document.querySelector('.auth-card');

    if (params.get('registered') !== '1') {
        infoBox.hidden = true;
        infoBox.textContent = '';
        if (card) card.classList.remove('auth-card--success');
        return;
    }

    const role = params.get('role') || 'OWNER';

    if (role === 'ACCOUNTANT') {
        infoBox.textContent =
            'Your accountant account has been created. The owner or admin needs to approve your access before you can use all features.';
    } else if (role === 'OWNER') {
        infoBox.textContent =
            'Your owner account has been created. The admin or owner from your organisation needs to approve your access before you can use the workspace.';
    } else if (role === 'ADMIN') {
        infoBox.textContent =
            'Your admin account has been created. You can sign in and start managing organisations.';
    } else {
        infoBox.textContent =
            'Your account has been created. You can sign in now.';
    }

    infoBox.hidden = false;
    if (card) card.classList.add('auth-card--success');
    const url = new URL(window.location.href);
    url.searchParams.delete('registered');
    url.searchParams.delete('role');
    window.history.replaceState({}, '', url.toString());
}

function showError(message, box) {
    if (!box) return;
    box.textContent = message;
    box.hidden = false;
}

function hideError(box) {
    if (!box) return;
    box.textContent = '';
    box.hidden = true;
}
