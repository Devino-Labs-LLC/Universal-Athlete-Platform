import { Redirect, Stack } from 'expo-router';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { LoadingView } from '@/src/core/components/LoadingView';

export default function AuthLayout() {
  const { status: authStatus } = useAuthSession();
  const { state: onboardingState } = useAthleteOnboarding();
  const { status: bootstrapStatus } = useBootstrap();

  if (
    authStatus === 'INITIALIZING' ||
    authStatus === 'REFRESHING' ||
    onboardingState === 'LOADING' ||
    bootstrapStatus === 'BOOTSTRAPPING'
  ) {
    return <LoadingView message="Checking session…" />;
  }

  if (bootstrapStatus === 'AUTHENTICATED_READY') {
    return <Redirect href="/(tabs)" />;
  }

  // Authenticated but not shell-ready (onboarding / bootstrap) — leave auth screens.
  if (authStatus === 'AUTHENTICATED') {
    return <Redirect href="/bootstrap" />;
  }

  return (
    <Stack screenOptions={{ headerShown: true }}>
      <Stack.Screen name="login" options={{ title: 'Sign in' }} />
      <Stack.Screen name="register" options={{ title: 'Create account' }} />
      <Stack.Screen name="verify-email" options={{ title: 'Verify email' }} />
    </Stack>
  );
}
