import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { CompactInfoRow } from '@/src/core/components/Surface';
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
      <HomeCard eyebrow="Post-session" title="Training load" testID="completion-summary-loading">
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>Calculating load…</Text>
      </HomeCard>
    );
  }

  if (!load) {
    return (
      <HomeCard eyebrow="Post-session" title="Training load" testID="completion-summary-empty">
        <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
          Load summary is not available yet.
        </Text>
      </HomeCard>
    );
  }

  return (
    <HomeCard eyebrow="Post-session" title="Training load" testID="completion-summary-card">
      <View style={styles.rows}>
        {load.sessionRpe != null ? (
          <CompactInfoRow label="Session RPE" value={formatDecimal(load.sessionRpe, 1)} />
        ) : null}
        {load.sessionDurationMinutes != null ? (
          <CompactInfoRow label="Duration" value={`${load.sessionDurationMinutes} min`} />
        ) : null}
        {load.sessionRpeLoad != null ? (
          <CompactInfoRow label="RPE load" value={formatDecimal(load.sessionRpeLoad, 0)} />
        ) : null}
        {load.completedSetCount != null ? (
          <CompactInfoRow label="Completed sets" value={String(load.completedSetCount)} />
        ) : null}
        {load.skippedSetCount != null && load.skippedSetCount > 0 ? (
          <CompactInfoRow label="Skipped sets" value={String(load.skippedSetCount)} />
        ) : null}
        {load.completedRepetitionCount != null ? (
          <CompactInfoRow label="Repetitions" value={String(load.completedRepetitionCount)} />
        ) : null}
        {load.totalVolumeKilograms != null ? (
          <CompactInfoRow label="Volume" value={formatVolumeKg(load.totalVolumeKilograms)} />
        ) : null}
        {load.totalDurationSeconds != null ? (
          <CompactInfoRow
            label="Work duration"
            value={formatDurationSeconds(load.totalDurationSeconds)}
          />
        ) : null}
        {load.totalDistanceMeters != null ? (
          <CompactInfoRow label="Distance" value={formatDistance(load.totalDistanceMeters)} />
        ) : null}
      </View>
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  meta: {
    fontSize: 14,
  },
  rows: {
    gap: 8,
  },
});
