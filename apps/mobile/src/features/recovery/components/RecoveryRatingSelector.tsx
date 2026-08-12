import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import {
  labelsForMetric,
  metricDisplayName,
  RecoveryRatingMetric,
} from '@/src/features/recovery/models/ratingLabels';

interface RecoveryRatingSelectorProps {
  metric: RecoveryRatingMetric;
  value?: number;
  onChange: (value: number) => void;
  testID?: string;
}

export function RecoveryRatingSelector({
  metric,
  value,
  onChange,
  testID,
}: RecoveryRatingSelectorProps) {
  const theme = useAppTheme();
  const labels = labelsForMetric(metric);
  const title = metricDisplayName(metric);

  return (
    <View style={styles.container} testID={testID}>
      <Text style={[styles.title, { color: theme.colors.text }]}>{title}</Text>
      <View style={styles.row}>
        {[1, 2, 3, 4, 5].map((rating) => {
          const selected = value === rating;
          const label = labels[rating] ?? String(rating);
          return (
            <Pressable
              key={rating}
              accessibilityRole="button"
              accessibilityState={{ selected }}
              accessibilityLabel={`${title}, ${label}, ${rating} of 5`}
              testID={testID ? `${testID}-${rating}` : undefined}
              onPress={() => onChange(rating)}
              style={({ pressed }) => [
                styles.button,
                {
                  borderColor: selected ? theme.colors.primary : theme.colors.border,
                  backgroundColor: selected
                    ? theme.colors.primaryMuted
                    : theme.colors.surface,
                  opacity: pressed ? 0.85 : 1,
                },
              ]}>
              <Text
                style={[
                  styles.ratingNumber,
                  { color: selected ? theme.colors.primary : theme.colors.text },
                ]}>
                {rating}
              </Text>
              <Text
                style={[
                  styles.ratingLabel,
                  { color: selected ? theme.colors.primary : theme.colors.textMuted },
                ]}
                numberOfLines={2}>
                {label}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 8,
  },
  title: {
    fontSize: 15,
    fontWeight: '600',
  },
  row: {
    flexDirection: 'row',
    gap: 6,
  },
  button: {
    flex: 1,
    borderWidth: 2,
    borderRadius: 10,
    paddingVertical: 10,
    paddingHorizontal: 4,
    alignItems: 'center',
    minHeight: 72,
    justifyContent: 'center',
    gap: 4,
  },
  ratingNumber: {
    fontSize: 18,
    fontWeight: '700',
  },
  ratingLabel: {
    fontSize: 10,
    textAlign: 'center',
  },
});
