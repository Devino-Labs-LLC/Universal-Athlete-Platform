import { type PropsWithChildren, useMemo } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { AuthSessionProvider } from '@/app/providers/AuthSessionProvider';
import { BootstrapProvider } from '@/app/providers/BootstrapProvider';
import { ThemeProvider } from '@/app/providers/ThemeProvider';

function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: 1,
        refetchOnWindowFocus: false,
      },
    },
  });
}

export function AppProviders({ children }: PropsWithChildren) {
  const queryClient = useMemo(() => createQueryClient(), []);

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthSessionProvider queryClient={queryClient}>
          <BootstrapProvider>{children}</BootstrapProvider>
        </AuthSessionProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}
