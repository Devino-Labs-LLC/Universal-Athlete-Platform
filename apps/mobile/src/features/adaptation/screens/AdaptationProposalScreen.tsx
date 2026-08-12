import { useCallback, useState } from 'react';
import { Alert, StyleSheet, Text } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Button, PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { AdaptationItemCard } from '@/src/features/adaptation/components/AdaptationItemCard';
import { ApplySummarySheet } from '@/src/features/adaptation/components/ApplySummarySheet';
import { ContextOnlyAdjustmentsCard } from '@/src/features/adaptation/components/ContextOnlyAdjustmentsCard';
import { ProposalHeader } from '@/src/features/adaptation/components/ProposalHeader';
import { useAdaptationProposal } from '@/src/features/adaptation/hooks/useAdaptationProposal';
import { useApplyAdaptation } from '@/src/features/adaptation/hooks/useApplyAdaptation';
import { useCancelAdaptation } from '@/src/features/adaptation/hooks/useCancelAdaptation';
import { useRegenerateAdaptation } from '@/src/features/adaptation/hooks/useRegenerateAdaptation';
import { useUpdateAdaptationItem } from '@/src/features/adaptation/hooks/useUpdateAdaptationItem';
import {
  canApplyProposal,
  contextOnlyAdjustments,
  isProposalMutable,
} from '@/src/features/adaptation/models/adaptationSchemas';
import {
  adaptationErrorMessage,
  isVersionConflictError,
} from '@/src/features/adaptation/utils/adaptationErrors';
import {
  navigateToAdaptationCandidatePicker,
} from '@/src/features/adaptation/utils/proposalNavigation';
import { navigateToOccurrenceLaunch } from '@/src/features/training/utils/trainingNavigation';

interface AdaptationProposalScreenProps {
  planId: string;
  dayId: string;
  occurrenceId: string;
  proposalId: string;
}

