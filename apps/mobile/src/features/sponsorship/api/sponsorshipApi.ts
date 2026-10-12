import { ApiClient } from '@/src/core/api/apiClient';

export type PlacementSurface = 'HOME' | 'TRAINING' | 'TEAM';

export interface SponsorshipPlacement {
  placementId: string;
  campaignId: string;
  sponsorId: string;
  sponsorDisplayName: string;
  label: string;
  headline: string;
  body: string;
  ctaLabel: string;
  destinationUrl: string;
  creativeUrl: string | null;
  surface: PlacementSurface;
}

export async function fetchPlacement(
  client: ApiClient,
  surface: PlacementSurface,
): Promise<SponsorshipPlacement | null> {
  const response = await client.axios.get<SponsorshipPlacement>('/api/v1/sponsorships/placements', {
    params: { surface },
    validateStatus: (status) => status === 200 || status === 204,
  });
  if (response.status === 204) {
    return null;
  }
  return response.data;
}

export async function recordPlacementImpression(client: ApiClient, placementId: string): Promise<void> {
  await client.axios.post(`/api/v1/sponsorships/placements/${placementId}/impressions`);
}

export async function recordPlacementClick(client: ApiClient, placementId: string): Promise<void> {
  await client.axios.post(`/api/v1/sponsorships/placements/${placementId}/clicks`);
}
