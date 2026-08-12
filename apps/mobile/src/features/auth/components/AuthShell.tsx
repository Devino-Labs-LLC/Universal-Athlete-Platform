import { PropsWithChildren } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Screen } from '@/src/core/components/Screen';
import { EyebrowText, Surface } from '@/src/core/components/Surface';

interface AuthShellProps extends PropsWithChildren {
  title: string;
  subtitle?: string;
  testID?: string;
}

export function AuthShell({ title, subtitle, testID, children }: AuthShellProps) {
  const theme = useAppTheme();

  return (
    <Screen scroll includeBottomInset testID={testID} contentStyle={styles.content}>
      <View style={styles.brandBlock}>
        <EyebrowText tone="cyan" testID="auth-brand-eyebrow">
          Universal Athlete
        </EyebrowText>
        <Text testID="auth-brand-mark" style={[styles.mark, { color: theme.colors.primary }]}>
          UAP
        </Text>
        <Text style={[styles.title, { color: theme.colors.text }]}>{title}</Text>
        {subtitle ? (
          <Text style={[styles.subtitle, { color: theme.colors.textMuted }]}>{subtitle}</Text>
        ) : null}
      </View>
      <Surface elevated style={styles.formSurface}>
        {children}
      </Surface>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: {
    justifyContent: 'center',
    paddingVertical: 32,
  },
  brandBlock: {
    gap: 6,
    marginBottom: 8,
  },
  mark: {
    fontSize: 34,
    fontWeight: '800',
    letterSpacing: 2,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    letterSpacing: -0.3,
  },
  subtitle: {
    fontSize: 15,
    lineHeight: 21,
  },
  formSurface: {
    gap: 14,
    borderWidth: StyleSheet.hairlineWidth,
  },
});
