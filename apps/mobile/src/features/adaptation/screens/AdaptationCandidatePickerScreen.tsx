import { useMemo } from 'react';
import { Alert, StyleSheet, Text } from 'react-native';
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
import { useAdaptationProposal } from '@/src/features/adaptation/hooks/useAdaptationProposal';
import { useSubstitutionCandidates } from '@/src/features/adaptation/hooks/useSubstitutionCandidates';
import { useUpdateAdaptationItem } from '@/src/features/adaptation/hooks/useUpdateAdaptationItem';
import {
  adaptationErrorMessage,
  isVersionConflictError,
} from '@/src/features/adaptation/utils/adaptationErrors';

interface AdaptationCandidatePickerScreenProps {
  planId: string;
  dayId: string;
  occurrenceId: string;
  proposalId: string;
  itemId: string;
}

export function AdaptationCandidatePickerScreen({
  planId,
  dayId,
  occurrenceId,
  proposalId,
  itemId,
}: AdaptationCandidatePickerScreenProps) {
  const theme = useAppTheme();
  const proposalQuery = useAdaptationProposal(proposalId);
  const updateItemMutation = useUpdateAdaptationItem();

  const item = proposalQuery.data?.items.find((entry) => entry.id === itemId);
  const executionId = item?.workoutExerciseExecutionId ?? '';

  const fallbackCandidatesQuery = useSubstitutionCandidates(
    planId,
    dayId,
    occurrenceId,
    executionId,
    Boolean(item && (item.alternatives ?? []).length === 0),
  );

  const candidates: CandidateDisplay[] = useMemo(() => {
    const alternatives = item?.alternatives ?? [];
    if (alternatives.length > 0) {
      return alternatives.map((candidate) => ({ source: 'alternative' as const, candidate }));
    }
    return (fallbackCandidatesQuery.data ?? []).map((candidate) => ({
      source: 'occurrence' as const,
      candidate,
    }));
  }, [fallbackCandidatesQuery.data, item?.alternatives]);

  if (proposalQuery.isLoading && !proposalQuery.data) {
    return <LoadingView message="Loading alternatives…" />;
  }

  if (proposalQuery.isError && !proposalQuery.data) {
    const message = isApiError(proposalQuery.error)
      ? proposalQuery.error.message
      : 'Failed to load proposal';
    return <ErrorView message={message} onRetry={() => proposalQuery.refetch()} />;
  }

  if (!item) {
    return <ErrorView message="Adaptation item not found" onRetry={() => router.back()} />;
  }

  const handleSelect = (display: CandidateDisplay) => {
    const selection = getCandidateSelection(display);
    updateItemMutation.mutate(
      {
        planId,
        dayId,
        occurrenceId,
        proposalId,
        itemId,
        body: {
          decision: 'OVERRIDDEN',
          targetExerciseDefinitionId: selection.targetExerciseDefinitionId,
          substitutionRelationshipId: selection.relationshipId,
        },
      },
      {
        onSuccess: () => router.back(),
        onError: (error) => {
          if (isVersionConflictError(error)) {
            void proposalQuery.refetch();
          }
          Alert.alert('Could not save selection', adaptationErrorMessage(error));
        },
      },
    );
  };

  return (
    <Screen scroll testID="adaptation-candidate-picker-screen">
      <Text style={[styles.title, { color: theme.colors.text }]}>
        Choose alternative for {item.currentPerformedNameSnapshot}
      </Text>

      <CandidatePicker
        candidates={candidates}
        selectedExerciseDefinitionId={item.selectedTargetExerciseDefinitionId}
        onSelect={handleSelect}
      />

      {fallbackCandidatesQuery.isFetching ? (
        <Text style={[styles.loading, { color: theme.colors.textMuted }]}>Loading more options…</Text>
      ) : null}

      {updateItemMutation.isPending ? (
        <PrimaryButton label="Saving…" disabled onPress={() => undefined} />
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 18,
    fontWeight: '700',
  },
  loading: {
    fontSize: 13,
  },
});
