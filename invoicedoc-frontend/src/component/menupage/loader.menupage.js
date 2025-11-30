import menupageHtml from './menupage.html?raw';
import './menupage.css';
import { initMenupage } from './menupage.js';
import { setView } from '../../shared/mount.js';

export function mountMenupage() {
    setView(`<section class="menupage">${menupageHtml}</section>`);
    initMenupage();
}
