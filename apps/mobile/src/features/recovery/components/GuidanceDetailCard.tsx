import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  adjustmentTypeLabel,
  readinessBandLabel,
  recommendationActionLabel,
} from '@/src/features/home/models/todayLabels';
import { readinessDimensionLabel } from '@/src/features/recovery/models/recoveryLabels';
import { DailyTrainingRecommendation } from '@/src/features/recovery/models/recoverySchemas';

interface GuidanceDetailCardProps {
  recommendation: DailyTrainingRecommendation;
  onReviewAdaptation?: () => void;
  adaptationPending?: boolean;
}

export function GuidanceDetailCard({
  recommendation,
  onReviewAdaptation,
  adaptationPending = false,
}: GuidanceDetailCardProps) {
  const theme = useAppTheme();
  const actionLabel = recommendationActionLabel(recommendation.overallAction);
  const showAdaptationCta =
    recommendation.overallAction === 'MODIFY_SESSION' &&
    (recommendation.scheduledOccurrences?.length ?? 0) > 0 &&
    onReviewAdaptation;

  return (
    <HomeCard testID="guidance-detail-card" title="Training guidance">
      <StatusChip label={actionLabel} variant="info" />

      {recommendation.recommendationStatus ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Status: {recommendation.recommendationStatus.replace(/_/g, ' ').toLowerCase()}
        </Text>
      ) : null}

      {recommendation.readinessBand ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Readiness: {readinessBandLabel(recommendation.readinessBand)}
          {recommendation.readinessScore != null
            ? ` (${Math.round(Number(recommendation.readinessScore))})`
            : ''}
        </Text>
      ) : null}

      {recommendation.limitingDimensions && recommendation.limitingDimensions.length > 0 ? (
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textMuted }]}>
            Influenced by
          </Text>
          {recommendation.limitingDimensions.map((dim) => (
            <Text key={dim} style={[styles.item, { color: theme.colors.text }]}>
              • {readinessDimensionLabel(dim)}
            </Text>
          ))}
        </View>
      ) : null}

      {recommendation.adjustments.length > 0 ? (
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textMuted }]}>
            Suggested adjustments
          </Text>
          {recommendation.adjustments
            .slice()
            .sort((a, b) => a.orderIndex - b.orderIndex)
            .map((adjustment) => (
              <View key={adjustment.adjustmentId} style={styles.adjustment}>
                <Text style={[styles.item, { color: theme.colors.text }]}>
                  • {adjustmentTypeLabel(adjustment.type)}
                </Text>
                {adjustment.explanationKey ? (
                  <Text style={[styles.explanation, { color: theme.colors.textMuted }]}>
                    {adjustment.explanationKey.replace(/_/g, ' ').toLowerCase()}
                  </Text>
                ) : null}
              </View>
            ))}
        </View>
      ) : (
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          No specific adjustments suggested. Proceed based on how you feel.
        </Text>
      )}

      {showAdaptationCta ? (
        <PrimaryButton
          testID="review-workout-adaptation-cta"
          label="Review Workout Adaptation"
          onPress={onReviewAdaptation}
          disabled={adaptationPending}
        />
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  meta: {
    fontSize: 13,
  },
  body: {
    fontSize: 14,
  },
  section: {
    gap: 4,
  },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.3,
  },
  item: {
    fontSize: 14,
  },
  adjustment: {
    gap: 2,
  },
  explanation: {
    fontSize: 12,
    marginLeft: 12,
  },
});
