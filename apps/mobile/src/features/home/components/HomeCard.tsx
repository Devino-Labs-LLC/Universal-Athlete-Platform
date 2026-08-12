import { PropsWithChildren } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EyebrowText, Surface } from '@/src/core/components/Surface';

interface HomeCardProps extends PropsWithChildren {
  title?: string;
  subtitle?: string;
  eyebrow?: string;
  style?: ViewStyle;
  testID?: string;
}

export function HomeCard({
  title,
  subtitle,
  eyebrow,
  style,
  testID,
  children,
}: HomeCardProps) {
  const theme = useAppTheme();

  return (
    <Surface testID={testID} elevated style={style}>
      {eyebrow ? <EyebrowText tone="cyan">{eyebrow}</EyebrowText> : null}
      {title ? (
        <Text style={[styles.title, { color: theme.colors.text }]}>{title}</Text>
      ) : null}
      {subtitle ? (
        <Text style={[styles.subtitle, { color: theme.colors.textMuted }]}>{subtitle}</Text>
      ) : null}
      <View style={styles.body}>{children}</View>
    </Surface>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 17,
    fontWeight: '700',
  },
  subtitle: {
    fontSize: 13,
    lineHeight: 18,
  },
  body: {
    gap: 8,
  },
});