export function AdaptationProposalScreen({
  planId,
  dayId,
  occurrenceId,
  proposalId,
}: AdaptationProposalScreenProps) {
  const theme = useAppTheme();
  const proposalQuery = useAdaptationProposal(proposalId);
  const updateItemMutation = useUpdateAdaptationItem();
  const applyMutation = useApplyAdaptation();
  const cancelMutation = useCancelAdaptation();
  const regenerateMutation = useRegenerateAdaptation();
  const [applySheetVisible, setApplySheetVisible] = useState(false);

  const scope = { planId, dayId, occurrenceId, proposalId };

  const handleMutationError = useCallback(
    (error: unknown, fallback: string) => {
      if (isVersionConflictError(error)) {
        void proposalQuery.refetch();
      }
      Alert.alert(fallback, adaptationErrorMessage(error));
    },
    [proposalQuery],
  );

  const patchItem = useCallback(
    (
      itemId: string,
      body: Parameters<typeof updateItemMutation.mutate>[0]['body'],
    ) => {
      updateItemMutation.mutate(
        { ...scope, itemId, body },
        {
          onError: (error) => handleMutationError(error, 'Could not update item'),
        },
      );
    },
    [handleMutationError, scope, updateItemMutation],
  );

  if (proposalQuery.isLoading && !proposalQuery.data) {
    return <LoadingView message="Loading adaptation proposal…" />;
  }

  if (proposalQuery.isError && !proposalQuery.data) {
    const message = isApiError(proposalQuery.error)
      ? proposalQuery.error.message
      : 'Failed to load adaptation proposal';
    return <ErrorView message={message} onRetry={() => proposalQuery.refetch()} />;
  }

  const proposal = proposalQuery.data;
  if (!proposal) {
    return <LoadingView message="Loading adaptation proposal…" />;
  }

  const sortedItems = [...proposal.items].sort((a, b) => a.executionOrder - b.executionOrder);
  const mutable = isProposalMutable(proposal.status);
  const readyToApply = canApplyProposal(proposal);
  const readOnly = !mutable || proposal.status === 'APPLIED';
  const showRegenerate = proposal.status === 'STALE' || proposal.status === 'EXPIRED';
  const contextAdjustments = contextOnlyAdjustments(proposal);
  const anyPending =
    updateItemMutation.isPending ||
    applyMutation.isPending ||
    cancelMutation.isPending ||
    regenerateMutation.isPending;

  const handleApply = () => {
    applyMutation.mutate(
      {
        ...scope,
        expectedProposalVersion: proposal.version,
      },
      {
        onSuccess: () => {
          setApplySheetVisible(false);
          navigateToOccurrenceLaunch(planId, dayId, occurrenceId);
        },
        onError: (error) => handleMutationError(error, 'Could not apply adaptation'),
      },
    );
  };

  const handleCancel = () => {
    Alert.alert(
      'Cancel proposal?',
      'Your review decisions will be discarded.',
      [
        { text: 'Keep reviewing', style: 'cancel' },
        {
          text: 'Cancel proposal',
          style: 'destructive',
          onPress: () => {
            cancelMutation.mutate(scope, {
              onSuccess: () => router.back(),
              onError: (error) => handleMutationError(error, 'Could not cancel proposal'),
            });
          },
        },
      ],
    );
  };

  const handleRegenerate = () => {
    regenerateMutation.mutate(scope, {
      onSuccess: (next) => {
        router.replace(
          `/(tabs)/training/plans/${next.trainingPlanId}/days/${next.workoutDayId}/occurrences/${next.workoutOccurrenceId}/adaptation/${next.id}`,
        );
      },
      onError: (error) => handleMutationError(error, 'Could not regenerate proposal'),
    });
  };

  return (
    <Screen scroll testID="adaptation-proposal-screen">
      <ProposalHeader proposal={proposal} />
      <ContextOnlyAdjustmentsCard adjustments={contextAdjustments} />

      {sortedItems.map((item) => (
        <AdaptationItemCard
          key={item.id}
          item={item}
          proposalStatus={proposal.status}
          readOnly={readOnly}
          pending={updateItemMutation.isPending}
          onAccept={() => patchItem(item.id, { decision: 'ACCEPTED' })}
          onChooseAnother={() =>
            navigateToAdaptationCandidatePicker(planId, dayId, occurrenceId, proposalId, item.id)
          }
          onReject={() => patchItem(item.id, { decision: 'REJECTED' })}
          onReset={() => patchItem(item.id, { decision: 'PENDING' })}
        />
      ))}

      {proposal.status === 'APPLIED' ? (
        <>
          <Text style={[styles.appliedNote, { color: theme.colors.textMuted }]}>
            This adaptation was applied to your workout.
          </Text>
          <PrimaryButton
            label="Return to Workout"
            onPress={() => navigateToOccurrenceLaunch(planId, dayId, occurrenceId)}
          />
        </>
      ) : null}

      {showRegenerate ? (
        <PrimaryButton
          testID="regenerate-adaptation"
          label="Regenerate Proposal"
          onPress={handleRegenerate}
          loading={regenerateMutation.isPending}
          disabled={anyPending}
        />
      ) : null}

      {mutable ? (
        <>
          {readyToApply ? (
            <PrimaryButton
              testID="apply-adaptation"
              label="Apply Adaptation"
              onPress={() => setApplySheetVisible(true)}
              disabled={anyPending}
            />
          ) : null}
          <Button
            variant="destructive"
            testID="cancel-adaptation"
            label="Cancel Proposal"
            onPress={handleCancel}
            disabled={anyPending}
          />
        </>
      ) : null}

      <ApplySummarySheet
        visible={applySheetVisible}
        proposal={proposal}
        pending={applyMutation.isPending}
        onConfirm={handleApply}
        onDismiss={() => setApplySheetVisible(false)}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  appliedNote: {
    fontSize: 14,
  },
});
