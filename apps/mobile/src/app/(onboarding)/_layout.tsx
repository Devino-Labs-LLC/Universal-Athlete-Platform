import { Redirect, Stack, usePathname } from 'expo-router';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { onboardingRouteForState } from '@/src/features/onboarding/onboardingRoutes';
import { LoadingView } from '@/src/core/components/LoadingView';

/**
 * Onboarding routes are also reused for Profile-tab edits after completion.
 * Incomplete athletes are constrained to the current/earlier steps.
 * Completed athletes may open any step for edits without being kicked to tabs.
 */
export default function OnboardingLayout() {
  const pathname = usePathname();
  const { status: authStatus } = useAuthSession();
  const { state: onboardingState } = useAthleteOnboarding();

  if (authStatus === 'INITIALIZING' || authStatus === 'REFRESHING' || onboardingState === 'LOADING') {
    return <LoadingView message="Loading setup…" />;
  }

  if (authStatus === 'UNAUTHENTICATED' || authStatus === 'EXPIRED') {
    return <Redirect href="/(auth)/login" />;
  }

  if (onboardingState === 'ERROR') {
    return <Redirect href="/bootstrap" />;
  }

  if (
    onboardingState === 'PROFILE_REQUIRED' ||
    onboardingState === 'SPORTS_REQUIRED' ||
    onboardingState === 'GOALS_REQUIRED'
  ) {
    const requiredRoute = onboardingRouteForState(onboardingState);
    if (requiredRoute) {
      const allowedPrefixes = allowedOnboardingPaths(onboardingState);
      const onAllowedPath = allowedPrefixes.some(
        (prefix) => pathname === prefix || pathname.endsWith(prefix),
      );
      if (!onAllowedPath) {
        return <Redirect href={requiredRoute} />;
      }
    }
  }

  return (
    <Stack screenOptions={{ headerShown: true, title: 'Setup' }}>
      <Stack.Screen name="profile" options={{ title: 'Athlete profile' }} />
      <Stack.Screen name="sports" options={{ title: 'Sports' }} />
      <Stack.Screen name="goals" options={{ title: 'Goals' }} />
    </Stack>
  );
}

function allowedOnboardingPaths(
  state: 'PROFILE_REQUIRED' | 'SPORTS_REQUIRED' | 'GOALS_REQUIRED',
): string[] {
  switch (state) {
    case 'PROFILE_REQUIRED':
      return ['/profile', '/(onboarding)/profile'];
    case 'SPORTS_REQUIRED':
      return ['/profile', '/sports', '/(onboarding)/profile', '/(onboarding)/sports'];
    case 'GOALS_REQUIRED':
      return [
        '/profile',
        '/sports',
        '/goals',
        '/(onboarding)/profile',
        '/(onboarding)/sports',
        '/(onboarding)/goals',
      ];
  }
}
