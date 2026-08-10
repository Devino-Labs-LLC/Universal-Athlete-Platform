import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import { readinessBandLabel } from '@/src/features/home/models/todayLabels';
import { TrainingTodayDashboard } from '@/src/features/training/schemas';

interface ReadinessCardProps {
  readiness: TrainingTodayDashboard['readiness'];
}

function bandVariant(band: string | null | undefined): 'success' | 'warning' | 'danger' | 'default' {
  switch (band) {
    case 'HIGH':
      return 'success';
    case 'MODERATE':
      return 'warning';
    case 'LOW':
      return 'danger';
    default:
      return 'default';
  }
}

export function ReadinessCard({ readiness }: ReadinessCardProps) {
  const theme = useAppTheme();

  if (!readiness.readinessPresent) {
    return (
      <HomeCard testID="readiness-card" title="Readiness">
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>
          Readiness has not been calculated yet.
        </Text>
      </HomeCard>
    );
  }

  const bandLabel = readinessBandLabel(readiness.readinessBand);
  const score =
    readiness.readinessScore != null && !Number.isNaN(readiness.readinessScore)
      ? readiness.readinessScore
      : null;
  const scorePercent = score != null ? Math.min(100, Math.max(0, score)) : null;

  return (
    <HomeCard testID="readiness-card" title="Readiness">
      <StatusChip
        testID="readiness-band-chip"
        label={bandLabel}
        variant={bandVariant(readiness.readinessBand)}
      />

      {scorePercent != null ? (
        <View style={styles.scoreSection}>
          <Text style={[styles.scoreValue, { color: theme.colors.text }]}>
            {Math.round(scorePercent)}
          </Text>
          <View style={[styles.scoreTrack, { backgroundColor: theme.colors.border }]}>
            <View
              testID="readiness-score-bar"
              style={[
                styles.scoreFill,
                {
                  backgroundColor: theme.colors.primary,
                  width: `${scorePercent}%`,
                },
              ]}
            />
          </View>
        </View>
      ) : null}

      {readiness.limitingDimensions && readiness.limitingDimensions.length > 0 ? (
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Limiting: {readiness.limitingDimensions.slice(0, 3).join(', ')}
        </Text>
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  body: {
    fontSize: 15,
  },
  scoreSection: {
    gap: 6,
  },
  scoreValue: {
    fontSize: 28,
    fontWeight: '700',
  },
  scoreTrack: {
    height: 8,
    borderRadius: 4,
    overflow: 'hidden',
  },
  scoreFill: {
    height: '100%',
    borderRadius: 4,
  },
  meta: {
    fontSize: 13,
  },
});
