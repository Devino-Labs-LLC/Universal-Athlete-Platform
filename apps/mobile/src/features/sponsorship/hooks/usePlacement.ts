import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import {
  fetchPlacement,
  recordPlacementClick,
  recordPlacementImpression,
  type PlacementSurface,
} from '@/src/features/sponsorship/api/sponsorshipApi';

export function usePlacement(surface: PlacementSurface) {
  const { apiClient, status } = useAuthSession();
  const query = useQuery({
    queryKey: ['sponsorship', 'placement', surface],
    queryFn: () => fetchPlacement(apiClient, surface),
    enabled: status === 'AUTHENTICATED',
  });
  useEffect(() => {
    if (query.data?.placementId) {
      void recordPlacementImpression(apiClient, query.data.placementId);
    }
  }, [apiClient, query.data?.placementId]);
  const click = useMutation({
    mutationFn: async () => {
      if (!query.data) {
        return;
      }
      await recordPlacementClick(apiClient, query.data.placementId);
    },
  });
  return { ...query, recordClick: click.mutate };
}
