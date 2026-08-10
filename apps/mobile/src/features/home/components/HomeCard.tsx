import { PropsWithChildren } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

interface HomeCardProps extends PropsWithChildren {
  title?: string;
  subtitle?: string;
  style?: ViewStyle;
  testID?: string;
}

export function HomeCard({ title, subtitle, style, testID, children }: HomeCardProps) {
  const theme = useAppTheme();

  return (
    <View
      testID={testID}
      style={[
        styles.card,
        {
          backgroundColor: theme.colors.surface,
          borderColor: theme.colors.border,
        },
        style,
      ]}>
      {title ? (
        <Text style={[styles.title, { color: theme.colors.text }]}>{title}</Text>
      ) : null}
      {subtitle ? (
        <Text style={[styles.subtitle, { color: theme.colors.textMuted }]}>{subtitle}</Text>
      ) : null}
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 16,
    gap: 8,
  },
  title: {
    fontSize: 17,
    fontWeight: '700',
  },
  subtitle: {
    fontSize: 13,
  },
});
