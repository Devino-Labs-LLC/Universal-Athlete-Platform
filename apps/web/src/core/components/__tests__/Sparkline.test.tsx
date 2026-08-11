import { describe, expect, it } from 'vitest';

import { Sparkline } from '@/core/components/Sparkline';
import { render, screen } from '@/test/utils';

describe('Sparkline', () => {
  it('renders an accessible empty state when no points have numeric values', () => {
    render(<Sparkline points={[{ label: 'a', value: null }]} ariaLabel="Fatigue trend" />);
    expect(screen.getByRole('img', { name: 'Fatigue trend: no data available' })).toBeInTheDocument();
  });

  it('renders an SVG with an accessible label including the latest value', () => {
    render(
      <Sparkline
        points={[
          { label: '2026-01-01', value: 2 },
          { label: '2026-01-02', value: 4 },
        ]}
        ariaLabel="Fatigue trend"
        valueFormatter={(v) => v.toFixed(1)}
      />,
    );
    expect(screen.getByRole('img', { name: 'Fatigue trend. Latest value: 4.0' })).toBeInTheDocument();
  });

  it('skips missing (null) points instead of treating them as zero', () => {
    const { container } = render(
      <Sparkline
        points={[
          { label: '2026-01-01', value: 10 },
          { label: '2026-01-02', value: null },
          { label: '2026-01-03', value: 10 },
        ]}
        ariaLabel="Volume trend"
      />,
    );
    // Only two numeric points should be plotted as circles, not three.
    expect(container.querySelectorAll('circle')).toHaveLength(2);
  });

  it('falls back to the raw value when no formatter is provided', () => {
    render(<Sparkline points={[{ label: 'a', value: 7 }]} ariaLabel="Plain trend" />);
    expect(screen.getByRole('img', { name: 'Plain trend. Latest value: 7' })).toBeInTheDocument();
  });
});
