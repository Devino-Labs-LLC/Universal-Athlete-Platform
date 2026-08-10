import { QueryClientProvider, useQueryClient } from '@tanstack/react-query';
import { PropsWithChildren, useMemo } from 'react';

import { createQueryClient } from '@/src/app/config/queryClient';
import { AthleteOnboardingProvider } from '@/src/app/providers/AthleteOnboardingProvider';
import { AuthSessionProvider } from '@/src/app/providers/AuthSessionProvider';
import { BootstrapProvider } from '@/src/app/providers/BootstrapProvider';
import { ThemeProvider } from '@/src/app/theme/ThemeProvider';

export function AppProviders({ children }: PropsWithChildren) {
  const queryClient = useMemo(() => createQueryClient(), []);

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthSessionProvider queryClient={queryClient}>
          <AthleteOnboardingProvider>
            <BootstrapProvider>{children}</BootstrapProvider>
          </AthleteOnboardingProvider>
        </AuthSessionProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}

export function useAppQueryClient() {
  return useQueryClient();
}
