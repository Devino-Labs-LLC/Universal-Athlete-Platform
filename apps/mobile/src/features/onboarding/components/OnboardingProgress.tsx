import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EyebrowText } from '@/src/core/components/Surface';

const STEPS = [
  { key: 'profile', label: 'Profile' },
  { key: 'sports', label: 'Sports' },
  { key: 'goals', label: 'Goals' },
] as const;

export type OnboardingStepKey = (typeof STEPS)[number]['key'];

interface OnboardingProgressProps {
  current: OnboardingStepKey;
}

export function OnboardingProgress({ current }: OnboardingProgressProps) {
  const theme = useAppTheme();
  const currentIndex = STEPS.findIndex((step) => step.key === current);

  return (
    <View style={styles.wrap} accessibilityRole="summary" accessibilityLabel={`Setup step ${currentIndex + 1} of 3`}>
      <EyebrowText tone="cyan">Athlete setup</EyebrowText>
      <View style={styles.row}>
        {STEPS.map((step, index) => {
          const active = index === currentIndex;
          const complete = index < currentIndex;
          return (
            <View key={step.key} style={styles.step}>
              <View
                style={[
                  styles.dot,
                  {
                    backgroundColor: active || complete ? theme.colors.accentCyan : theme.colors.border,
                  },
                ]}
              />
              <Text
                style={[
                  styles.label,
                  {
                    color: active ? theme.colors.text : theme.colors.textMuted,
                    fontWeight: active ? '700' : '500',
                  },
                ]}>
                {step.label}
              </Text>
            </View>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    gap: 10,
  },
  row: {
    flexDirection: 'row',
    gap: 12,
  },
  step: {
    flex: 1,
    gap: 6,
  },
  dot: {
    height: 4,
    borderRadius: 999,
  },
  label: {
    fontSize: 12,
  },
});
