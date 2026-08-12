import { Pressable, StyleSheet, Text } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { formatEnumLabel } from '@/src/features/profile/enumLabels';
import { OverviewPlan } from '@/src/features/training/models/browseSchemas';

interface TrainingPlanCardProps {
  plan: OverviewPlan;
}

export function TrainingPlanCard({ plan }: TrainingPlanCardProps) {
  const theme = useAppTheme();

  const navigateToPlan = () => {
    router.push(`/(tabs)/training/plans/${plan.trainingPlanId}`);
  };

  return (
    <Pressable onPress={navigateToPlan} style={({ pressed }) => [{ opacity: pressed ? 0.85 : 1 }]}>
      <HomeCard testID={`training-plan-card-${plan.trainingPlanId}`} eyebrow="Plan">
        <Text style={[styles.name, { color: theme.colors.text }]} numberOfLines={2}>
          {plan.name}
        </Text>
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          {formatEnumLabel(plan.type)} · {plan.startDate}
          {plan.endDate ? ` – ${plan.endDate}` : ''}
        </Text>
        <StatusChip label={formatEnumLabel(plan.status)} variant="info" />
      </HomeCard>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  name: {
    fontSize: 16,
    fontWeight: '600',
  },
  meta: {
    fontSize: 14,
  },
});
