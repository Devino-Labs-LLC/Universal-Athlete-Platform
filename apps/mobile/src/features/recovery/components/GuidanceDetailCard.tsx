import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { CompactInfoRow, SectionHeader } from '@/src/core/components/Surface';
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

  const readinessScore =
    recommendation.readinessScore != null && !Number.isNaN(Number(recommendation.readinessScore))
      ? Math.round(Number(recommendation.readinessScore))
      : null;

  return (
    <HomeCard testID="guidance-detail-card" eyebrow="Guidance" title="Training guidance">
      <StatusChip label={actionLabel} variant="info" />

      {recommendation.recommendationStatus ? (
        <CompactInfoRow
          label="Status"
          value={recommendation.recommendationStatus.replace(/_/g, ' ').toLowerCase()}
        />
      ) : null}

      {recommendation.readinessBand ? (
        <CompactInfoRow
          label="Readiness"
          value={`${readinessBandLabel(recommendation.readinessBand)}${
            readinessScore != null ? ` (${readinessScore})` : ''
          }`}
        />
      ) : null}

      {recommendation.limitingDimensions && recommendation.limitingDimensions.length > 0 ? (
        <View style={styles.section}>
          <SectionHeader title="Influenced by" />
          {recommendation.limitingDimensions.map((dim) => (
            <Text key={dim} style={[styles.item, { color: theme.colors.text }]}>
              • {readinessDimensionLabel(dim)}
            </Text>
          ))}
        </View>
      ) : null}

      {recommendation.adjustments.length > 0 ? (
        <View style={styles.section}>
          <SectionHeader title="Suggested adjustments" />
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
          loading={adaptationPending}
        />
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  body: {
    fontSize: 14,
  },
  section: {
    gap: 4,
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
