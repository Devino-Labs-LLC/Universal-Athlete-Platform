import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { fetchMyConsentGrants } from '@/features/consent/api/consentsApi';
import { consentKeys } from '@/features/consent/models/queryKeys';

export function useMyConsentGrants() {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: consentKeys.myGrants(),
    queryFn: () => fetchMyConsentGrants(apiClient),
    enabled: status === 'AUTHENTICATED',
  });
}
