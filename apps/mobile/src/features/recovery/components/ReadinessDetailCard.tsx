import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { CompactInfoRow, ScoreRing, SectionHeader } from '@/src/core/components/Surface';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { readinessExplanationLines } from '@/src/features/home/models/readinessInsight';
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
  return (
    <CompactInfoRow
      testID={`readiness-dimension-${dimensionType}`}
      label={readinessDimensionLabel(dimensionType)}
      value={
        available
          ? comparisonBand
            ? comparisonBand.replace(/_/g, ' ').toLowerCase()
            : 'Contributing'
          : 'Not available'
      }
    />
  );
}

interface ReadinessDetailCardProps {
  assessment: DailyReadinessAssessment;
}

export function ReadinessDetailCard({ assessment }: ReadinessDetailCardProps) {
  const theme = useAppTheme();
  const score =
    assessment.readinessScore != null && !Number.isNaN(Number(assessment.readinessScore))
      ? Number(assessment.readinessScore)
      : null;
  const explanations = readinessExplanationLines({
    readinessBand: assessment.readinessBand,
    dataSufficiency: assessment.dataSufficiency,
    limitingDimensions: assessment.limitingDimensions,
  });

  return (
    <HomeCard testID="readiness-detail-card" eyebrow="Athlete state" title="Readiness">
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

      <ScoreRing score={score} label="Score" />

      {explanations.map((line) => (
        <Text key={line} style={[styles.item, { color: theme.colors.textMuted }]}>
          {line}
        </Text>
      ))}

      {assessment.limitingDimensions.length > 0 ? (
        <View style={styles.section}>
          <SectionHeader title="Limiting dimensions" />
          {assessment.limitingDimensions.map((dim) => (
            <Text key={dim} style={[styles.item, { color: theme.colors.text }]}>
              • {readinessDimensionLabel(dim)}
            </Text>
          ))}
        </View>
      ) : null}

      {assessment.strongestDimensions.length > 0 ? (
        <View style={styles.section}>
          <SectionHeader title="Strongest dimensions" />
          {assessment.strongestDimensions.map((dim) => (
            <Text key={dim} style={[styles.item, { color: theme.colors.text }]}>
              • {readinessDimensionLabel(dim)}
            </Text>
          ))}
        </View>
      ) : null}

      {assessment.contributions.length > 0 ? (
        <View style={styles.section}>
          <SectionHeader title="Contributions" />
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
  section: {
    gap: 4,
  },
  item: {
    fontSize: 14,
  },
});
