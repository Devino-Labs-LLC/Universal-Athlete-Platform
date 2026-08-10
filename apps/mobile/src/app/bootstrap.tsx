import { Redirect } from 'expo-router';
import { useEffect } from 'react';

import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';

export default function BootstrapScreen() {
  const { status, errorMessage, retry } = useBootstrap();

  useEffect(() => {
    // BootstrapProvider reacts to auth changes; this screen only routes.
  }, []);

  if (status === 'BOOTSTRAPPING') {
    return <LoadingView message="Starting Universal Athlete…" />;
  }

  if (status === 'BOOTSTRAP_ERROR') {
    return (
      <ErrorView
        message={errorMessage ?? 'Unable to load client bootstrap.'}
        onRetry={() => {
          void retry();
        }}
      />
    );
  }

  if (status === 'UNAUTHENTICATED') {
    return <Redirect href="/(auth)/login" />;
  }

  if (status === 'INCOMPATIBLE_CLIENT') {
    return <Redirect href="/incompatible" />;
  }

  if (status === 'AUTHENTICATED_READY') {
    return <Redirect href="/(tabs)" />;
  }

  return <LoadingView />;
}
