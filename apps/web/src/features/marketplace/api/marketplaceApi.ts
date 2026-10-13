import type { ApiClient } from '@/core/api/apiClient';

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

export interface OperatorMarketplaceListing {
  id: string;
  partnerId: string;
  title: string;
  shortDescription: string;
  fullDescription: string;
  listingType: MarketplaceListingType;
  categoryCode: string;
  status: string;
  featured: boolean;
  audience: string;
  destinationUrl: string;
  priceDisplay: string | null;
  discountText: string | null;
  promoCode: string | null;
  startsAt: string;
  endsAt: string | null;
}

export interface MarketplaceReportRow {
  listingId: string;
  title: string;
  partnerId: string;
  partnerName: string;
  categoryCode: string;
  listingType: MarketplaceListingType;
  impressions: number;
  detailViews: number;
  outboundClicks: number;
  redemptionInteractions: number;
  saves: number;
  clickThroughRate: number;
}

export async function fetchMarketplaceCategories(client: ApiClient): Promise<MarketplaceCategory[]> {
  const response = await client.axios.get<MarketplaceCategory[]>('/api/v1/marketplace/categories');
  return response.data;
}

export async function fetchMarketplaceListings(
  client: ApiClient,
  params: { category?: string; type?: string; q?: string; featured?: boolean; page?: number; size?: number },
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

export async function fetchMarketplaceSaves(client: ApiClient): Promise<MarketplaceListing[]> {
  const response = await client.axios.get<MarketplaceListing[]>('/api/v1/marketplace/saves');
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

export async function listOperatorMarketplaceListings(
  client: ApiClient,
): Promise<OperatorMarketplaceListing[]> {
  const response = await client.axios.get<OperatorMarketplaceListing[]>('/api/v1/platform/marketplace/listings');
  return response.data;
}

export async function createOperatorMarketplaceListing(
  client: ApiClient,
  body: Record<string, unknown>,
): Promise<OperatorMarketplaceListing> {
  const response = await client.axios.post<OperatorMarketplaceListing>(
    '/api/v1/platform/marketplace/listings',
    body,
  );
  return response.data;
}

export async function transitionMarketplaceListing(
  client: ApiClient,
  listingId: string,
  status: string,
): Promise<OperatorMarketplaceListing> {
  const response = await client.axios.post<OperatorMarketplaceListing>(
    `/api/v1/platform/marketplace/listings/${listingId}/status`,
    { status },
  );
  return response.data;
}

export async function fetchMarketplaceReports(client: ApiClient): Promise<MarketplaceReportRow[]> {
  const response = await client.axios.get<MarketplaceReportRow[]>('/api/v1/platform/marketplace/reports');
  return response.data;
}
