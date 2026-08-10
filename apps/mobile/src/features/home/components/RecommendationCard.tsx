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
}

export function RecommendationCard({ recommendation }: RecommendationCardProps) {
  const theme = useAppTheme();

  if (!recommendation.recommendationPresent) {
    return (
      <HomeCard testID="recommendation-card" title="Training guidance">
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          No guidance generated for today yet.
        </Text>
      </HomeCard>
    );
  }

  const actionLabel = recommendationActionLabel(recommendation.overallAction);
  const adjustments = (recommendation.adjustmentTypes ?? []).slice(0, 3);

  return (
    <HomeCard testID="recommendation-card" title="Training guidance">
      <StatusChip testID="recommendation-action-chip" label={actionLabel} variant="info" />

      {recommendation.recommendationStatus ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Status: {recommendation.recommendationStatus}
        </Text>
      ) : null}

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
    fontSize: 15,
  },
  meta: {
    fontSize: 13,
  },
  adjustments: {
    gap: 4,
  },
  adjustment: {
    fontSize: 14,
  },
});
