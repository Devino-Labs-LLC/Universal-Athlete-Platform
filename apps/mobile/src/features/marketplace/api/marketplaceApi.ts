import { ApiClient } from '@/src/core/api/apiClient';

export type MarketplaceListingType = 'PRODUCT' | 'SERVICE' | 'PROGRAM' | 'EVENT' | 'OFFER';

export interface MarketplaceCategory {
  code: string;
  name: string;
  sortOrder: number;
}

export interface MarketplaceListing {
  id: string;
  partnerId: string;
  partnerDisplayName: string;
  title: string;
  shortDescription: string;
  fullDescription: string;
  listingType: MarketplaceListingType;
  categoryCode: string;
  categoryName: string;
  featured: boolean;
  available: boolean;
  offerActive: boolean;
  imageUrl: string | null;
  destinationUrl: string | null;
  priceDisplay: string | null;
  discountText: string | null;
  promoCode: string | null;
  saved: boolean;
}

export interface MarketplaceListingPage {
  listings: MarketplaceListing[];
  page: number;
  size: number;
  totalElements: number;
}

export async function fetchMarketplaceCategories(client: ApiClient): Promise<MarketplaceCategory[]> {
  const response = await client.axios.get<MarketplaceCategory[]>('/api/v1/marketplace/categories');
  return response.data;
}

export async function fetchMarketplaceListings(
  client: ApiClient,
  params: { category?: string; type?: string; q?: string },
): Promise<MarketplaceListingPage> {
  const response = await client.axios.get<MarketplaceListingPage>('/api/v1/marketplace/listings', { params });
  return response.data;
}

export async function fetchMarketplaceListing(
  client: ApiClient,
  listingId: string,
): Promise<MarketplaceListing> {
  const response = await client.axios.get<MarketplaceListing>(`/api/v1/marketplace/listings/${listingId}`);
  return response.data;
}

export async function saveMarketplaceListing(client: ApiClient, listingId: string): Promise<void> {
  await client.axios.put(`/api/v1/marketplace/listings/${listingId}/save`);
}

export async function unsaveMarketplaceListing(client: ApiClient, listingId: string): Promise<void> {
  await client.axios.delete(`/api/v1/marketplace/listings/${listingId}/save`);
}

export async function recordMarketplaceImpression(client: ApiClient, listingId: string): Promise<void> {
  await client.axios.post(`/api/v1/marketplace/listings/${listingId}/impressions`);
}

export async function recordMarketplaceDetailView(client: ApiClient, listingId: string): Promise<void> {
  await client.axios.post(`/api/v1/marketplace/listings/${listingId}/detail-views`);
}

export async function recordMarketplaceClick(client: ApiClient, listingId: string): Promise<void> {
  await client.axios.post(`/api/v1/marketplace/listings/${listingId}/clicks`);
}

export async function recordMarketplaceRedemption(client: ApiClient, listingId: string): Promise<void> {
  await client.axios.post(`/api/v1/marketplace/listings/${listingId}/redemptions`);
}
