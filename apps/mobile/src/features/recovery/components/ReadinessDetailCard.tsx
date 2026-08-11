import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { readinessBandLabel } from '@/src/features/home/models/todayLabels';
import { readinessDimensionLabel } from '@/src/features/recovery/models/recoveryLabels';
import { DailyReadinessAssessment } from '@/src/features/recovery/models/recoverySchemas';

interface ReadinessDimensionRowProps {
  dimensionType: string;
  comparisonBand?: string;
  available?: boolean;
}

export function ReadinessDimensionRow({
  dimensionType,
  comparisonBand,
  available = true,
}: ReadinessDimensionRowProps) {
  const theme = useAppTheme();

  return (
    <View style={styles.row} testID={`readiness-dimension-${dimensionType}`}>
      <Text style={[styles.name, { color: theme.colors.text }]}>
        {readinessDimensionLabel(dimensionType)}
      </Text>
      <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
        {available
          ? comparisonBand
            ? comparisonBand.replace(/_/g, ' ').toLowerCase()
            : 'Contributing'
          : 'Not available'}
      </Text>
    </View>
  );
}

interface ReadinessDetailCardProps {
  assessment: DailyReadinessAssessment;
}

export function ReadinessDetailCard({ assessment }: ReadinessDetailCardProps) {
  const theme = useAppTheme();
  const score =
    assessment.readinessScore != null && !Number.isNaN(Number(assessment.readinessScore))
      ? Math.round(Number(assessment.readinessScore))
      : null;

  return (
    <HomeCard testID="readiness-detail-card" title="Readiness">
      <StatusChip
        label={readinessBandLabel(assessment.readinessBand)}
        variant={
          assessment.readinessBand === 'HIGH'
            ? 'success'
            : assessment.readinessBand === 'LOW'
              ? 'danger'
              : 'warning'
        }
      />

      {score != null ? (
        <Text style={[styles.score, { color: theme.colors.text }]}>{score}</Text>
      ) : null}

      {assessment.limitingDimensions.length > 0 ? (
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textMuted }]}>
            Limiting dimensions
          </Text>
          {assessment.limitingDimensions.map((dim) => (
            <Text key={dim} style={[styles.item, { color: theme.colors.text }]}>
              • {readinessDimensionLabel(dim)}
            </Text>
          ))}
        </View>
      ) : null}

      {assessment.strongestDimensions.length > 0 ? (
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textMuted }]}>
            Strongest dimensions
          </Text>
          {assessment.strongestDimensions.map((dim) => (
            <Text key={dim} style={[styles.item, { color: theme.colors.text }]}>
              • {readinessDimensionLabel(dim)}
            </Text>
          ))}
        </View>
      ) : null}

      {assessment.contributions.length > 0 ? (
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: theme.colors.textMuted }]}>
            Contributions
          </Text>
          {assessment.contributions.map((contribution) => (
            <ReadinessDimensionRow
              key={contribution.dimensionType}
              dimensionType={contribution.dimensionType}
              comparisonBand={contribution.comparisonBand}
              available={contribution.available}
            />
          ))}
        </View>
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  score: {
    fontSize: 32,
    fontWeight: '700',
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
  row: {
    gap: 2,
    paddingVertical: 4,
  },
  name: {
    fontSize: 14,
    fontWeight: '600',
  },
  meta: {
    fontSize: 13,
  },
});
