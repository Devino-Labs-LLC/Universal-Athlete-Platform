import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { RecommendationAdjustment } from '@/src/features/adaptation/models/adaptationSchemas';
import { adjustmentTypeLabel } from '@/src/features/home/models/todayLabels';

interface ContextOnlyAdjustmentsCardProps {
  adjustments: RecommendationAdjustment[];
}

export function ContextOnlyAdjustmentsCard({ adjustments }: ContextOnlyAdjustmentsCardProps) {
  const theme = useAppTheme();

  if (adjustments.length === 0) {
    return null;
  }

  return (
    <HomeCard testID="context-only-adjustments-card" title="Guidance context">
      <Text style={[styles.notice, { color: theme.colors.text }]}>
        These guidance notes are for context only. Applying this adaptation will not change sets,
        reps, load, or duration.
      </Text>
      <View style={styles.list}>
        {adjustments
          .slice()
          .sort((a, b) => a.orderIndex - b.orderIndex)
          .map((adjustment) => (
            <View key={`${adjustment.type}-${adjustment.orderIndex}`} style={styles.item}>
              <Text style={[styles.itemTitle, { color: theme.colors.text }]}>
                {adjustmentTypeLabel(adjustment.type)}
              </Text>
              {adjustment.explanationKey ? (
                <Text style={[styles.explanation, { color: theme.colors.textMuted }]}>
                  {adjustment.explanationKey.replace(/_/g, ' ').toLowerCase()}
                </Text>
              ) : null}
            </View>
          ))}
      </View>
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  notice: {
    fontSize: 14,
    lineHeight: 20,
  },
  list: {
    gap: 10,
  },
  item: {
    gap: 4,
  },
  itemTitle: {
    fontSize: 15,
    fontWeight: '600',
  },
  explanation: {
    fontSize: 13,
  },
});
