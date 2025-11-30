import usersHtml from './users-access.html?raw';
import './users-access.css';
import { setView } from '../../shared/mount.js';
import { initUsersAccess } from './users-access.js';

export function mountUsersAccess() {
    setView(`<section class="users-page">${usersHtml}</section>`);
    initUsersAccess();
}
