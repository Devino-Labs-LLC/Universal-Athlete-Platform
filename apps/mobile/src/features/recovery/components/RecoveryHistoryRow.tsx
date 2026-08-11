import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
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
      style={[styles.row, { borderColor: theme.colors.border, backgroundColor: theme.colors.surface }]}
      testID={`history-row-${day.date}`}>
      <View style={styles.header}>
        <Text style={[styles.date, { color: theme.colors.text }]}>{day.date}</Text>
        <Text style={[styles.completeness, { color: theme.colors.textMuted }]}>
          {checkIn.completeness.replace(/_/g, ' ').toLowerCase()}
        </Text>
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
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    borderWidth: 1,
    borderRadius: 10,
    padding: 12,
    gap: 4,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  date: {
    fontSize: 15,
    fontWeight: '600',
  },
  completeness: {
    fontSize: 12,
  },
  metrics: {
    fontSize: 13,
  },
  load: {
    fontSize: 12,
  },
});
