import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { StatusBadge } from '@/src/core/components/Surface';
import { useDerivedStateMutations } from '@/src/features/home/hooks/useDerivedStateMutations';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { TrainingTodayDashboard } from '@/src/features/training/schemas';
import { router } from 'expo-router';

interface InsightsStepListProps {
  date: string;
  overviewCheckInPresent: boolean;
  actions?: TrainingTodayDashboard['actions'];
  athleteState?: TrainingTodayDashboard['athleteState'];
  readinessPresent?: boolean;
  recommendationPresent?: boolean;
}

type StepStatus = 'complete' | 'current' | 'pending';

interface Step {
  key: string;
  title: string;
  status: StepStatus;
  ctaLabel?: string;
  onPress?: () => void;
  loading?: boolean;
}

export function InsightsStepList({
  date,
  overviewCheckInPresent,
  actions,
  athleteState,
  readinessPresent = false,
  recommendationPresent = false,
}: InsightsStepListProps) {
  const theme = useAppTheme();
  const mutations = useDerivedStateMutations(date);

  const checkInComplete = overviewCheckInPresent;
  const stateComplete = athleteState?.snapshotPresent === true;
  const canGenerateState = actions?.canGenerateAthleteStateSnapshot?.allowed === true;
  const canCalculateReadiness = actions?.canGenerateReadinessAssessment?.allowed === true;
  const canGenerateGuidance = actions?.canGenerateTrainingRecommendation?.allowed === true;

  const pendingAction = mutations.athleteStateMutation.isPending
    ? 'state'
    : mutations.readinessMutation.isPending
      ? 'readiness'
      : mutations.recommendationMutation.isPending
        ? 'guidance'
        : null;

  const steps: Step[] = [
    {
      key: 'check-in',
      title: 'Daily check-in',
      status: checkInComplete ? 'complete' : 'current',
      ctaLabel: checkInComplete ? undefined : 'Check In',
      onPress: checkInComplete ? undefined : () => router.push('/(tabs)/recovery/check-in'),
    },
    {
      key: 'state',
      title: 'Daily athlete state',
      status: stateComplete ? 'complete' : checkInComplete ? 'current' : 'pending',
      ctaLabel:
        checkInComplete && canGenerateState ? 'Generate Daily State' : undefined,
      onPress:
        checkInComplete && canGenerateState
          ? () => mutations.athleteStateMutation.mutate()
          : undefined,
      loading: pendingAction === 'state',
    },
    {
      key: 'readiness',
      title: 'Readiness assessment',
      status:
        readinessPresent
          ? 'complete'
          : !checkInComplete || !stateComplete
            ? 'pending'
            : canCalculateReadiness
              ? 'current'
              : 'pending',
      ctaLabel: canCalculateReadiness ? 'Calculate Readiness' : undefined,
      onPress: canCalculateReadiness
        ? () => mutations.readinessMutation.mutate()
        : undefined,
      loading: pendingAction === 'readiness',
    },
    {
      key: 'guidance',
      title: 'Training guidance',
      status:
        recommendationPresent
          ? 'complete'
          : !checkInComplete || !stateComplete || !readinessPresent
            ? 'pending'
            : canGenerateGuidance
              ? 'current'
              : 'pending',
      ctaLabel: canGenerateGuidance ? 'Generate Training Guidance' : undefined,
      onPress: canGenerateGuidance
        ? () => mutations.recommendationMutation.mutate()
        : undefined,
      loading: pendingAction === 'guidance',
    },
  ];

  // Only show CTA on the next incomplete step
  let foundCurrent = false;
  const visibleSteps = steps.map((step) => {
    if (step.status === 'complete') {
      return { ...step, ctaLabel: undefined, onPress: undefined, loading: false };
    }
    if (!foundCurrent && step.ctaLabel) {
      foundCurrent = true;
      return step;
    }
    return { ...step, ctaLabel: undefined, onPress: undefined, loading: false };
  });

  return (
    <HomeCard testID="insights-step-list" eyebrow="Pipeline" title="Insights pipeline">
      {visibleSteps.map((step, index) => (
        <View key={step.key} style={styles.step}>
          <View style={styles.stepHeader}>
            <Text style={[styles.stepNumber, { color: theme.colors.textMuted }]}>
              {index + 1}.
            </Text>
            <Text style={[styles.stepTitle, { color: theme.colors.text }]}>{step.title}</Text>
            <StatusBadge
              label={
                step.status === 'complete'
                  ? 'Done'
                  : step.status === 'current'
                    ? 'Next'
                    : 'Waiting'
              }
              tone={
                step.status === 'complete'
                  ? 'success'
                  : step.status === 'current'
                    ? 'info'
                    : 'default'
              }
            />
          </View>
          {step.ctaLabel && step.onPress ? (
            <PrimaryButton
              label={step.ctaLabel}
              onPress={step.onPress}
              loading={step.loading}
            />
          ) : null}
        </View>
      ))}

      {mutations.errorMessage ? (
        <Text style={[styles.error, { color: theme.colors.danger }]}>{mutations.errorMessage}</Text>
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  step: {
    gap: 8,
  },
  stepHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  stepNumber: {
    fontSize: 14,
    fontWeight: '600',
    width: 20,
  },
  stepTitle: {
    flex: 1,
    fontSize: 14,
    fontWeight: '600',
  },
  error: {
    fontSize: 13,
  },
});
