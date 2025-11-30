import { describe, test, expect, beforeEach } from 'vitest';

let landingModule;

beforeEach(async () => {
    document.body.innerHTML = '';
    document.title = 'Original title';

    landingModule = await import(
        '../../../src/component/landing-page/landing.js'
        );
});

describe('initLanding', () => {
    test('does nothing if .landing root is missing', () => {
        document.body.innerHTML = `
      <main id="app">
        <div id="view-root">
          <p>No landing here</p>
        </div>
      </main>
    `;

        landingModule.initLanding();
        expect(document.title).toBe('Original title');
    });

    test('sets document.title and wires hover handlers for role cards', () => {
        document.body.innerHTML = `
      <main id="app">
        <div id="view-root">
          <section class="landing">
            <header class="landing-hero">
              <div class="hero-inner">
                <span class="hero-pill">Modern online invoicing for teams</span>
                <h1>InvoiceApp for your business</h1>
              </div>
            </header>

            <section class="landing-section">
              <div class="role-tags">
                <div class="role-tag" data-test="owner"></div>
                <div class="role-tag" data-test="accountant"></div>
                <div class="role-tag" data-test="admin"></div>
              </div>
            </section>
          </section>
        </div>
      </main>
    `;

        landingModule.initLanding();

        expect(document.title).toBe('InvoiceApp – Online invoicing for teams');

        const ownerCard = document.querySelector('[data-test="owner"]');
        expect(ownerCard).not.toBeNull();

        expect(ownerCard.classList.contains('role-tag--active')).toBe(false);

        ownerCard.dispatchEvent(new Event('mouseenter', { bubbles: true }));

        expect(ownerCard.classList.contains('role-tag--active')).toBe(true);

        ownerCard.dispatchEvent(new Event('mouseleave', { bubbles: true }));

        expect(ownerCard.classList.contains('role-tag--active')).toBe(false);
    });
});
