import { describe, expect, it } from 'vitest';

import { ScoreRing } from '@/core/components/ScoreRing';
import { render, screen } from '@/test/utils';

describe('ScoreRing', () => {
  it('exposes score in accessible label when present', () => {
    render(<ScoreRing score={82} label="Readiness" />);
    expect(screen.getByRole('img', { name: 'Readiness: 82 of 100' })).toBeInTheDocument();
    expect(screen.getByText('82')).toBeInTheDocument();
  });

  it('renders empty state without inventing a score', () => {
    render(<ScoreRing score={null} label="Readiness" emptyLabel="—" />);
    expect(screen.getByRole('img', { name: 'Readiness: not available' })).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
