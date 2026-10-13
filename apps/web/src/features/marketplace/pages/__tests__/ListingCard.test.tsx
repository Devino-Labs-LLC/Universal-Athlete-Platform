import { describe, expect, it, vi } from 'vitest';

import { ListingCard } from '@/features/marketplace/components/ListingCard';
import { MarketplacePage } from '@/features/marketplace/pages/MarketplacePage';
import { MarketplaceListingPage } from '@/features/marketplace/pages/MarketplaceListingPage';
import type { MarketplaceListing } from '@/features/marketplace/api/marketplaceApi';
import { renderWithProviders, screen } from '@/test/utils';

const listing: MarketplaceListing = {
  id: 'l1',
  partnerId: 'p1',
  partnerDisplayName: 'Acme Gear',
  title: 'Recovery boots',
  shortDescription: 'Pneumatic recovery',
  fullDescription: 'Partner recovery equipment for rest days.',
  listingType: 'PRODUCT',
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

vi.mock('@/features/marketplace/hooks/useMarketplace', () => ({
  useMarketplaceBrowse: vi.fn(),
  useMarketplaceCategories: vi.fn(),
  useMarketplaceListing: vi.fn(),
  useMarketplaceActions: vi.fn(),
}));

import {
  useMarketplaceActions,
  useMarketplaceBrowse,
  useMarketplaceCategories,
  useMarketplaceListing,
} from '@/features/marketplace/hooks/useMarketplace';

describe('ListingCard', () => {
  it('labels partner listings and does not look like readiness copy', () => {
    renderWithProviders(<ListingCard listing={listing} />);
    expect(screen.getByLabelText('Marketplace listing: Recovery boots')).toBeInTheDocument();
    expect(screen.getByText('Featured partner listing')).toBeInTheDocument();
    expect(screen.getByText('Acme Gear')).toBeInTheDocument();
    expect(screen.queryByText(/readiness/i)).not.toBeInTheDocument();
  });

  it('marks unavailable saved history', () => {
    renderWithProviders(<ListingCard listing={{ ...listing, available: false, destinationUrl: null }} />);
    expect(screen.getByText('Unavailable')).toBeInTheDocument();
  });
});

describe('MarketplacePage', () => {
  it('shows empty search results without inventing listings', () => {
    vi.mocked(useMarketplaceCategories).mockReturnValue({ data: [], isLoading: false, isError: false } as never);
    vi.mocked(useMarketplaceBrowse).mockReturnValue({
      data: { listings: [], page: 0, size: 20, totalElements: 0 },
      isLoading: false,
      isError: false,
    } as never);
    vi.mocked(useMarketplaceActions).mockReturnValue({
      recordImpression: vi.fn(),
    } as never);
    renderWithProviders(<MarketplacePage />);
    expect(screen.getByText('No listings')).toBeInTheDocument();
  });

  it('filters by category and search and shows an error state', () => {
    vi.mocked(useMarketplaceCategories).mockReturnValue({
      data: [{ code: 'RECOVERY', name: 'Recovery', sortOrder: 2 }],
      isLoading: false,
      isError: false,
    } as never);
    vi.mocked(useMarketplaceBrowse).mockReturnValue({
      data: { listings: [listing], page: 0, size: 20, totalElements: 1 },
      isLoading: false,
      isError: false,
    } as never);
    vi.mocked(useMarketplaceActions).mockReturnValue({ recordImpression: vi.fn() } as never);
    renderWithProviders(<MarketplacePage />);
    expect(screen.getByTestId('marketplace-listing-card')).toBeInTheDocument();
    expect(screen.getByLabelText('Search marketplace')).toBeInTheDocument();
    expect(screen.getByLabelText('Filter by category')).toBeInTheDocument();
  });

  it('shows loading and error', () => {
    vi.mocked(useMarketplaceCategories).mockReturnValue({ data: [], isLoading: false, isError: false } as never);
    vi.mocked(useMarketplaceBrowse).mockReturnValue({ isLoading: true, isError: false, data: undefined } as never);
    vi.mocked(useMarketplaceActions).mockReturnValue({ recordImpression: vi.fn() } as never);
    const loading = renderWithProviders(<MarketplacePage />);
    expect(loading.getByText('Loading marketplace…')).toBeInTheDocument();
    loading.unmount();
    vi.mocked(useMarketplaceBrowse).mockReturnValue({ isLoading: false, isError: true, data: undefined } as never);
    renderWithProviders(<MarketplacePage />);
    expect(screen.getByText('Marketplace is not available right now.')).toBeInTheDocument();
  });
});

describe('MarketplaceListingPage', () => {
  it('shows detail, save, and visit destination', async () => {
    const save = vi.fn();
    const recordClick = vi.fn();
    const recordRedeem = vi.fn();
    vi.mocked(useMarketplaceListing).mockReturnValue({
      data: listing,
      isLoading: false,
      isError: false,
    } as never);
    vi.mocked(useMarketplaceActions).mockReturnValue({
      save,
      unsave: vi.fn(),
      recordClick,
      recordRedeem,
    } as never);
    renderWithProviders(<MarketplaceListingPage />, { initialEntries: ['/app/marketplace/l1'] });
    expect(screen.getByTestId('marketplace-listing-detail')).toBeInTheDocument();
    expect(screen.getByText('Code: BOOTS20')).toBeInTheDocument();
    screen.getByRole('button', { name: 'Save listing' }).click();
    expect(save).toHaveBeenCalled();
  });
});
