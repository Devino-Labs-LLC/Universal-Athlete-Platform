import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ActionFlagButton } from '@/src/features/home/components/ActionFlagButton';
import {
  TrainingDashboardOccurrence,
  TrainingTodayDashboard,
} from '@/src/features/training/schemas';
import { navigateHomeWorkoutAction } from '@/src/features/training/utils/trainingNavigation';

interface HomeQuickActionsProps {
  actions: TrainingTodayDashboard['actions'];
  primaryOccurrence?: TrainingDashboardOccurrence | null;
  onGenerateDailyState: () => void;
  onCalculateReadiness: () => void;
  onGenerateGuidance: () => void;
  onGenerateAdaptation?: () => void;
  pendingAction?: 'state' | 'readiness' | 'guidance' | 'adaptation' | null;
}

export function HomeQuickActions({
  actions,
  primaryOccurrence,
  onGenerateDailyState,
  onCalculateReadiness,
  onGenerateGuidance,
  onGenerateAdaptation,
  pendingAction = null,
}: HomeQuickActionsProps) {
  const theme = useAppTheme();

  if (!actions) {
    return null;
  }

  const items: {
    key: string;
    label: string;
    allowed: boolean;
    onPress: () => void;
    loading?: boolean;
  }[] = [];

  if (actions.canGenerateAthleteStateSnapshot.allowed) {
    items.push({
      key: 'state',
      label: 'Generate Daily State',
      allowed: true,
      onPress: onGenerateDailyState,
      loading: pendingAction === 'state',
    });
  }

  if (actions.canGenerateReadinessAssessment.allowed) {
    items.push({
      key: 'readiness',
      label: 'Calculate Readiness',
      allowed: true,
      onPress: onCalculateReadiness,
      loading: pendingAction === 'readiness',
    });
  }

  if (actions.canGenerateTrainingRecommendation.allowed) {
    items.push({
      key: 'guidance',
      label: 'Generate Guidance',
      allowed: true,
      onPress: onGenerateGuidance,
      loading: pendingAction === 'guidance',
    });
  }

  if (actions.canCreateRecoveryCheckIn.allowed || actions.canUpdateRecoveryCheckIn.allowed) {
    items.push({
      key: 'check-in',
      label: actions.canCreateRecoveryCheckIn.allowed ? 'Check in' : 'Update check-in',
      allowed: true,
      onPress: () => router.push('/(tabs)/recovery/check-in'),
    });
  }

  if (actions.canContinueWorkout.allowed) {
    items.push({
      key: 'continue',
      label: 'Continue Workout',
      allowed: true,
      onPress: () => navigateHomeWorkoutAction(primaryOccurrence),
    });
  } else if (actions.canStartWorkout.allowed) {
    items.push({
      key: 'start',
      label: 'Start Workout',
      allowed: true,
      onPress: () => navigateHomeWorkoutAction(primaryOccurrence),
    });
  }

  if (actions.canGenerateAdaptationProposal.allowed) {
    items.push({
      key: 'adaptation',
      label: primaryOccurrence ? 'Find Alternatives' : 'Review Adaptation',
      allowed: true,
      onPress: () => onGenerateAdaptation?.(),
      loading: pendingAction === 'adaptation',
    });
  }

  if (items.length === 0) {
    return null;
  }

  return (
    <View testID="home-quick-actions" style={styles.wrap}>
      <Text style={[styles.title, { color: theme.colors.textMuted }]}>Quick actions</Text>
      <View style={styles.grid}>
        {items.map((item) => (
          <View key={item.key} style={styles.cell}>
            <ActionFlagButton
              testID={`quick-action-${item.key}`}
              label={item.label}
              onPress={item.onPress}
              disabled={!item.allowed}
              loading={item.loading}
            />
          </View>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    gap: 8,
  },
  title: {
    fontSize: 11,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  cell: {
    flexGrow: 1,
    flexBasis: '46%',
    minWidth: 140,
  },
});
