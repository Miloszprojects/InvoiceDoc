import loginHtml from './login.html?raw';
import './login.css';
import { initLogin } from './login.js';
import { setView } from '../../shared/mount.js';

export function mountLogin() {
    setView(`<section class="auth auth--login">${loginHtml}</section>`);
    initLogin();
}
