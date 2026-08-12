import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ScoreRing } from '@/src/core/components/ScoreRing';
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

function ringTone(
  band: string | null | undefined,
): 'lime' | 'warning' | 'danger' | 'cyan' {
  switch (band) {
    case 'HIGH':
      return 'lime';
    case 'MODERATE':
      return 'warning';
    case 'LOW':
      return 'danger';
    default:
      return 'cyan';
  }
}

function parseScore(raw: unknown): number | null {
  if (raw == null) return null;
  const n = typeof raw === 'number' ? raw : Number(raw);
  if (Number.isNaN(n)) return null;
  return n;
}

/** Dominant athlete-state hero with ScoreRing. Null score → empty ring + —, never zero. */
export function ReadinessCard({ readiness }: ReadinessCardProps) {
  const theme = useAppTheme();
  const present = readiness.readinessPresent;
  const score = present ? parseScore(readiness.readinessScore) : null;
  const bandLabel = present ? readinessBandLabel(readiness.readinessBand) : 'Not assessed';
  const limiting = present
    ? (readiness.limitingDimensions ?? []).slice(0, 3)
    : [];

  return (
    <HomeCard
      testID="readiness-card"
      eyebrow="Athlete state"
      title="Readiness"
      dense>
      <View style={styles.heroRow}>
        <ScoreRing
          testID="readiness-score-ring"
          score={score}
          label="Score"
          size="lg"
          tone={present ? ringTone(readiness.readinessBand) : 'cyan'}
        />
        <View style={styles.heroCopy}>
          <StatusBadge
            testID="readiness-band-chip"
            label={bandLabel}
            tone={present ? bandTone(readiness.readinessBand) : 'default'}
          />
          <Text
            style={[
              styles.statusLine,
              { color: theme.colors.text },
            ]}>
            {present
              ? score != null
                ? `Readiness ${Math.round(Math.min(100, Math.max(0, score)))}`
                : 'Readiness recorded without a numeric score'
              : 'Readiness has not been calculated yet'}
          </Text>
          {limiting.length > 0 ? (
            <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
              Limiting: {limiting.join(', ')}
            </Text>
          ) : present ? (
            <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
              No limiting dimensions flagged
            </Text>
          ) : (
            <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
              Calculate readiness after your recovery check-in
            </Text>
          )}
        </View>
      </View>
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  heroRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  heroCopy: {
    flex: 1,
    gap: 8,
    minWidth: 0,
  },
  statusLine: {
    fontSize: 17,
    fontWeight: '700',
    lineHeight: 22,
  },
  meta: {
    fontSize: 13,
    lineHeight: 18,
  },
});
