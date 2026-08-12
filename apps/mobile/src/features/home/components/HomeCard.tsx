import { PropsWithChildren } from 'react';
import { StyleProp, StyleSheet, Text, View, ViewStyle } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EyebrowText, Surface } from '@/src/core/components/Surface';

interface HomeCardProps extends PropsWithChildren {
  title?: string;
  subtitle?: string;
  eyebrow?: string;
  /** Tighter padding for dense performance cards. */
  dense?: boolean;
  style?: StyleProp<ViewStyle>;
  testID?: string;
}

export function HomeCard({
  title,
  subtitle,
  eyebrow,
  dense = false,
  style,
  testID,
  children,
}: HomeCardProps) {
  const theme = useAppTheme();

  return (
    <Surface
      testID={testID}
      elevated
      style={[
        dense ? { padding: theme.spacing.md, gap: theme.spacing.xs } : null,
        style,
      ]}>
      {eyebrow ? <EyebrowText tone="cyan">{eyebrow}</EyebrowText> : null}
      {title ? (
        <Text
          style={[
            styles.title,
            {
              color: theme.colors.text,
              fontSize: dense ? 16 : theme.typography.sectionTitle,
            },
          ]}>
          {title}
        </Text>
      ) : null}
      {subtitle ? (
        <Text style={[styles.subtitle, { color: theme.colors.textMuted }]}>{subtitle}</Text>
      ) : null}
      <View style={[styles.body, dense && styles.bodyDense]}>{children}</View>
    </Surface>
  );
}

const styles = StyleSheet.create({
  title: {
    fontWeight: '700',
  },
  subtitle: {
    fontSize: 13,
    lineHeight: 18,
  },
  body: {
    gap: 8,
  },
  bodyDense: {
    gap: 6,
  },
});
