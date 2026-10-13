import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect } from 'react';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  createOperatorMarketplaceListing,
  fetchMarketplaceCategories,
  fetchMarketplaceListing,
  fetchMarketplaceListings,
  fetchMarketplaceReports,
  fetchMarketplaceSaves,
  listOperatorMarketplaceListings,
  recordMarketplaceClick,
  recordMarketplaceDetailView,
  recordMarketplaceImpression,
  recordMarketplaceRedemption,
  saveMarketplaceListing,
  transitionMarketplaceListing,
  unsaveMarketplaceListing,
} from '@/features/marketplace/api/marketplaceApi';

export const marketplaceKeys = {
  categories: ['marketplace', 'categories'] as const,
  listings: (filters: Record<string, string | undefined>) =>
    ['marketplace', 'listings', filters] as const,
  listing: (id: string) => ['marketplace', 'listing', id] as const,
  saves: ['marketplace', 'saves'] as const,
  operatorListings: ['marketplace', 'operator', 'listings'] as const,
  reports: ['marketplace', 'operator', 'reports'] as const,
};

export function useMarketplaceBrowse(filters: {
  category?: string;
  type?: string;
  q?: string;
}) {
  const { apiClient } = useAuthSession();
  return useQuery({
    queryKey: marketplaceKeys.listings(filters),
    queryFn: () => fetchMarketplaceListings(apiClient, filters),
  });
}

export function useMarketplaceCategories() {
  const { apiClient } = useAuthSession();
  return useQuery({
    queryKey: marketplaceKeys.categories,
    queryFn: () => fetchMarketplaceCategories(apiClient),
  });
}

export function useMarketplaceListing(listingId: string | undefined) {
  const { apiClient } = useAuthSession();
  const query = useQuery({
    queryKey: marketplaceKeys.listing(listingId ?? ''),
    queryFn: () => fetchMarketplaceListing(apiClient, listingId!),
    enabled: Boolean(listingId),
  });
  useEffect(() => {
    if (query.data?.id) {
      void recordMarketplaceDetailView(apiClient, query.data.id);
    }
  }, [apiClient, query.data?.id]);
  return query;
}

export function useMarketplaceSaves() {
  const { apiClient } = useAuthSession();
  return useQuery({
    queryKey: marketplaceKeys.saves,
    queryFn: () => fetchMarketplaceSaves(apiClient),
  });
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

export function useMarketplaceAdmin() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();
  const listings = useQuery({
    queryKey: marketplaceKeys.operatorListings,
    queryFn: () => listOperatorMarketplaceListings(apiClient),
  });
  const reports = useQuery({
    queryKey: marketplaceKeys.reports,
    queryFn: () => fetchMarketplaceReports(apiClient),
  });
  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['marketplace'] });
  };
  return {
    listings,
    reports,
    createListing: async (body: Record<string, unknown>) => {
      await createOperatorMarketplaceListing(apiClient, body);
      await invalidate();
    },
    transition: async (listingId: string, status: string) => {
      await transitionMarketplaceListing(apiClient, listingId, status);
      await invalidate();
    },
  };
}
