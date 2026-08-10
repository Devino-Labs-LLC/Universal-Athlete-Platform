import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import {
  formatDecimal,
  formatDistance,
  formatDurationSeconds,
  formatVolumeKg,
} from '@/src/features/home/utils/formatMetrics';
import { TrainingLoadSummary } from '@/src/features/training/execution/models/executionSchemas';

interface CompletionSummaryCardProps {
  load: TrainingLoadSummary | null | undefined;
  isLoading?: boolean;
}

export function CompletionSummaryCard({ load, isLoading }: CompletionSummaryCardProps) {
  const theme = useAppTheme();

  if (isLoading) {
    return (
      <HomeCard title="Training load" testID="completion-summary-loading">
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>Calculating load…</Text>
      </HomeCard>
    );
  }

  if (!load) {
    return (
      <HomeCard title="Training load" testID="completion-summary-empty">
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Load summary is not available yet.
        </Text>
      </HomeCard>
    );
  }

  return (
    <HomeCard title="Training load" testID="completion-summary-card">
      {load.sessionRpe != null ? (
        <MetricRow label="Session RPE" value={formatDecimal(load.sessionRpe, 1)} theme={theme} />
      ) : null}
      {load.sessionDurationMinutes != null ? (
        <MetricRow
          label="Duration"
          value={`${load.sessionDurationMinutes} min`}
          theme={theme}
        />
      ) : null}
      {load.sessionRpeLoad != null ? (
        <MetricRow label="RPE load" value={formatDecimal(load.sessionRpeLoad, 0)} theme={theme} />
      ) : null}
      {load.completedSetCount != null ? (
        <MetricRow label="Completed sets" value={String(load.completedSetCount)} theme={theme} />
      ) : null}
      {load.skippedSetCount != null && load.skippedSetCount > 0 ? (
        <MetricRow label="Skipped sets" value={String(load.skippedSetCount)} theme={theme} />
      ) : null}
      {load.completedRepetitionCount != null ? (
        <MetricRow
          label="Repetitions"
          value={String(load.completedRepetitionCount)}
          theme={theme}
        />
      ) : null}
      {load.totalVolumeKilograms != null ? (
        <MetricRow
          label="Volume"
          value={formatVolumeKg(load.totalVolumeKilograms)}
          theme={theme}
        />
      ) : null}
      {load.totalDurationSeconds != null ? (
        <MetricRow
          label="Work duration"
          value={formatDurationSeconds(load.totalDurationSeconds)}
          theme={theme}
        />
      ) : null}
      {load.totalDistanceMeters != null ? (
        <MetricRow
          label="Distance"
          value={formatDistance(load.totalDistanceMeters)}
          theme={theme}
        />
      ) : null}
    </HomeCard>
  );
}

function MetricRow({
  label,
  value,
  theme,
}: {
  label: string;
  value: string;
  theme: ReturnType<typeof useAppTheme>;
}) {
  return (
    <View style={styles.row}>
      <Text style={[styles.label, { color: theme.colors.textMuted }]}>{label}</Text>
      <Text style={[styles.value, { color: theme.colors.text }]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  meta: {
    fontSize: 14,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
  },
  label: {
    fontSize: 14,
  },
  value: {
    fontSize: 14,
    fontWeight: '600',
  },
});
