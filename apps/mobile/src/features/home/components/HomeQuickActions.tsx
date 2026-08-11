import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ActionFlagButton } from '@/src/features/home/components/ActionFlagButton';
import { TrainingTodayDashboard } from '@/src/features/training/schemas';

interface HomeQuickActionsProps {
  actions: TrainingTodayDashboard['actions'];
  onGenerateDailyState: () => void;
  onCalculateReadiness: () => void;
  onGenerateGuidance: () => void;
  pendingAction?: 'state' | 'readiness' | 'guidance' | null;
}

export function HomeQuickActions({
  actions,
  onGenerateDailyState,
  onCalculateReadiness,
  onGenerateGuidance,
  pendingAction = null,
}: HomeQuickActionsProps) {
  const theme = useAppTheme();

  if (!actions) {
    return null;
  }

  const items: Array<{
    key: string;
    label: string;
    allowed: boolean;
    onPress: () => void;
    loading?: boolean;
  }> = [];

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
      label: 'Generate Training Guidance',
      allowed: true,
      onPress: onGenerateGuidance,
      loading: pendingAction === 'guidance',
    });
  }

  if (actions.canCreateRecoveryCheckIn.allowed || actions.canUpdateRecoveryCheckIn.allowed) {
    items.push({
      key: 'check-in',
      label: actions.canCreateRecoveryCheckIn.allowed ? 'Check In' : 'Update Check In',
      allowed: true,
      onPress: () => router.push('/(tabs)/recovery/check-in'),
    });
  }

  if (actions.canContinueWorkout.allowed) {
    items.push({
      key: 'continue',
      label: 'Continue Workout',
      allowed: true,
      onPress: () => router.push('/(tabs)/training'),
    });
  } else if (actions.canStartWorkout.allowed) {
    items.push({
      key: 'start',
      label: 'Start Workout',
      allowed: true,
      onPress: () => router.push('/(tabs)/training'),
    });
  }

  if (actions.canGenerateAdaptationProposal.allowed) {
    items.push({
      key: 'adaptation',
      label: 'Review Adaptation',
      allowed: true,
      onPress: () => router.push('/(tabs)/training'),
    });
  }

  if (items.length === 0) {
    return null;
  }

  return (
    <View testID="home-quick-actions">
      <Text style={[styles.title, { color: theme.colors.textMuted }]}>Quick actions</Text>
      <View style={styles.grid}>
        {items.map((item) => (
          <ActionFlagButton
            key={item.key}
            testID={`quick-action-${item.key}`}
            label={item.label}
            onPress={item.onPress}
            disabled={!item.allowed}
            loading={item.loading}
          />
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.4,
  },
  grid: {
    gap: 8,
  },
});
