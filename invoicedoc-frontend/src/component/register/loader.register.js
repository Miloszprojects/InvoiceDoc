import registerHtml from './register.html?raw';
import './register.css';
import { initRegister } from './register.js';
import { setView } from '../../shared/mount.js';

export function mountRegister() {
    setView(`<section class="auth auth--register">${registerHtml}</section>`);
    initRegister();
}
