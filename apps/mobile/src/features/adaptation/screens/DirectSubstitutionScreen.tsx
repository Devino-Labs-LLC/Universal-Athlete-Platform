import { useMemo, useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import {
  CandidateDisplay,
  getCandidateSelection,
} from '@/src/features/adaptation/components/CandidateCard';
import { CandidatePicker } from '@/src/features/adaptation/components/CandidatePicker';
import { SubstitutionReasonPicker } from '@/src/features/adaptation/components/SubstitutionReasonPicker';
import { useRevertSubstitution } from '@/src/features/adaptation/hooks/useRevertSubstitution';
import { useSubstituteExercise } from '@/src/features/adaptation/hooks/useSubstituteExercise';
import { useSubstitutionCandidates } from '@/src/features/adaptation/hooks/useSubstitutionCandidates';
import { useSubstitutionHistory } from '@/src/features/adaptation/hooks/useSubstitutionHistory';
import { athleteSubstitutionReasons } from '@/src/features/adaptation/models/adaptationLabels';
import { ExerciseSubstitutionReason } from '@/src/features/adaptation/models/adaptationSchemas';
import {
  adaptationErrorMessage,
  isSubstitutionLockedError,
} from '@/src/features/adaptation/utils/adaptationErrors';
import { useWorkoutExecution } from '@/src/features/training/execution/hooks/useWorkoutExecution';

interface DirectSubstitutionScreenProps {
  planId: string;
  dayId: string;
  occurrenceId: string;
  executionId: string;
}

export function DirectSubstitutionScreen({
  planId,
  dayId,
  occurrenceId,
  executionId,
}: DirectSubstitutionScreenProps) {
  const theme = useAppTheme();
  const { occurrenceQuery } = useWorkoutExecution(planId, dayId, occurrenceId);
  const candidatesQuery = useSubstitutionCandidates(planId, dayId, occurrenceId, executionId);
  const historyQuery = useSubstitutionHistory(planId, dayId, occurrenceId, executionId);
  const substituteMutation = useSubstituteExercise();
  const revertMutation = useRevertSubstitution();

  const execution = occurrenceQuery.data?.executions?.find((item) => item.id === executionId);
  const [selectedCandidate, setSelectedCandidate] = useState<CandidateDisplay | null>(null);
  const [reason, setReason] = useState<ExerciseSubstitutionReason | null>(null);
  const [confirming, setConfirming] = useState(false);

  const candidates: CandidateDisplay[] = useMemo(
    () =>
      (candidatesQuery.data ?? []).map((candidate) => ({
        source: 'occurrence' as const,
        candidate,
      })),
    [candidatesQuery.data],
  );

  const activeHistory = (historyQuery.data ?? []).filter((entry) => !entry.reverted);
  const latestSubstitution = activeHistory[0];

  if (occurrenceQuery.isLoading && !occurrenceQuery.data) {
    return <LoadingView message="Loading exercise…" />;
  }

  if (candidatesQuery.isLoading && !candidatesQuery.data) {
    return <LoadingView message="Loading alternatives…" />;
  }

  if (candidatesQuery.isError) {
    const message = isApiError(candidatesQuery.error)
      ? candidatesQuery.error.message
      : 'Failed to load substitution options';
    return <ErrorView message={message} onRetry={() => candidatesQuery.refetch()} />;
  }

  if (!execution) {
    return <ErrorView message="Exercise not found" onRetry={() => router.back()} />;
  }

  const currentName = execution.performedExerciseName ?? execution.exerciseName ?? 'Exercise';
  const selectedName = selectedCandidate
    ? selectedCandidate.source === 'alternative'
      ? selectedCandidate.candidate.targetNameSnapshot
      : selectedCandidate.candidate.targetCanonicalName
    : null;

  const handleConfirmSubstitute = () => {
    if (!selectedCandidate || !reason) {
      Alert.alert('Selection required', 'Choose an alternative and a reason before continuing.');
      return;
    }

    const selection = getCandidateSelection(selectedCandidate);
    substituteMutation.mutate(
      {
        planId,
        dayId,
        occurrenceId,
        executionId,
        body: {
          exerciseDefinitionId: selection.targetExerciseDefinitionId,
          reason,
          substitutionRelationshipId: selection.relationshipId,
        },
      },
      {
        onSuccess: () => router.back(),
        onError: (error) => {
          Alert.alert('Substitution failed', adaptationErrorMessage(error));
          if (isSubstitutionLockedError(error)) {
            router.back();
          }
        },
      },
    );
  };

  const handleRevert = () => {
    Alert.alert(
      'Revert substitution?',
      'This exercise will return to its prescribed movement.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Revert',
          onPress: () => {
            revertMutation.mutate(
              { planId, dayId, occurrenceId, executionId },
              {
                onSuccess: () => router.back(),
                onError: (error) =>
                  Alert.alert('Revert failed', adaptationErrorMessage(error)),
              },
            );
          },
        },
      ],
    );
  };

  return (
    <Screen scroll testID="direct-substitution-screen">
      <Text style={[styles.title, { color: theme.colors.text }]}>Substitute exercise</Text>
      <Text style={[styles.current, { color: theme.colors.textMuted }]}>Current: {currentName}</Text>

      {!confirming ? (
        <>
          <CandidatePicker
            candidates={candidates}
            onSelect={(candidate) => {
              setSelectedCandidate(candidate);
              setConfirming(true);
            }}
          />

          {latestSubstitution ? (
            <View style={styles.history}>
              <Text style={[styles.sectionTitle, { color: theme.colors.textMuted }]}>
                Recent substitution
              </Text>
              <Text style={[styles.historyLine, { color: theme.colors.text }]}>
                {latestSubstitution.fromExerciseName} → {latestSubstitution.toExerciseName}
              </Text>
              <PrimaryButton label="Revert to prescribed" onPress={handleRevert} />
            </View>
          ) : null}
        </>
      ) : (
        <>
          <Text style={[styles.confirmTitle, { color: theme.colors.text }]}>Confirm change</Text>
          <Text style={[styles.confirmBody, { color: theme.colors.text }]}>
            {currentName} → {selectedName}
          </Text>

          <Text style={[styles.sectionTitle, { color: theme.colors.textMuted }]}>
            Why are you substituting?
          </Text>
          <SubstitutionReasonPicker
            reasons={athleteSubstitutionReasons}
            selected={reason}
            onSelect={setReason}
          />

          <PrimaryButton
            testID="confirm-substitute"
            label="Confirm substitution"
            onPress={handleConfirmSubstitute}
            disabled={substituteMutation.isPending}
          />
          <PrimaryButton
            label="Back to alternatives"
            onPress={() => setConfirming(false)}
            disabled={substituteMutation.isPending}
          />
        </>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 20,
    fontWeight: '700',
  },
  current: {
    fontSize: 15,
  },
  confirmTitle: {
    fontSize: 18,
    fontWeight: '600',
  },
  confirmBody: {
    fontSize: 16,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.4,
  },
  history: {
    gap: 8,
    marginTop: 12,
  },
  historyLine: {
    fontSize: 14,
  },
});
