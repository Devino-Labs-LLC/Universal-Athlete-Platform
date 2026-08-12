import { Redirect, Tabs } from 'expo-router';
import { ColorValue, Platform } from 'react-native';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAthleteOnboarding } from '@/src/app/providers/AthleteOnboardingProvider';
import { useBootstrap } from '@/src/app/providers/BootstrapProvider';
import { TabBarIcon, TabRouteName } from '@/src/core/navigation/tabBarIcons';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { LoadingView } from '@/src/core/components/LoadingView';

function createTabBarIcon(route: TabRouteName) {
  function Icon({
    color,
    focused,
  }: {
    color: ColorValue;
    focused: boolean;
    size: number;
  }) {
    return <TabBarIcon route={route} focused={focused} color={String(color)} />;
  }
  Icon.displayName = `TabBarIcon(${route})`;
  return Icon;
}

export default function TabsLayout() {
  const theme = useAppTheme();
  const { status: authStatus } = useAuthSession();
  const { state: onboardingState } = useAthleteOnboarding();
  const { status: bootstrapStatus } = useBootstrap();

  if (
    authStatus === 'UNAUTHENTICATED' ||
    authStatus === 'EXPIRED'
  ) {
    return <Redirect href="/(auth)/login" />;
  }

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
        tabBarActiveTintColor: theme.colors.accentCyan,
        tabBarInactiveTintColor: theme.colors.tabBarInactive,
        tabBarLabelStyle: {
          fontSize: 10,
          fontWeight: '600',
          letterSpacing: 0.2,
        },
        tabBarStyle: {
          backgroundColor: theme.colors.tabBarBackground,
          borderTopColor: theme.colors.tabBarBorder,
          borderTopWidth: 1,
          minHeight: Platform.OS === 'ios' ? 84 : 64,
          paddingTop: 6,
          paddingBottom: Platform.OS === 'ios' ? 22 : 10,
        },
        tabBarItemStyle: {
          minHeight: 44,
        },
        headerStyle: {
          backgroundColor: theme.colors.background,
        },
        headerTitleStyle: {
          fontWeight: '700',
          color: theme.colors.text,
        },
        headerShadowVisible: false,
        headerTintColor: theme.colors.accentCyan,
      }}>
      <Tabs.Screen
        name="index"
        options={{ title: 'Home', tabBarIcon: createTabBarIcon('index') }}
      />
      <Tabs.Screen
        name="training"
        options={{
          title: 'Training',
          headerShown: false,
          tabBarIcon: createTabBarIcon('training'),
        }}
      />
      <Tabs.Screen
        name="recovery"
        options={{
          title: 'Recovery',
          headerShown: false,
          tabBarIcon: createTabBarIcon('recovery'),
        }}
      />
      <Tabs.Screen
        name="performance"
        options={{
          title: 'Performance',
          headerShown: false,
          tabBarIcon: createTabBarIcon('performance'),
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Profile',
          headerShown: false,
          tabBarIcon: createTabBarIcon('profile'),
        }}
      />
    </Tabs>
  );
}
