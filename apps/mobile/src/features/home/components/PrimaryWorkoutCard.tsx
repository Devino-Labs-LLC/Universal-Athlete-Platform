import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  FEASIBILITY_STATUS_LABELS,
  occurrenceStatusLabel,
} from '@/src/features/home/models/todayLabels';
import {
  TrainingActionFlag,
  TrainingDashboardOccurrence,
} from '@/src/features/training/schemas';

interface PrimaryWorkoutCardProps {
  occurrence: TrainingDashboardOccurrence | null | undefined;
  canStartWorkout?: TrainingActionFlag;
  canContinueWorkout?: TrainingActionFlag;
  dominant?: boolean;
}

function readinessVariantForStatus(status: string): 'default' | 'success' | 'warning' | 'info' {
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

export function PrimaryWorkoutCard({
  occurrence,
  canStartWorkout,
  canContinueWorkout,
  dominant = false,
}: PrimaryWorkoutCardProps) {
  const theme = useAppTheme();

  const navigateToTraining = () => {
    router.push('/(tabs)/training');
  };

  if (!occurrence) {
    return (
      <HomeCard
        testID="primary-workout-card"
        eyebrow="Training"
        title="Today's workout">
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          No workout scheduled for today.
        </Text>
        <PrimaryButton label="View Training" onPress={navigateToTraining} />
      </HomeCard>
    );
  }

  const statusLabel = occurrenceStatusLabel(occurrence.status);
  const feasibilityLabel = occurrence.feasibilityStatus
    ? (FEASIBILITY_STATUS_LABELS[occurrence.feasibilityStatus] ??
      occurrence.feasibilityStatus)
    : null;

  const showContinue = canContinueWorkout?.allowed && occurrence.status === 'IN_PROGRESS';
  const showStart =
    canStartWorkout?.allowed &&
    (occurrence.status === 'SCHEDULED' || occurrence.status === 'IN_PROGRESS');
  const showViewTraining = occurrence.status === 'COMPLETED' || (!showContinue && !showStart);

  return (
    <HomeCard
      testID="primary-workout-card"
      eyebrow="Training"
      title={dominant ? "Today's workout" : occurrence.workoutDayName}
      subtitle={occurrence.trainingPlanName}
      style={dominant ? styles.dominant : undefined}>
      <View style={styles.row}>
        <StatusChip
          testID="workout-status-chip"
          label={statusLabel}
          variant={readinessVariantForStatus(occurrence.status)}
        />
        {feasibilityLabel ? (
          <StatusChip label={feasibilityLabel} variant="warning" />
        ) : null}
      </View>

      <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
        {occurrence.completedExerciseCount}/{occurrence.exerciseCount} exercises
      </Text>

      {occurrence.actualEnvironmentName || occurrence.plannedEnvironmentName ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Environment:{' '}
          {occurrence.actualEnvironmentName ?? occurrence.plannedEnvironmentName}
        </Text>
      ) : (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          No environment selected
        </Text>
      )}

      {showContinue ? (
        <PrimaryButton label="Continue Workout" onPress={navigateToTraining} />
      ) : null}
      {!showContinue && showStart ? (
        <PrimaryButton label="Start Workout" onPress={navigateToTraining} />
      ) : null}
      {showViewTraining ? (
        <PrimaryButton
          label={occurrence.status === 'COMPLETED' ? 'View Training' : 'View Training'}
          onPress={navigateToTraining}
        />
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  dominant: {
    borderWidth: 2,
  },
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  body: {
    fontSize: 15,
  },
  meta: {
    fontSize: 14,
  },
});
