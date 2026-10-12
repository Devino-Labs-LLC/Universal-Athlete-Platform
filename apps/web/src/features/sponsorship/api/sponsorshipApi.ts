import type { ApiClient } from '@/core/api/apiClient';

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

export interface SponsorRecord {
  id: string;
  name: string;
  displayName: string;
  status: string;
  logoUrl: string | null;
  websiteUrl: string | null;
  description: string | null;
}

export interface CampaignRecord {
  id: string;
  sponsorId: string;
  name: string;
  status: string;
  startsAt: string;
  endsAt: string | null;
  headline: string;
  body: string;
  ctaLabel: string;
  destinationUrl: string;
  creativeUrl: string | null;
  placements: Array<{
    id: string;
    surface: string;
    audience: string;
    organizationId: string | null;
    teamId: string | null;
  }>;
}

export interface SponsorshipReportRow {
  sponsorId: string;
  sponsorName: string;
  campaignId: string;
  campaignName: string;
  surface: string;
  impressions: number;
  clicks: number;
  clickThroughRate: number;
}

export async function fetchPlacement(
  client: ApiClient,
  surface: PlacementSurface,
  teamId?: string,
): Promise<SponsorshipPlacement | null> {
  const response = await client.axios.get<SponsorshipPlacement>('/api/v1/sponsorships/placements', {
    params: { surface, teamId },
    validateStatus: (status) => status === 200 || status === 204,
  });
  if (response.status === 204) {
    return null;
  }
  return response.data;
}

export async function recordPlacementImpression(
  client: ApiClient,
  placementId: string,
  teamId?: string,
): Promise<void> {
  await client.axios.post(`/api/v1/sponsorships/placements/${placementId}/impressions`, null, {
    params: { teamId },
  });
}

export async function recordPlacementClick(
  client: ApiClient,
  placementId: string,
  teamId?: string,
): Promise<void> {
  await client.axios.post(`/api/v1/sponsorships/placements/${placementId}/clicks`, null, {
    params: { teamId },
  });
}

export async function fetchOperatorMe(client: ApiClient): Promise<boolean> {
  const response = await client.axios.get<{ operator: boolean }>(
    '/api/v1/platform/sponsorships/operators/me',
    { validateStatus: (status) => status === 200 || status === 404 },
  );
  return response.status === 200 && response.data.operator === true;
}

export async function listSponsors(client: ApiClient): Promise<SponsorRecord[]> {
  const response = await client.axios.get<SponsorRecord[]>('/api/v1/platform/sponsorships/sponsors');
  return response.data;
}

export async function createSponsor(
  client: ApiClient,
  body: Partial<SponsorRecord> & { name: string; displayName: string },
): Promise<SponsorRecord> {
  const response = await client.axios.post<SponsorRecord>('/api/v1/platform/sponsorships/sponsors', body);
  return response.data;
}

export async function transitionSponsor(
  client: ApiClient,
  sponsorId: string,
  status: string,
): Promise<SponsorRecord> {
  const response = await client.axios.post<SponsorRecord>(
    `/api/v1/platform/sponsorships/sponsors/${sponsorId}/status`,
    { status },
  );
  return response.data;
}

export async function listCampaigns(client: ApiClient): Promise<CampaignRecord[]> {
  const response = await client.axios.get<CampaignRecord[]>('/api/v1/platform/sponsorships/campaigns');
  return response.data;
}

export async function createCampaign(client: ApiClient, body: Record<string, unknown>): Promise<CampaignRecord> {
  const response = await client.axios.post<CampaignRecord>('/api/v1/platform/sponsorships/campaigns', body);
  return response.data;
}

export async function transitionCampaign(
  client: ApiClient,
  campaignId: string,
  status: string,
): Promise<CampaignRecord> {
  const response = await client.axios.post<CampaignRecord>(
    `/api/v1/platform/sponsorships/campaigns/${campaignId}/status`,
    { status },
  );
  return response.data;
}

export async function attachPlacement(
  client: ApiClient,
  campaignId: string,
  body: Record<string, unknown>,
): Promise<void> {
  await client.axios.post(`/api/v1/platform/sponsorships/campaigns/${campaignId}/placements`, body);
}

export async function fetchSponsorshipReports(client: ApiClient): Promise<SponsorshipReportRow[]> {
  const response = await client.axios.get<SponsorshipReportRow[]>('/api/v1/platform/sponsorships/reports');
  return response.data;
}
