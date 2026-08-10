import { Redirect, Stack } from 'expo-router';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { LoadingView } from '@/src/core/components/LoadingView';

export default function AuthLayout() {
  const { status: authStatus } = useAuthSession();
  const { status: bootstrapStatus } = useBootstrap();

  if (
    authStatus === 'INITIALIZING' ||
    authStatus === 'REFRESHING' ||
    bootstrapStatus === 'BOOTSTRAPPING'
  ) {
    return <LoadingView message="Checking session…" />;
  }

  if (bootstrapStatus === 'AUTHENTICATED_READY') {
    return <Redirect href="/(tabs)" />;
  }

  return (
    <Stack screenOptions={{ headerShown: true }}>
      <Stack.Screen name="login" options={{ title: 'Sign in' }} />
      <Stack.Screen name="register" options={{ title: 'Create account' }} />
      <Stack.Screen name="verify-email" options={{ title: 'Verify email' }} />
    </Stack>
  );
}
