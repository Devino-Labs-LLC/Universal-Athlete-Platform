import { describe, expect, it } from 'vitest';

import {
  OverviewSection,
  RatingRow,
  ScopeChips,
} from '@/features/coach/components/OverviewSection';
import { renderWithProviders, screen } from '@/test/utils';

describe('OverviewSection components', () => {
  it('renders available children and neutral status copy otherwise', () => {
    const { rerender } = renderWithProviders(
      <OverviewSection title="Readiness" status="AVAILABLE">
        <p>Score body</p>
      </OverviewSection>,
    );
    expect(screen.getByText('Score body')).toBeInTheDocument();

    rerender(<OverviewSection title="Readiness" status="NOT_SHARED" />);
    expect(screen.getAllByText('Not shared').length).toBeGreaterThan(0);
  });

  it('renders rating rows and empty scope chips', () => {
    renderWithProviders(
      <>
        <RatingRow label="Mood" rating={{ value: 3, label: 'OK' }} />
        <RatingRow label="Stress" rating={null} />
        <ScopeChips scopes={[]} />
        <ScopeChips scopes={['READINESS_CATEGORY']} />
      </>,
    );
    expect(screen.getByText('OK (3)')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
    expect(screen.getByText('No effective scopes for this membership.')).toBeInTheDocument();
    expect(screen.getByText('Readiness category')).toBeInTheDocument();
  });
});
