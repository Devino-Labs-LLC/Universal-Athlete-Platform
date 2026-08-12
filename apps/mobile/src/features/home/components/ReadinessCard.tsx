import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { StatusBadge } from '@/src/core/components/Surface';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { readinessBandLabel } from '@/src/features/home/models/todayLabels';
import { TrainingTodayDashboard } from '@/src/features/training/schemas';

interface ReadinessCardProps {
  readiness: TrainingTodayDashboard['readiness'];
}

function bandTone(band: string | null | undefined): 'success' | 'warning' | 'danger' | 'default' {
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

function bandFill(band: string | null | undefined, theme: ReturnType<typeof useAppTheme>['colors']) {
  switch (band) {
    case 'HIGH':
      return theme.success;
    case 'MODERATE':
      return theme.warning;
    case 'LOW':
      return theme.danger;
    default:
      return theme.accentCyan;
  }
}

export function ReadinessCard({ readiness }: ReadinessCardProps) {
  const theme = useAppTheme();

  if (!readiness.readinessPresent) {
    return (
      <HomeCard testID="readiness-card" eyebrow="Athlete state" title="Readiness">
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
    <HomeCard testID="readiness-card" eyebrow="Athlete state" title="Readiness">
      <StatusBadge
        testID="readiness-band-chip"
        label={bandLabel}
        tone={bandTone(readiness.readinessBand)}
      />

      {scorePercent != null ? (
        <View style={styles.scoreSection}>
          <Text
            style={[
              styles.scoreValue,
              {
                color: theme.colors.text,
                fontSize: theme.typography.metric,
              },
            ]}>
            {Math.round(scorePercent)}
          </Text>
          <Text style={[styles.scoreCaption, { color: theme.colors.textMuted }]}>Score</Text>
          <View style={[styles.scoreTrack, { backgroundColor: theme.colors.surfaceMuted }]}>
            <View
              testID="readiness-score-bar"
              style={[
                styles.scoreFill,
                {
                  backgroundColor: bandFill(readiness.readinessBand, theme.colors),
                  width: `${scorePercent}%`,
                },
              ]}
            />
          </View>
        </View>
      ) : (
        <Text style={[styles.body, { color: theme.colors.textMuted }]}>Score unavailable</Text>
      )}

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
    gap: 4,
  },
  scoreValue: {
    fontWeight: '700',
  },
  scoreCaption: {
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  scoreTrack: {
    height: 8,
    borderRadius: 4,
    overflow: 'hidden',
    marginTop: 4,
  },
  scoreFill: {
    height: '100%',
    borderRadius: 4,
  },
  meta: {
    fontSize: 13,
  },
});
