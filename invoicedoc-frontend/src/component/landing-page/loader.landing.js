import landingHtml from './landing.html?raw';
import './landing.css';
import { initLanding } from './landing.js';
import { setView } from '../../shared/mount.js';

export function mountLanding() {
    setView(`<section class="landing">${landingHtml}</section>`);
    initLanding();
}
