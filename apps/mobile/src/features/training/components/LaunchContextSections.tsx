import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  FEASIBILITY_STATUS_LABELS,
  occurrenceStatusLabel,
  readinessBandLabel,
  recommendationActionLabel,
} from '@/src/features/home/models/todayLabels';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';
import { WorkoutLaunchContext } from '@/src/features/training/models/browseSchemas';
import { formatLaunchExercisePrescription } from '@/src/features/training/utils/prescriptionFormat';

interface LaunchContextSectionsProps {
  context: WorkoutLaunchContext;
}

export function LaunchContextSections({ context }: LaunchContextSectionsProps) {
  const theme = useAppTheme();
  const { occurrence, environment, feasibility, recommendationContext, adaptation, exercises } =
    context;

  return (
    <>
      <HomeCard title="Workout" testID="launch-occurrence-section">
        <StatusChip
          label={occurrenceStatusLabel(occurrence.status)}
          variant={occurrence.status === 'IN_PROGRESS' ? 'info' : 'default'}
        />
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Scheduled {occurrence.scheduledDate}
        </Text>
        {occurrence.startEligible != null ? (
          <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
            Start eligible: {occurrence.startEligible ? 'Yes' : 'No'}
          </Text>
        ) : null}
      </HomeCard>

      <HomeCard title="Environment" testID="launch-environment-section">
        {environment ? (
          <>
            <Text style={[styles.meta, { color: theme.colors.text }]}>
              Planned: {environment.plannedEnvironmentName ?? 'No training environment selected'}
            </Text>
            <Text style={[styles.meta, { color: theme.colors.text }]}>
              Actual:{' '}
              {environment.actualEnvironmentName ??
                environment.plannedEnvironmentName ??
                'No training environment selected'}
            </Text>
          </>
        ) : (
          <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
            No training environment selected
          </Text>
        )}
      </HomeCard>

      {feasibility?.feasibilityPresent ? (
        <HomeCard title="Feasibility" testID="launch-feasibility-section">
          <StatusChip
            label={
              feasibility.status
                ? (FEASIBILITY_STATUS_LABELS[feasibility.status] ??
                  formatEnumLabel(feasibility.status))
                : 'Unknown'
            }
            variant="warning"
          />
          <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
            {feasibility.feasibleExercises ?? 0}/{feasibility.totalExercises ?? 0} exercises feasible
          </Text>
          {(feasibility.infeasibleExercises ?? 0) > 0 ? (
            <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
              {feasibility.infeasibleExercises} exercise
              {feasibility.infeasibleExercises === 1 ? '' : 's'} need alternatives
            </Text>
          ) : null}
        </HomeCard>
      ) : null}

      {recommendationContext?.recommendationPresent ? (
        <HomeCard title="Recommendation" testID="launch-recommendation-section">
          <Text style={[styles.meta, { color: theme.colors.text }]}>
            {recommendationActionLabel(recommendationContext.overallAction)}
          </Text>
          {recommendationContext.readinessBand ? (
            <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
              Readiness: {readinessBandLabel(recommendationContext.readinessBand)}
            </Text>
          ) : null}
        </HomeCard>
      ) : null}

      {adaptation?.activeProposalPresent ? (
        <HomeCard title="Adaptation proposal" testID="launch-adaptation-section">
          <StatusChip
            label={adaptation.status ? formatEnumLabel(adaptation.status) : 'Active'}
            variant="warning"
          />
          <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
            {adaptation.unresolvedCount ?? 0} unresolved change(s)
          </Text>
        </HomeCard>
      ) : null}

      {(exercises ?? []).length > 0 ? (
        <HomeCard title="Exercises" testID="launch-exercises-section">
          <View style={styles.exerciseList}>
            {(exercises ?? []).map((exercise) => (
              <View key={exercise.executionId} style={styles.exerciseRow}>
                <Text style={[styles.exerciseName, { color: theme.colors.text }]}>
                  {exercise.performedExerciseName ?? exercise.prescribedExerciseName ?? 'Exercise'}
                </Text>
                {exercise.substituted ? (
                  <StatusChip label="Substituted" variant="warning" />
                ) : null}
                <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
                  {formatLaunchExercisePrescription(exercise)}
                </Text>
              </View>
            ))}
          </View>
        </HomeCard>
      ) : null}
    </>
  );
}

const styles = StyleSheet.create({
  meta: {
    fontSize: 14,
  },
  exerciseList: {
    gap: 12,
  },
  exerciseRow: {
    gap: 4,
  },
  exerciseName: {
    fontSize: 15,
    fontWeight: '600',
  },
});
