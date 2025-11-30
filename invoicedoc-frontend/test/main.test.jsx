import { describe, test, expect, vi } from 'vitest';

const renderMock = vi.fn();

vi.mock('react-dom/client', () => {
    return {
        __esModule: true,
        createRoot: vi.fn(() => ({
            render: renderMock,
        })),
    };
});

describe('main entry file', () => {
    test('mounts <App /> into #root using React 18 createRoot', async () => {
        const rootElement = document.createElement('div');
        rootElement.id = 'root';
        document.body.appendChild(rootElement);

        await import('../src/main.jsx');

        const { createRoot } = await import('react-dom/client');

        expect(createRoot).toHaveBeenCalledTimes(1);
        expect(createRoot).toHaveBeenCalledWith(rootElement);
        expect(renderMock).toHaveBeenCalledTimes(1);
    });
});
