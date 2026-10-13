import { render } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { MarketplaceListingScreen } from '@/src/features/marketplace/screens/MarketplaceListingScreen';

jest.mock('@/src/features/marketplace/hooks/useMarketplace', () => ({
  useMarketplaceListing: () => ({ isLoading: false, isError: true, data: undefined }),
  useMarketplaceActions: () => ({
    save: jest.fn(),
    unsave: jest.fn(),
    recordClick: jest.fn(),
    recordRedeem: jest.fn(),
  }),
}));

describe('MarketplaceListingScreen unavailable', () => {
  it('shows unavailable copy when the listing cannot be loaded', async () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { getByText } = await render(
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <MarketplaceListingScreen listingId="missing" />
        </ThemeProvider>
      </QueryClientProvider>,
    );
    expect(getByText('This listing is unavailable.')).toBeTruthy();
  });
});
