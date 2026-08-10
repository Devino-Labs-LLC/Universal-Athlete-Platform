import { Redirect, Tabs } from 'expo-router';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { LoadingView } from '@/src/core/components/LoadingView';

export default function TabsLayout() {
  const theme = useAppTheme();
  const { status: authStatus } = useAuthSession();
  const { state: onboardingState } = useAthleteOnboarding();
  const { status: bootstrapStatus } = useBootstrap();

  if (
    authStatus === 'INITIALIZING' ||
    authStatus === 'REFRESHING' ||
    onboardingState === 'LOADING' ||
    bootstrapStatus === 'BOOTSTRAPPING'
  ) {
    return <LoadingView message="Loading app…" />;
  }

  if (
    onboardingState === 'PROFILE_REQUIRED' ||
    onboardingState === 'SPORTS_REQUIRED' ||
    onboardingState === 'GOALS_REQUIRED' ||
    onboardingState === 'ERROR'
  ) {
    return <Redirect href="/bootstrap" />;
  }

  if (bootstrapStatus !== 'AUTHENTICATED_READY') {
    return <Redirect href="/bootstrap" />;
  }

  return (
    <Tabs
      screenOptions={{
        headerShown: true,
        tabBarActiveTintColor: theme.colors.primary,
        tabBarInactiveTintColor: theme.colors.textMuted,
        tabBarStyle: {
          backgroundColor: theme.colors.surface,
          borderTopColor: theme.colors.border,
        },
        headerStyle: {
          backgroundColor: theme.colors.surface,
        },
        headerTintColor: theme.colors.text,
      }}>
      <Tabs.Screen name="index" options={{ title: 'Home' }} />
      <Tabs.Screen name="training" options={{ title: 'Training' }} />
      <Tabs.Screen name="recovery" options={{ title: 'Recovery' }} />
      <Tabs.Screen name="performance" options={{ title: 'Performance' }} />
      <Tabs.Screen name="profile" options={{ title: 'Profile' }} />
    </Tabs>
  );
}
