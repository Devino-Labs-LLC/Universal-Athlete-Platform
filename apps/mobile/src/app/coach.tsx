import { Redirect } from 'expo-router';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { LoadingView } from '@/src/core/components/LoadingView';
import { CoachScreen } from '@/src/features/coach/screens/CoachScreen';

export default function CoachRoute() {
  const { status: authStatus } = useAuthSession();
  const { state: onboardingState } = useAthleteOnboarding();
  const { status: bootstrapStatus } = useBootstrap();

  if (authStatus === 'UNAUTHENTICATED' || authStatus === 'EXPIRED') {
    return <Redirect href="/(auth)/login" />;
  }

  if (
    authStatus === 'INITIALIZING' ||
    authStatus === 'REFRESHING' ||
    onboardingState === 'LOADING' ||
    bootstrapStatus === 'BOOTSTRAPPING'
  ) {
    return <LoadingView message="Loading…" />;
  }

  if (
    onboardingState === 'PROFILE_REQUIRED' ||
    onboardingState === 'SPORTS_REQUIRED' ||
    onboardingState === 'GOALS_REQUIRED' ||
    onboardingState === 'ERROR' ||
    bootstrapStatus !== 'AUTHENTICATED_READY'
  ) {
    return <Redirect href="/bootstrap" />;
  }

  return <CoachScreen />;
}
