import { fireEvent, render } from '@testing-library/react-native';
import { Linking } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { SponsorCard } from '@/src/features/sponsorship/components/SponsorCard';
import { SponsorPlacementSlot } from '@/src/features/sponsorship/components/SponsorPlacementSlot';

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

jest.mock('@/src/features/sponsorship/hooks/usePlacement', () => ({
  usePlacement: jest.fn(),
}));

import { usePlacement } from '@/src/features/sponsorship/hooks/usePlacement';

describe('SponsorCard', () => {
  it('labels sponsored content and opens the HTTPS destination', async () => {
    const open = jest.spyOn(Linking, 'openURL').mockResolvedValue(undefined);
    const onVisit = jest.fn();
    const { getByText, getByLabelText, queryByText } = await render(
      <ThemeProvider>
        <SponsorCard placement={placement} onVisit={onVisit} />
      </ThemeProvider>,
    );
    expect(getByLabelText('Sponsored partner content')).toBeTruthy();
    expect(getByText('Sponsored')).toBeTruthy();
    expect(queryByText(/readiness/i)).toBeNull();
    fireEvent.press(getByText('Learn more'));
    expect(onVisit).toHaveBeenCalled();
    expect(open).toHaveBeenCalledWith('https://acme.example/offer');
    open.mockRestore();
  });
});

describe('SponsorPlacementSlot', () => {
  it('renders nothing without a placement', async () => {
    (usePlacement as jest.Mock).mockReturnValue({
      data: null,
      isLoading: false,
      isError: false,
      recordClick: jest.fn(),
    });
    const { toJSON } = await render(
      <ThemeProvider>
        <SponsorPlacementSlot surface="HOME" />
      </ThemeProvider>,
    );
    expect(toJSON()).toBeNull();
  });
});
