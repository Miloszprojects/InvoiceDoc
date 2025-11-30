import { registerUser } from '../../shared/authApi.js';
import { HttpError } from '../../shared/httpClient.js';

export function initRegister() {
    const form = document.getElementById('register-form');
    const errorBox = document.getElementById('register-error');
    if (!form) return;

    hideError(errorBox);

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideError(errorBox);

        const formData = new FormData(form);
        const payload = {
            fullName: formData.get('fullName')?.toString().trim(),
            email: formData.get('email')?.toString().trim(),
            organisation: formData.get('organisation')?.toString().trim(),
            password: formData.get('password')?.toString() ?? '',
            confirmPassword:
                formData.get('confirmPassword')?.toString() ?? '',
            role: formData.get('role')?.toString() || 'OWNER',
        };

        if (!payload.fullName || !payload.email || !payload.organisation) {
            return showError('Please fill in all required fields.', errorBox);
        }

        if (payload.password.length < 8) {
            return showError(
                'Password should be at least 8 characters long.',
                errorBox,
            );
        }

        if (payload.password !== payload.confirmPassword) {
            return showError('Passwords do not match.', errorBox);
        }

        try {
            const result = await registerUser(payload);

            const params = new URLSearchParams({
                registered: '1',
                role: result.role,
            });
            window.location.href = `/auth/login?${params.toString()}`;
        } catch (err) {
            if (err instanceof HttpError) {
                const { status, body } = err;

                let code = null;
                if (body && typeof body === 'object') {
                    code = body.message || null;
                } else if (typeof body === 'string') {
                    code = body;
                }

                if (status === 409 && code === 'EMAIL_EXISTS') {
                    return showError(
                        'An account with this email already exists. You can sign in or use a different address.',
                        errorBox,
                    );
                }

                if (status === 403 && code === 'ORG_ALREADY_HAS_ADMIN') {
                    return showError(
                        'This organisation already has an Admin. Ask them to add you as an Owner or Accountant, or choose a different organisation name.',
                        errorBox,
                    );
                }

                if (status === 400) {
                    if (code === 'ORG_NAME_REQUIRED') {
                        return showError(
                            'Organisation name is required.',
                            errorBox,
                        );
                    }
                    if (code === 'ORG_NOT_FOUND') {
                        if (payload.role === 'OWNER' || payload.role === 'ACCOUNTANT') {
                            return showError(
                                'This organisation does not exist yet. Ask an Admin to create it first, or check for typos in the name.',
                                errorBox,
                            );
                        }
                        return showError(
                            'Organisation not found. Please check the name and try again.',
                            errorBox,
                        );
                    }
                    if (code === 'ROLE_NOT_SUPPORTED') {
                        return showError(
                            'This role is not supported. Please choose Admin, Owner or Accountant.',
                            errorBox,
                        );
                    }
                }
            }
            console.error('Register error', err);
            showError(
                'Something went wrong while creating your account. Please try again.',
                errorBox,
            );
        }
    });
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
