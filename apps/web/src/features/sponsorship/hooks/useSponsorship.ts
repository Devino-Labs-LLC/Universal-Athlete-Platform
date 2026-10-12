import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  attachPlacement,
  createCampaign,
  createSponsor,
  fetchOperatorMe,
  fetchPlacement,
  fetchSponsorshipReports,
  listCampaigns,
  listSponsors,
  recordPlacementClick,
  recordPlacementImpression,
  transitionCampaign,
  transitionSponsor,
  type PlacementSurface,
} from '@/features/sponsorship/api/sponsorshipApi';

export const sponsorshipKeys = {
  placement: (surface: PlacementSurface, teamId?: string) =>
    ['sponsorship', 'placement', surface, teamId ?? ''] as const,
  operator: ['sponsorship', 'operator'] as const,
  sponsors: ['sponsorship', 'sponsors'] as const,
  campaigns: ['sponsorship', 'campaigns'] as const,
  reports: ['sponsorship', 'reports'] as const,
};

export function usePlacement(surface: PlacementSurface, teamId?: string) {
  const { apiClient } = useAuthSession();
  const query = useQuery({
    queryKey: sponsorshipKeys.placement(surface, teamId),
    queryFn: () => fetchPlacement(apiClient, surface, teamId),
  });
  useEffect(() => {
    if (query.data?.placementId) {
      void recordPlacementImpression(apiClient, query.data.placementId, teamId);
    }
  }, [apiClient, query.data?.placementId, teamId]);
  const click = useMutation({
    mutationFn: async () => {
      if (!query.data) {
        return;
      }
      await recordPlacementClick(apiClient, query.data.placementId, teamId);
    },
  });
  return { ...query, recordClick: click.mutate };
}

export function useSponsorshipOperator() {
  const { apiClient } = useAuthSession();
  return useQuery({
    queryKey: sponsorshipKeys.operator,
    queryFn: () => fetchOperatorMe(apiClient),
  });
}

export function useSponsorAdmin() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  const sponsors = useQuery({
    queryKey: sponsorshipKeys.sponsors,
    queryFn: () => listSponsors(apiClient),
  });
  const campaigns = useQuery({
    queryKey: sponsorshipKeys.campaigns,
    queryFn: () => listCampaigns(apiClient),
  });
  const reports = useQuery({
    queryKey: sponsorshipKeys.reports,
    queryFn: () => fetchSponsorshipReports(apiClient),
  });
  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['sponsorship'] });
  };
  return {
    sponsors,
    campaigns,
    reports,
    createSponsor: async (body: { name: string; displayName: string; websiteUrl?: string }) => {
      const created = await createSponsor(apiClient, body);
      await invalidate();
      return created;
    },
    transitionSponsor: async (id: string, status: string) => {
      await transitionSponsor(apiClient, id, status);
      await invalidate();
    },
    createCampaign: async (body: Record<string, unknown>) => {
      const created = await createCampaign(apiClient, body);
      await invalidate();
      return created;
    },
    transitionCampaign: async (id: string, status: string) => {
      await transitionCampaign(apiClient, id, status);
      await invalidate();
    },
    attachPlacement: async (id: string, body: Record<string, unknown>) => {
      await attachPlacement(apiClient, id, body);
      await invalidate();
    },
  };
}
