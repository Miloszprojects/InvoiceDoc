import { describe, test, expect, beforeEach, vi } from 'vitest';
import { setView, clearView } from '../../src/shared/mount.js';

describe('mount helpers', () => {
    beforeEach(() => {
        document.body.innerHTML = `
      <main id="app">
        <div id="view-root"></div>
      </main>
    `;

        if (!window.scrollTo) {
            window.scrollTo = vi.fn();
        } else {
            vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
        }
    });

    test('setView sets innerHTML of #view-root and scrolls to top', () => {
        const html = '<p>Hello view</p>';

        setView(html);

        const root = document.getElementById('view-root');
        expect(root).not.toBeNull();
        expect(root.innerHTML).toBe(html);
        expect(window.scrollTo).toHaveBeenCalledWith({ top: 0, behavior: 'instant' });
    });

    test('clearView clears innerHTML of #view-root', () => {
        const root = document.getElementById('view-root');
        root.innerHTML = '<p>Old content</p>';

        clearView();

        expect(root.innerHTML).toBe('');
    });

    test('setView throws if #view-root does not exist', () => {
        document.body.innerHTML = '<main id="app"></main>';

        expect(() => setView('<p>x</p>')).toThrow(
            '#view-root not found (sprawdź App.jsx)',
        );
    });
});
