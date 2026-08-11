import { describe, expect, it } from 'vitest';

import { ComparisonBandBadge, SufficiencyBadge } from '@/features/recovery/components/ComparisonBandBadge';
import { render, screen } from '@/test/utils';

describe('ComparisonBandBadge', () => {
  it('renders the neutral label for a comparison band', () => {
    render(<ComparisonBandBadge band="ABOVE_BASELINE" />);
    expect(screen.getByText('Above baseline')).toBeInTheDocument();
  });

  it('renders "Not available" when no band is present', () => {
    render(<ComparisonBandBadge band={null} />);
    expect(screen.getByText('Not available')).toBeInTheDocument();
  });
});

describe('SufficiencyBadge', () => {
  it('renders the mandated sufficiency copy', () => {
    render(<SufficiencyBadge sufficiency="INSUFFICIENT" />);
    expect(screen.getByText('Not enough prior data')).toBeInTheDocument();
  });

  it('renders "Baseline established" for sufficient data', () => {
    render(<SufficiencyBadge sufficiency="SUFFICIENT" />);
    expect(screen.getByText('Baseline established')).toBeInTheDocument();
  });
});
