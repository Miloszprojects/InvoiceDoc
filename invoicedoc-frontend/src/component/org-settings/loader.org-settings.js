import orgHtml from './org-settings.html?raw';
import './org-settings.css';
import { setView } from '../../shared/mount.js';
import { initOrgSettings } from './org-settings.js';

export function mountOrgSettings() {
    setView(`<section class="org-settings-root">${orgHtml}</section>`);
    initOrgSettings();
}
