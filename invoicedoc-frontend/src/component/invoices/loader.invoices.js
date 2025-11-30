import invoicesHtml from './invoices.html?raw';
import './invoices.css';
import { initInvoices } from './invoices.js';
import { setView } from '../../shared/mount.js';

export function mountInvoices() {
    setView(`<section class="invoices-page">${invoicesHtml}</section>`);
    initInvoices();
}