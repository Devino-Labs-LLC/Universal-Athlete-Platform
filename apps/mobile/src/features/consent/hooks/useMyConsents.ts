import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { listMyConsents } from '@/src/features/consent/api/consentsApi';
import { consentKeys } from '@/src/features/consent/models/queryKeys';

export function useMyConsents() {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: consentKeys.mine(),
    queryFn: () => listMyConsents(apiClient),
    enabled: status === 'AUTHENTICATED',
    staleTime: 30_000,
    retry: 1,
  });
}
