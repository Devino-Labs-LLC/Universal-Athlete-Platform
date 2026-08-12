import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { MetricTile, Surface } from '@/src/core/components/Surface';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  DailyTrainingLoadSummary,
  WeeklyTrainingLoadSummary,
  WorkoutOccurrenceLoadSummary,
} from '@/src/features/performance/models/performanceSchemas';
import {
  formatLoadVolume,
  formatRatedUnratedSummary,
  formatSessionRpeLoad,
  isOccurrenceRated,
} from '@/src/features/performance/utils/formatLoadMetrics';
import { formatDateDisplay } from '@/src/features/home/utils/formatDateDisplay';
import { formatDecimal, formatDurationSeconds } from '@/src/features/home/utils/formatMetrics';
import { CategoryBreakdownList } from '@/src/features/performance/components/CategoryBreakdownList';

interface LoadHistoryRowProps {
  mode: 'OCCURRENCE' | 'DAILY' | 'WEEKLY';
  occurrence?: WorkoutOccurrenceLoadSummary;
  daily?: DailyTrainingLoadSummary;
  weekly?: WeeklyTrainingLoadSummary;
}

export function LoadHistoryRow({ mode, occurrence, daily, weekly }: LoadHistoryRowProps) {
  const theme = useAppTheme();

  if (mode === 'OCCURRENCE' && occurrence) {
    const rated = isOccurrenceRated(occurrence);
    const volume = formatLoadVolume(occurrence.totalVolumeKilograms);
    const duration =
      occurrence.totalDurationSeconds != null && occurrence.totalDurationSeconds > 0
        ? formatDurationSeconds(occurrence.totalDurationSeconds)
        : null;
    const sessionLoad = formatSessionRpeLoad(occurrence.sessionRpeLoad);
    return (
      <Surface
        elevated
        testID={`load-history-occurrence-${occurrence.workoutOccurrenceId}`}
        style={styles.row}>
        <View style={styles.header}>
          <Text style={[styles.title, { color: theme.colors.text }]}>
            {formatDateDisplay(occurrence.scheduledDate)}
          </Text>
          <StatusChip
            label={rated ? 'Rated' : 'Unrated'}
            variant={rated ? 'success' : 'warning'}
          />
        </View>
        <View style={styles.metrics}>
          <MetricTile label="Volume" value={volume} />
          <MetricTile label="Duration" value={duration} />
          <MetricTile
            label="Session load"
            value={sessionLoad ?? (rated ? null : 'not rated')}
          />
        </View>
        {occurrence.sessionRpe != null ? (
          <Text style={[styles.line, { color: theme.colors.textMuted }]}>
            RPE: {formatDecimal(occurrence.sessionRpe)}
          </Text>
        ) : null}
        <CategoryBreakdownList summaries={occurrence.categorySummaries} />
      </Surface>
    );
  }

  if (mode === 'DAILY' && daily) {
    const volume = formatLoadVolume(daily.totalVolumeKilograms);
    const duration =
      daily.totalDurationSeconds > 0 ? formatDurationSeconds(daily.totalDurationSeconds) : null;
    const sessionLoad = formatSessionRpeLoad(daily.totalSessionRpeLoad);
    return (
      <Surface elevated testID={`load-history-daily-${daily.date}`} style={styles.row}>
        <View style={styles.header}>
          <Text style={[styles.title, { color: theme.colors.text }]}>
            {formatDateDisplay(daily.date)}
          </Text>
          <Text style={[styles.subtitle, { color: theme.colors.textMuted }]}>
            {formatRatedUnratedSummary(daily)}
          </Text>
        </View>
        <View style={styles.metrics}>
          <MetricTile label="Volume" value={volume} />
          <MetricTile label="Duration" value={duration} />
          <MetricTile
            label="Session load"
            value={
              sessionLoad ??
              (daily.unratedOccurrenceCount > 0 ? 'not rated' : null)
            }
          />
        </View>
        <CategoryBreakdownList summaries={daily.categorySummaries} />
      </Surface>
    );
  }

  if (mode === 'WEEKLY' && weekly) {
    const volume = formatLoadVolume(weekly.totalVolumeKilograms);
    const duration =
      weekly.totalDurationSeconds > 0 ? formatDurationSeconds(weekly.totalDurationSeconds) : null;
    const sessionLoad = formatSessionRpeLoad(weekly.totalSessionRpeLoad);
    return (
      <Surface elevated testID={`load-history-weekly-${weekly.weekStartDate}`} style={styles.row}>
        <View style={styles.header}>
          <Text style={[styles.title, { color: theme.colors.text }]}>
            {formatDateDisplay(weekly.weekStartDate)} – {formatDateDisplay(weekly.weekEndDate)}
          </Text>
          <Text style={[styles.subtitle, { color: theme.colors.textMuted }]}>
            {weekly.trainingDays} training days
            {formatRatedUnratedSummary(weekly) ? ` · ${formatRatedUnratedSummary(weekly)}` : ''}
          </Text>
        </View>
        <View style={styles.metrics}>
          <MetricTile label="Volume" value={volume} />
          <MetricTile label="Duration" value={duration} />
          <MetricTile
            label="Session load"
            value={
              sessionLoad ??
              (weekly.unratedOccurrenceCount > 0 ? 'not rated' : null)
            }
          />
        </View>
        <CategoryBreakdownList summaries={weekly.categorySummaries} />
      </Surface>
    );
  }

  return null;
}

const styles = StyleSheet.create({
  row: {
    gap: 8,
    marginBottom: 4,
  },
  header: {
    gap: 4,
  },
  title: {
    fontSize: 15,
    fontWeight: '600',
  },
  subtitle: {
    fontSize: 12,
  },
  metrics: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  line: {
    fontSize: 13,
  },
});
