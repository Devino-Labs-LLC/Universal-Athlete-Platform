import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  adjustmentTypeLabel,
  recommendationActionLabel,
} from '@/src/features/home/models/todayLabels';
import { TrainingTodayDashboard } from '@/src/features/training/schemas';

interface RecommendationCardProps {
  recommendation: TrainingTodayDashboard['recommendation'];
  compact?: boolean;
}

export function RecommendationCard({
  recommendation,
  compact = false,
}: RecommendationCardProps) {
  const theme = useAppTheme();

  if (!recommendation.recommendationPresent) {
    return (
      <HomeCard
        testID="recommendation-card"
        eyebrow={compact ? undefined : undefined}
        title="Guidance"
        dense={compact}>
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          No guidance yet
        </Text>
      </HomeCard>
    );
  }

  const actionLabel = recommendationActionLabel(recommendation.overallAction);
  const adjustments = (recommendation.adjustmentTypes ?? []).slice(0, compact ? 2 : 3);

  return (
    <HomeCard testID="recommendation-card" title="Guidance" dense={compact}>
      <StatusChip testID="recommendation-action-chip" label={actionLabel} variant="info" />

      {adjustments.length > 0 ? (
        <View style={styles.adjustments}>
          {adjustments.map((type) => (
            <Text key={type} style={[styles.adjustment, { color: theme.colors.text }]}>
              • {adjustmentTypeLabel(type)}
            </Text>
          ))}
        </View>
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  body: {
    fontSize: 13,
    lineHeight: 18,
  },
  adjustments: {
    gap: 2,
  },
  adjustment: {
    fontSize: 12,
    lineHeight: 16,
  },
});
