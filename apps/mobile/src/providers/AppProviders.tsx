import { QueryClientProvider } from '@tanstack/react-query';
import { PropsWithChildren, useMemo } from 'react';

import { createQueryClient } from '@/src/app/config/queryClient';
import { AuthSessionProvider } from '@/src/app/providers/AuthSessionProvider';
import { BootstrapProvider } from '@/src/app/providers/BootstrapProvider';
import { ThemeProvider } from '@/src/app/theme/ThemeProvider';

export function AppProviders({ children }: PropsWithChildren) {
  const queryClient = useMemo(() => createQueryClient(), []);

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthSessionProvider>
          <BootstrapProvider>{children}</BootstrapProvider>
        </AuthSessionProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}
