export function setView(html) {
    const root = document.getElementById('view-root');
    if (!root) {
        throw new Error('#view-root not found (sprawdź App.jsx)');
    }
    root.innerHTML = html;
    window.scrollTo({ top: 0, behavior: 'instant' });
}

export function clearView() {
    const root = document.getElementById('view-root');
    if (root) {
        root.innerHTML = '';
    }
}