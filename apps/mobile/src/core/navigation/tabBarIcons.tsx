import { Ionicons } from '@expo/vector-icons';
import type { ComponentProps } from 'react';

export type TabRouteName = 'index' | 'training' | 'recovery' | 'performance' | 'profile';

export type TabIconName = ComponentProps<typeof Ionicons>['name'];

/** Stable icon identities for each bottom tab — outline vs filled by focus. */
export const TAB_ICON_MAP: Record<
  TabRouteName,
  { focused: TabIconName; unfocused: TabIconName; label: string }
> = {
  index: { focused: 'home', unfocused: 'home-outline', label: 'Home' },
  training: { focused: 'barbell', unfocused: 'barbell-outline', label: 'Training' },
  recovery: { focused: 'heart', unfocused: 'heart-outline', label: 'Recovery' },
  performance: {
    focused: 'stats-chart',
    unfocused: 'stats-chart-outline',
    label: 'Performance',
  },
  profile: { focused: 'person', unfocused: 'person-outline', label: 'Profile' },
};

export function resolveTabIconName(
  route: TabRouteName,
  focused: boolean,
): TabIconName {
  const entry = TAB_ICON_MAP[route];
  return focused ? entry.focused : entry.unfocused;
}

export function TabBarIcon({
  route,
  focused,
  color,
  size = 22,
}: {
  route: TabRouteName;
  focused: boolean;
  color: string;
  size?: number;
}) {
  const name = resolveTabIconName(route, focused);
  return (
    <Ionicons
      name={name}
      size={size}
      color={color}
      accessibilityLabel={`${TAB_ICON_MAP[route].label} tab`}
    />
  );
}
