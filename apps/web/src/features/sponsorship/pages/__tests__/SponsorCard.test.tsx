import { describe, expect, it, vi } from 'vitest';

import { SponsorCard } from '@/features/sponsorship/components/SponsorCard';
import { SponsorPlacementSlot } from '@/features/sponsorship/components/SponsorPlacementSlot';
import { renderWithProviders, screen } from '@/test/utils';

const placement = {
  placementId: 'p1',
  campaignId: 'c1',
  sponsorId: 's1',
  sponsorDisplayName: 'Acme',
  label: 'Sponsored',
  headline: 'Hydrate with Acme',
  body: 'Partner support for training days.',
  ctaLabel: 'Learn more',
  destinationUrl: 'https://acme.example/offer',
  creativeUrl: null,
  surface: 'HOME' as const,
};

vi.mock('@/features/sponsorship/hooks/useSponsorship', () => ({
  usePlacement: vi.fn(),
}));

import { usePlacement } from '@/features/sponsorship/hooks/useSponsorship';

describe('SponsorCard', () => {
  it('labels partner content and does not look like readiness copy', () => {
    const onVisit = vi.fn();
    renderWithProviders(<SponsorCard placement={placement} onVisit={onVisit} />);
    expect(screen.getByLabelText('Sponsored partner content')).toBeInTheDocument();
    expect(screen.getByText('Sponsored')).toBeInTheDocument();
    expect(screen.getByText('Acme')).toBeInTheDocument();
    expect(screen.getByRole('link')).toHaveAttribute('href', 'https://acme.example/offer');
    expect(screen.queryByText(/readiness/i)).not.toBeInTheDocument();
  });
});

describe('SponsorPlacementSlot', () => {
  it('renders nothing without a placement', () => {
    vi.mocked(usePlacement).mockReturnValue({
      data: null,
      isLoading: false,
      isError: false,
      recordClick: vi.fn(),
    } as never);
    const { container } = renderWithProviders(<SponsorPlacementSlot surface="HOME" />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders a labeled card when the API returns a placement', () => {
    vi.mocked(usePlacement).mockReturnValue({
      data: placement,
      isLoading: false,
      isError: false,
      recordClick: vi.fn(),
    } as never);
    renderWithProviders(<SponsorPlacementSlot surface="HOME" />);
    expect(screen.getByTestId('sponsor-card')).toBeInTheDocument();
  });
});
