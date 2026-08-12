import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button, PrimaryButton } from '@/src/core/components/PrimaryButton';
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
  /** Highlight the next/current session. */
  dominant?: boolean;
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

function eyebrowFor(status: string, dominant?: boolean): string {
  if (dominant && status === 'IN_PROGRESS') {
    return 'In progress';
  }
  if (dominant) {
    return 'Next session';
  }
  return 'Session';
}

export function WorkoutOccurrenceCard({
  occurrence,
  onPress,
  onPrimaryAction,
  showPrimaryAction = true,
  primaryActionLabel,
  dominant = false,
  testID,
}: WorkoutOccurrenceCardProps) {
  const theme = useAppTheme();
  const actionLabel = primaryActionLabel ?? defaultActionLabel(occurrence.status);
  const exerciseCount =
    occurrence.exerciseCount == null ? null : occurrence.exerciseCount;
  const completedCount =
    occurrence.completedExerciseCount == null ? null : occurrence.completedExerciseCount;
  const progressLabel =
    exerciseCount == null
      ? 'Exercise count unavailable'
      : completedCount == null
        ? `${exerciseCount} exercises`
        : `${completedCount}/${exerciseCount} exercises`;

  const content = (
    <>
      <View style={styles.header}>
        <Text
          style={[
            styles.title,
            {
              color: theme.colors.text,
              fontSize: dominant ? theme.typography.sectionTitle : 16,
            },
          ]}
          numberOfLines={2}>
          {occurrence.workoutDayName}
        </Text>
        <StatusChip
          testID={`${testID ?? 'occurrence-card'}-status`}
          label={occurrenceStatusLabel(occurrence.status)}
          variant={statusVariant(occurrence.status)}
        />
      </View>
      <Text style={[styles.meta, { color: theme.colors.textMuted }]} numberOfLines={2}>
        {occurrence.trainingPlanName} · {occurrence.scheduledDate}
      </Text>
      <Text style={[styles.progress, { color: theme.colors.text }]}>{progressLabel}</Text>
      {showPrimaryAction && actionLabel && onPrimaryAction ? (
        dominant ? (
          <PrimaryButton label={actionLabel} onPress={onPrimaryAction} />
        ) : (
          <Button label={actionLabel} onPress={onPrimaryAction} variant="secondary" />
        )
      ) : null}
    </>
  );

  if (onPress) {
    return (
      <Pressable
        testID={testID ?? 'workout-occurrence-card'}
        accessibilityRole="button"
        onPress={onPress}
        style={({ pressed }) => [{ opacity: pressed ? 0.85 : 1 }]}>
        <HomeCard
          dense
          eyebrow={eyebrowFor(occurrence.status, dominant)}
          style={
            dominant
              ? { borderColor: theme.colors.primary, borderWidth: 1.5 }
              : undefined
          }>
          {content}
        </HomeCard>
      </Pressable>
    );
  }

  return (
    <HomeCard
      testID={testID ?? 'workout-occurrence-card'}
      dense
      eyebrow={eyebrowFor(occurrence.status, dominant)}
      style={
        dominant ? { borderColor: theme.colors.primary, borderWidth: 1.5 } : undefined
      }>
      {content}
    </HomeCard>
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
    fontWeight: '700',
  },
  meta: {
    fontSize: 13,
  },
  progress: {
    fontSize: 14,
    fontWeight: '700',
  },
});
