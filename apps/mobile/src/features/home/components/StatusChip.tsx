import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

type StatusChipVariant = 'default' | 'success' | 'warning' | 'danger' | 'info';

interface StatusChipProps {
  label: string;
  variant?: StatusChipVariant;
  testID?: string;
}

export function StatusChip({ label, variant = 'default', testID }: StatusChipProps) {
  const theme = useAppTheme();

  const { backgroundColor, textColor } = (() => {
    switch (variant) {
      case 'success':
        return {
          backgroundColor: theme.colors.successMuted,
          textColor: theme.colors.success,
        };
      case 'warning':
        return {
          backgroundColor: theme.colors.warningMuted,
          textColor: theme.colors.warning,
        };
      case 'danger':
        return {
          backgroundColor: theme.colors.dangerMuted,
          textColor: theme.colors.danger,
        };
      case 'info':
        return {
          backgroundColor: theme.colors.infoMuted,
          textColor: theme.colors.info,
        };
      default:
        return {
          backgroundColor: theme.colors.surfaceMuted,
          textColor: theme.colors.textMuted,
        };
    }
  })();

  return (
    <View
      testID={testID}
      accessibilityRole="text"
      accessibilityLabel={label}
      style={[styles.chip, { backgroundColor }]}>
      <Text style={[styles.label, { color: textColor }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  chip: {
    alignSelf: 'flex-start',
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 5,
    minHeight: 28,
    justifyContent: 'center',
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
  },
});
