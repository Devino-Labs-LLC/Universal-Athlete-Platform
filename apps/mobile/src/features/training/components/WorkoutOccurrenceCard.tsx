import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { occurrenceStatusLabel } from '@/src/features/home/models/todayLabels';
import { CalendarEntry, OverviewOccurrence } from '@/src/features/training/models/browseSchemas';

type OccurrenceLike = Pick<
  CalendarEntry | OverviewOccurrence,
  | 'occurrenceId'
  | 'trainingPlanId'
  | 'trainingPlanName'
  | 'workoutDayId'
  | 'workoutDayName'
  | 'scheduledDate'
  | 'status'
  | 'exerciseCount'
  | 'completedExerciseCount'
>;

interface WorkoutOccurrenceCardProps {
  occurrence: OccurrenceLike;
  onPress?: () => void;
  onPrimaryAction?: () => void;
  showPrimaryAction?: boolean;
  primaryActionLabel?: string;
  testID?: string;
}

function statusVariant(status: string): 'default' | 'success' | 'warning' | 'info' {
  switch (status) {
    case 'IN_PROGRESS':
      return 'info';
    case 'COMPLETED':
      return 'success';
    case 'SCHEDULED':
      return 'default';
    default:
      return 'warning';
  }
}

function defaultActionLabel(status: string): string | null {
  switch (status) {
    case 'IN_PROGRESS':
      return 'Continue';
    case 'SCHEDULED':
      return 'Prepare';
    case 'COMPLETED':
      return 'Review';
    default:
      return 'View';
  }
}

export function WorkoutOccurrenceCard({
  occurrence,
  onPress,
  onPrimaryAction,
  showPrimaryAction = true,
  primaryActionLabel,
  testID,
}: WorkoutOccurrenceCardProps) {
  const theme = useAppTheme();
  const actionLabel = primaryActionLabel ?? defaultActionLabel(occurrence.status);

  const content = (
    <>
      <View style={styles.header}>
        <Text style={[styles.title, { color: theme.colors.text }]}>{occurrence.workoutDayName}</Text>
        <StatusChip
          testID={`${testID ?? 'occurrence-card'}-status`}
          label={occurrenceStatusLabel(occurrence.status)}
          variant={statusVariant(occurrence.status)}
        />
      </View>
      <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
        {occurrence.trainingPlanName} · {occurrence.scheduledDate}
      </Text>
      <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
        {occurrence.completedExerciseCount}/{occurrence.exerciseCount} exercises
      </Text>
      {showPrimaryAction && actionLabel && onPrimaryAction ? (
        <PrimaryButton label={actionLabel} onPress={onPrimaryAction} />
      ) : null}
    </>
  );

  if (onPress) {
    return (
      <Pressable
        testID={testID ?? 'workout-occurrence-card'}
        onPress={onPress}
        style={({ pressed }) => [{ opacity: pressed ? 0.85 : 1 }]}>
        <HomeCard>{content}</HomeCard>
      </Pressable>
    );
  }

  return (
    <HomeCard testID={testID ?? 'workout-occurrence-card'}>{content}</HomeCard>
  );
}

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 8,
  },
  title: {
    flex: 1,
    fontSize: 16,
    fontWeight: '600',
  },
  meta: {
    fontSize: 14,
  },
});
