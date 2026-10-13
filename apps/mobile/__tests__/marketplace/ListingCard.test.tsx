import type { ReactElement } from 'react';
import { cleanup, fireEvent, render, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Linking } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { ListingCard } from '@/src/features/marketplace/components/ListingCard';
import { MarketplaceListingScreen } from '@/src/features/marketplace/screens/MarketplaceListingScreen';
import { MarketplaceScreen } from '@/src/features/marketplace/screens/MarketplaceScreen';

const listing = {
  id: 'l1',
  partnerId: 'p1',
  partnerDisplayName: 'Acme Gear',
  title: 'Recovery boots',
  shortDescription: 'Pneumatic recovery',
  fullDescription: 'Partner recovery equipment for rest days.',
  listingType: 'PRODUCT' as const,
  categoryCode: 'RECOVERY',
  categoryName: 'Recovery',
  featured: true,
  available: true,
  offerActive: true,
  imageUrl: null,
  destinationUrl: 'https://acme.example/boots',
  priceDisplay: '$199',
  discountText: '20 percent off',
  promoCode: 'BOOTS20',
  saved: false,
};

jest.mock('expo-router', () => ({
  router: { push: jest.fn() },
}));

jest.mock('@/src/features/marketplace/hooks/useMarketplace', () => ({
  useMarketplaceBrowse: jest.fn(() => ({
    data: { listings: [], page: 0, size: 20, totalElements: 0 },
    isLoading: false,
    isError: false,
  })),
  useMarketplaceCategories: jest.fn(() => ({ data: [], isLoading: false })),
  useMarketplaceListing: jest.fn(() => ({ data: undefined, isLoading: false, isError: true })),
  useMarketplaceActions: jest.fn(() => ({
    save: jest.fn(),
    unsave: jest.fn(),
    recordImpression: jest.fn(),
    recordClick: jest.fn(),
    recordRedeem: jest.fn(),
  })),
}));

const {
  useMarketplaceActions,
  useMarketplaceBrowse,
  useMarketplaceCategories,
  useMarketplaceListing,
} = jest.requireMock('@/src/features/marketplace/hooks/useMarketplace');

afterEach(() => {
  cleanup();
});

function renderScreen(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>{ui}</ThemeProvider>
    </QueryClientProvider>,
  );
}

describe('ListingCard', () => {
  it('labels partner listings without readiness language', async () => {
    const { getByLabelText, queryByText } = await renderScreen(
      <ListingCard listing={listing} onPress={jest.fn()} />,
    );
    expect(getByLabelText('Marketplace listing: Recovery boots')).toBeTruthy();
    expect(queryByText(/readiness/i)).toBeNull();
  });
});

describe('MarketplaceScreen', () => {
  it('shows empty results', async () => {
    const { getByText } = await renderScreen(<MarketplaceScreen />);
    expect(getByText(/No marketplace listings match/)).toBeTruthy();
  });

  it('shows error state', async () => {
    useMarketplaceBrowse.mockReturnValue({ isLoading: false, isError: true, data: undefined });
    const { getByText } = await renderScreen(<MarketplaceScreen />);
    expect(getByText('Marketplace is not available right now.')).toBeTruthy();
  });

  it('renders listing cards from browse results', async () => {
    useMarketplaceBrowse.mockReturnValue({
      data: { listings: [listing], page: 0, size: 20, totalElements: 1 },
      isLoading: false,
      isError: false,
    });
    useMarketplaceCategories.mockReturnValue({
      data: [{ code: 'RECOVERY', name: 'Recovery', sortOrder: 2 }],
      isLoading: false,
    });
    const { getByTestId, getByLabelText } = await renderScreen(<MarketplaceScreen />);
    expect(getByTestId('marketplace-listing-card')).toBeTruthy();
    expect(getByLabelText('Search marketplace')).toBeTruthy();
  });
});

describe('MarketplaceListingScreen', () => {
  it('opens the HTTPS destination and can save', async () => {
    const open = jest.spyOn(Linking, 'openURL').mockResolvedValue(undefined);
    const save = jest.fn();
    const recordClick = jest.fn();
    const recordRedeem = jest.fn();
    useMarketplaceListing.mockReturnValue({
      data: listing,
      isLoading: false,
      isError: false,
    });
    useMarketplaceActions.mockReturnValue({
      save,
      unsave: jest.fn(),
      recordClick,
      recordRedeem,
      recordImpression: jest.fn(),
    });
    const { getByText } = await renderScreen(<MarketplaceListingScreen listingId="l1" />);
    fireEvent.press(getByText('Save listing'));
    expect(save).toHaveBeenCalledWith('l1');
    fireEvent.press(getByText('Redeem offer'));
    expect(recordClick).toHaveBeenCalled();
    await waitFor(() => expect(open).toHaveBeenCalledWith('https://acme.example/boots'));
    open.mockRestore();
  });

});
