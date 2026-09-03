import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  MISSING_INTELLIGENCE_COPY,
  missingRecommendationStep,
} from '@/src/features/home/models/readinessInsight';
import {
  adjustmentTypeLabel,
  recommendationActionLabel,
} from '@/src/features/home/models/todayLabels';
import { TrainingTodayDashboard } from '@/src/features/training/schemas';

interface RecommendationCardProps {
  recommendation: TrainingTodayDashboard['recommendation'];
  checkInPresent: boolean;
  snapshotPresent: boolean;
  readinessPresent: boolean;
  compact?: boolean;
}

export function RecommendationCard({
  recommendation,
  checkInPresent,
  snapshotPresent,
  readinessPresent,
  compact = false,
}: RecommendationCardProps) {
  const theme = useAppTheme();

  if (!recommendation.recommendationPresent) {
    const absence = missingRecommendationStep({
      checkInPresent,
      snapshotPresent,
      readinessPresent,
    });
    return (
      <HomeCard
        testID="recommendation-card"
        title="Guidance"
        dense={compact}>
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          {MISSING_INTELLIGENCE_COPY[absence]}
        </Text>
      </HomeCard>
    );
  }

  const actionLabel = recommendationActionLabel(recommendation.overallAction);
  const adjustments = (recommendation.adjustmentTypes ?? []).slice(0, compact ? 2 : 3);
  const recommendationId = recommendation.recommendationId;
  const openDetail = recommendationId
    ? () => router.push(`/(tabs)/recovery/guidance/${recommendationId}`)
    : undefined;

  return (
    <HomeCard
      testID="recommendation-card"
      title="Guidance"
      dense={compact}
      onPress={openDetail}
      accessibilityHint={openDetail ? 'Opens guidance details' : undefined}>
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
