import { describe, test, expect } from 'vitest';
import { render } from '@testing-library/react';
import App from '../src/App.jsx';

describe('App component', () => {
    test('renders a <main> element with id "app"', () => {
        const { container } = render(<App />);

        const main = container.querySelector('main#app');
        expect(main).toBeInTheDocument();
        expect(main).toHaveClass('page');
    });

    test('contains a container with id "view-root" inside <main>', () => {
        const { container } = render(<App />);

        const viewRoot = container.querySelector('#view-root');
        expect(viewRoot).toBeInTheDocument();
        expect(viewRoot.parentElement?.id).toBe('app');
    });
});
