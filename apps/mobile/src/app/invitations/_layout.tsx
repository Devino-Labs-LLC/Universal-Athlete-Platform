import { Redirect, Stack } from 'expo-router';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { LoadingView } from '@/src/core/components/LoadingView';

/**
 * Authenticated stack for athlete invitation list + deep-link token screens.
 * Not a tab — reachable from Profile and uap://invitations/<token>.
 */
export default function InvitationsLayout() {
  const theme = useAppTheme();
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

  return (
    <Stack
      screenOptions={{
        headerShown: true,
        headerStyle: { backgroundColor: theme.colors.background },
        headerTintColor: theme.colors.accentCyan,
        headerTitleStyle: { color: theme.colors.text, fontWeight: '600' },
        headerShadowVisible: false,
        contentStyle: { backgroundColor: theme.colors.background },
      }}>
      <Stack.Screen name="index" options={{ title: 'Invitations' }} />
      <Stack.Screen name="[token]" options={{ title: 'Invitation' }} />
    </Stack>
  );
}
