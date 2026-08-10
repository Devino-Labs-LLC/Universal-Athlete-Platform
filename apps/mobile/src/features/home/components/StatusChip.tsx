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

  const backgroundColor = (() => {
    switch (variant) {
      case 'success':
        return `${theme.colors.success}22`;
      case 'warning':
        return `${theme.colors.warning}22`;
      case 'danger':
        return `${theme.colors.danger}22`;
      case 'info':
        return `${theme.colors.info}22`;
      default:
        return `${theme.colors.textMuted}22`;
    }
  })();

  const textColor = (() => {
    switch (variant) {
      case 'success':
        return theme.colors.success;
      case 'warning':
        return theme.colors.warning;
      case 'danger':
        return theme.colors.danger;
      case 'info':
        return theme.colors.info;
      default:
        return theme.colors.textMuted;
    }
  })();

  return (
    <View testID={testID} style={[styles.chip, { backgroundColor }]}>
      <Text style={[styles.label, { color: textColor }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  chip: {
    alignSelf: 'flex-start',
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
  },
});
