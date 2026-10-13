import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect } from 'react';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import {
  fetchMarketplaceCategories,
  fetchMarketplaceListing,
  fetchMarketplaceListings,
  recordMarketplaceClick,
  recordMarketplaceDetailView,
  recordMarketplaceImpression,
  recordMarketplaceRedemption,
  saveMarketplaceListing,
  unsaveMarketplaceListing,
} from '@/src/features/marketplace/api/marketplaceApi';

export function useMarketplaceBrowse(filters: { category?: string; q?: string }) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: ['marketplace', 'listings', filters],
    queryFn: () => fetchMarketplaceListings(apiClient, filters),
    enabled: status === 'AUTHENTICATED',
  });
}

export function useMarketplaceCategories() {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: ['marketplace', 'categories'],
    queryFn: () => fetchMarketplaceCategories(apiClient),
    enabled: status === 'AUTHENTICATED',
  });
}

export function useMarketplaceListing(listingId: string) {
  const { apiClient, status } = useAuthSession();
  const query = useQuery({
    queryKey: ['marketplace', 'listing', listingId],
    queryFn: () => fetchMarketplaceListing(apiClient, listingId),
    enabled: status === 'AUTHENTICATED' && Boolean(listingId),
  });
  useEffect(() => {
    if (query.data?.id) {
      void recordMarketplaceDetailView(apiClient, query.data.id);
    }
  }, [apiClient, query.data?.id]);
  return query;
}

export function useMarketplaceActions() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['marketplace'] });
  };
  const save = useMutation({
    mutationFn: (listingId: string) => saveMarketplaceListing(apiClient, listingId),
    onSuccess: invalidate,
  });
  const unsave = useMutation({
    mutationFn: (listingId: string) => unsaveMarketplaceListing(apiClient, listingId),
    onSuccess: invalidate,
  });
  const recordImpression = useCallback(
    (listingId: string) => recordMarketplaceImpression(apiClient, listingId),
    [apiClient],
  );
  const recordClick = useCallback(
    (listingId: string) => recordMarketplaceClick(apiClient, listingId),
    [apiClient],
  );
  const recordRedeem = useCallback(
    (listingId: string) => recordMarketplaceRedemption(apiClient, listingId),
    [apiClient],
  );
  return {
    save: save.mutateAsync,
    unsave: unsave.mutateAsync,
    recordImpression,
    recordClick,
    recordRedeem,
  };
}
