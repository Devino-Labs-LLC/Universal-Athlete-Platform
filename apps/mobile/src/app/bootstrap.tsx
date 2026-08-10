import { Redirect } from 'expo-router';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { onboardingRouteForState } from '@/src/features/onboarding/onboardingRoutes';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';

export default function BootstrapScreen() {
  const { status: authStatus } = useAuthSession();
  const { state: onboardingState, errorMessage: onboardingError, refresh } = useAthleteOnboarding();
  const { status: bootstrapStatus, errorMessage: bootstrapError, retry } = useBootstrap();

  if (authStatus === 'INITIALIZING' || authStatus === 'REFRESHING') {
    return <LoadingView message="Restoring session…" />;
  }

  if (authStatus === 'UNAUTHENTICATED' || authStatus === 'EXPIRED') {
    return <Redirect href="/(auth)/login" />;
  }

  if (onboardingState === 'LOADING') {
    return <LoadingView message="Loading athlete profile…" />;
  }

  if (onboardingState === 'ERROR') {
    return (
      <ErrorView
        message={onboardingError ?? 'Unable to load onboarding data.'}
        onRetry={() => {
          void refresh();
        }}
      />
    );
  }

  const onboardingRoute = onboardingRouteForState(onboardingState);
  if (onboardingRoute) {
    return <Redirect href={onboardingRoute} />;
  }

  if (bootstrapStatus === 'BOOTSTRAPPING' || bootstrapStatus === 'IDLE') {
    return <LoadingView message="Starting Universal Athlete…" />;
  }

  if (bootstrapStatus === 'BOOTSTRAP_ERROR') {
    return (
      <ErrorView
        message={bootstrapError ?? 'Unable to load client bootstrap.'}
        onRetry={() => {
          void retry();
        }}
      />
    );
  }

  if (bootstrapStatus === 'INCOMPATIBLE_CLIENT') {
    return <Redirect href="/incompatible" />;
  }

  if (bootstrapStatus === 'AUTHENTICATED_READY') {
    return <Redirect href="/(tabs)" />;
  }

  return <LoadingView />;
}
