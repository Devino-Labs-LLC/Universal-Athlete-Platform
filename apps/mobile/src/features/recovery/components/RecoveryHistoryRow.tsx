import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { StatusBadge, Surface } from '@/src/core/components/Surface';
import { ratingLabelForMetric } from '@/src/features/recovery/models/ratingLabels';
import { AthleteRecoveryHistory } from '@/src/features/recovery/models/recoverySchemas';
import { formatSleepDuration } from '@/src/features/recovery/utils/sleepDuration';

interface RecoveryHistoryRowProps {
  day: AthleteRecoveryHistory['days'][number];
  onPress: () => void;
}

export function RecoveryHistoryRow({ day, onPress }: RecoveryHistoryRowProps) {
  const theme = useAppTheme();
  const checkIn = day.checkIn;
  const sleep = formatSleepDuration(checkIn.sleepDurationMinutes ?? undefined);

  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      testID={`history-row-${day.date}`}>
      <Surface elevated style={styles.row}>
        <View style={styles.header}>
          <Text style={[styles.date, { color: theme.colors.text }]}>{day.date}</Text>
          <StatusBadge
            label={checkIn.completeness.replace(/_/g, ' ').toLowerCase()}
            tone="info"
          />
        </View>
        <Text style={[styles.metrics, { color: theme.colors.textMuted }]}>
          Fatigue {ratingLabelForMetric('fatigue', checkIn.fatigue.value)} · Mood{' '}
          {ratingLabelForMetric('mood', checkIn.mood.value)}
          {sleep ? ` · Sleep ${sleep}` : ''}
        </Text>
        {day.trainingLoad ? (
          <Text style={[styles.load, { color: theme.colors.textMuted }]}>
            {day.trainingLoad.completedExerciseCount} exercises ·{' '}
            {day.trainingLoad.completedSetCount} sets
          </Text>
        ) : null}
      </Surface>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    gap: 4,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 8,
  },
  date: {
    fontSize: 15,
    fontWeight: '600',
    flex: 1,
  },
  metrics: {
    fontSize: 13,
  },
  load: {
    fontSize: 12,
  },
});
