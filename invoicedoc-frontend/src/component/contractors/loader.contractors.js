import contractorsHtml from './contractors.html?raw';
import './contractors.css';
import { setView } from '../../shared/mount.js';
import { initContractors } from './contractors.js';

export function mountContractors() {
    setView(`<section class="contractors-page">${contractorsHtml}</section>`);
    initContractors();
}
