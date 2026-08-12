import { Redirect, Stack } from 'expo-router';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { LoadingView } from '@/src/core/components/LoadingView';

function AuthStack() {
  const theme = useAppTheme();
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
      <Stack.Screen name="login" options={{ title: 'Sign in' }} />
      <Stack.Screen name="register" options={{ title: 'Create account' }} />
      <Stack.Screen name="verify-email" options={{ title: 'Verify email' }} />
    </Stack>
  );
}

/**
 * Public auth routes (login/register/verify).
 * Unauthenticated must never wait on onboarding/bootstrap loading gates.
 */
export default function AuthLayout() {
  const { status: authStatus } = useAuthSession();
  const { state: onboardingState } = useAthleteOnboarding();
  const { status: bootstrapStatus } = useBootstrap();

  // Expected clean-install / logout: settle on auth screens immediately.
  if (authStatus === 'UNAUTHENTICATED' || authStatus === 'EXPIRED') {
    return <AuthStack />;
  }

  if (authStatus === 'INITIALIZING' || authStatus === 'REFRESHING') {
    return <LoadingView message="Checking session…" />;
  }

  // Authenticated: wait for onboarding/bootstrap before leaving auth.
  if (onboardingState === 'LOADING' || bootstrapStatus === 'BOOTSTRAPPING') {
    return <LoadingView message="Checking session…" />;
  }

  if (bootstrapStatus === 'AUTHENTICATED_READY') {
    return <Redirect href="/(tabs)" />;
  }

  // Authenticated but not shell-ready (onboarding / bootstrap) — leave auth screens.
  if (authStatus === 'AUTHENTICATED') {
    return <Redirect href="/bootstrap" />;
  }

  return <AuthStack />;
}
