import { describe, expect, it } from 'vitest';

import { Page } from '@/core/components/Page';
import { render, screen } from '@/test/utils';

describe('Page', () => {
  it('renders the page title as the primary heading', () => {
    render(
      <Page title="Training load" description="History">
        <p>Content</p>
      </Page>,
    );
    expect(screen.getByRole('heading', { level: 1, name: 'Training load' })).toBeInTheDocument();
    expect(screen.getByText('History')).toBeInTheDocument();
  });

  it('supports padded mode for pages outside AppShell', () => {
    const { container } = render(
      <Page title="Unable to start" padded>
        <p>Bootstrap failed</p>
      </Page>,
    );
    expect(container.querySelector('section')?.className).toMatch(/padded/);
  });
});
