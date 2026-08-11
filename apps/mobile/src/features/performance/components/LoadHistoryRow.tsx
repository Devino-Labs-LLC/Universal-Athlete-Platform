import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { StatusChip } from '@/src/features/home/components/StatusChip';
import {
  DailyTrainingLoadSummary,
  WeeklyTrainingLoadSummary,
  WorkoutOccurrenceLoadSummary,
} from '@/src/features/performance/models/performanceSchemas';
import {
  formatDailyLoadSummary,
  formatOccurrenceLoadSummary,
  formatRatedUnratedSummary,
  formatWeeklyLoadSummary,
  isOccurrenceRated,
} from '@/src/features/performance/utils/formatLoadMetrics';
import { formatDateDisplay } from '@/src/features/home/utils/formatDateDisplay';
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
    const lines = formatOccurrenceLoadSummary(occurrence);
    const rated = isOccurrenceRated(occurrence);
    return (
      <View style={styles.row} testID={`load-history-occurrence-${occurrence.workoutOccurrenceId}`}>
        <View style={styles.header}>
          <Text style={[styles.title, { color: theme.colors.text }]}>
            {formatDateDisplay(occurrence.scheduledDate)}
          </Text>
          <StatusChip
            label={rated ? 'Rated' : 'Unrated'}
            variant={rated ? 'success' : 'warning'}
          />
        </View>
        {lines.map((line) => (
          <Text key={line} style={[styles.line, { color: theme.colors.textMuted }]}>
            {line}
          </Text>
        ))}
        <CategoryBreakdownList summaries={occurrence.categorySummaries} />
      </View>
    );
  }

  if (mode === 'DAILY' && daily) {
    const lines = formatDailyLoadSummary(daily);
    return (
      <View style={styles.row} testID={`load-history-daily-${daily.date}`}>
        <View style={styles.header}>
          <Text style={[styles.title, { color: theme.colors.text }]}>
            {formatDateDisplay(daily.date)}
          </Text>
          <Text style={[styles.subtitle, { color: theme.colors.textMuted }]}>
            {formatRatedUnratedSummary(daily)}
          </Text>
        </View>
        {lines.slice(1).map((line) => (
          <Text key={line} style={[styles.line, { color: theme.colors.textMuted }]}>
            {line}
          </Text>
        ))}
        <CategoryBreakdownList summaries={daily.categorySummaries} />
      </View>
    );
  }

  if (mode === 'WEEKLY' && weekly) {
    const lines = formatWeeklyLoadSummary(weekly);
    return (
      <View style={styles.row} testID={`load-history-weekly-${weekly.weekStartDate}`}>
        <View style={styles.header}>
          <Text style={[styles.title, { color: theme.colors.text }]}>
            {formatDateDisplay(weekly.weekStartDate)} – {formatDateDisplay(weekly.weekEndDate)}
          </Text>
          <Text style={[styles.subtitle, { color: theme.colors.textMuted }]}>
            {formatRatedUnratedSummary(weekly)}
          </Text>
        </View>
        {lines.map((line) => (
          <Text key={line} style={[styles.line, { color: theme.colors.textMuted }]}>
            {line}
          </Text>
        ))}
        <CategoryBreakdownList summaries={weekly.categorySummaries} />
      </View>
    );
  }

  return null;
}

const styles = StyleSheet.create({
  row: {
    gap: 4,
    marginBottom: 16,
  },
  header: {
    gap: 2,
  },
  title: {
    fontSize: 15,
    fontWeight: '600',
  },
  subtitle: {
    fontSize: 12,
  },
  line: {
    fontSize: 13,
  },
});
