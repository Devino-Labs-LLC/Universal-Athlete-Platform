import { useCallback, useState } from 'react';
import { Alert, StyleSheet, Text } from 'react-native';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { fetchAdaptationProposal } from '@/src/features/adaptation/api/proposalApi';
import { adaptationErrorMessage } from '@/src/features/adaptation/utils/adaptationErrors';
import {
  navigateToAdaptationProposal,
  resolveAdaptationRouteFromToday,
} from '@/src/features/adaptation/utils/proposalNavigation';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';
import { TrainingTodayDashboard } from '@/src/features/training/schemas';

interface AdaptationCardProps {
  adaptation: NonNullable<TrainingTodayDashboard['adaptation']>;
  training: TrainingTodayDashboard['training'];
}

export function AdaptationCard({ adaptation, training }: AdaptationCardProps) {
  const theme = useAppTheme();
  const { apiClient } = useAuthSession();
  const [loading, setLoading] = useState(false);

  const handleReview = useCallback(async () => {
    const proposalId = adaptation.adaptationProposalId;
    if (!proposalId) {
      Alert.alert('Adaptation unavailable', 'No active adaptation proposal was found.');
      return;
    }

    const route = resolveAdaptationRouteFromToday(adaptation, training);
    if (route) {
      navigateToAdaptationProposal(
        route.planId,
        route.dayId,
        route.occurrenceId,
        proposalId,
      );
      return;
    }

    setLoading(true);
    try {
      const proposal = await fetchAdaptationProposal(apiClient, proposalId);
      navigateToAdaptationProposal(
        proposal.trainingPlanId,
        proposal.workoutDayId,
        proposal.workoutOccurrenceId,
        proposal.id,
      );
    } catch (error) {
      Alert.alert('Could not open adaptation', adaptationErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [adaptation, apiClient, training]);

  if (!adaptation.activeProposalPresent) {
    return null;
  }

  const statusLabel = adaptation.status ? formatEnumLabel(adaptation.status) : 'Active';
  const originLabel = adaptation.origin ? formatEnumLabel(adaptation.origin) : null;

  return (
    <HomeCard testID="adaptation-card" title="Workout adaptation">
      <StatusChip label={statusLabel} variant="warning" />

      {originLabel ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>Origin: {originLabel}</Text>
      ) : null}

      {(adaptation.unresolvedCount ?? 0) > 0 ? (
        <Text style={[styles.meta, { color: theme.colors.text }]}>
          {adaptation.unresolvedCount} item(s) need review
        </Text>
      ) : null}

      <PrimaryButton
        label="Review Adaptation"
        onPress={() => void handleReview()}
        disabled={loading}
      />
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  meta: {
    fontSize: 14,
  },
});
