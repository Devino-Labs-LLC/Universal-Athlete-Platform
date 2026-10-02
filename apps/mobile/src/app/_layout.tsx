import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import 'react-native-reanimated';

import { AppProviders } from '@/src/app/providers/AppProviders';
import { installSecureCrypto } from '@/src/core/crypto/installSecureCrypto';

export { ErrorBoundary } from 'expo-router';

// Ensure Connected Apps (and other callers) can use Web-Crypto-compatible
// randomUUID / getRandomValues on Hermes before any feature code runs.
try {
  installSecureCrypto();
} catch {
  // Fail closed at call sites (e.g. newRequestId). Do not crash unrelated boot.
}

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  useEffect(() => {
    // System fonts only — SpaceMono was Expo boilerplate and unused in UI.
    void SplashScreen.hideAsync();
  }, []);

  return (
    <AppProviders>
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Screen name="index" />
        <Stack.Screen name="bootstrap" />
        <Stack.Screen name="incompatible" />
        <Stack.Screen name="(auth)" />
        <Stack.Screen name="(onboarding)" />
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="invitations" />
        <Stack.Screen name="sharing" />
      </Stack>
    </AppProviders>
  );
}
