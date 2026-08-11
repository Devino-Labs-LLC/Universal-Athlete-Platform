import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { WeeklyTrainingLoadSummary } from '@/src/features/performance/models/performanceSchemas';
import {
  formatRatedUnratedSummary,
  formatWeeklyLoadSummary,
} from '@/src/features/performance/utils/formatLoadMetrics';
import { formatDateDisplay } from '@/src/features/home/utils/formatDateDisplay';

interface LoadSnapshotCardProps {
  summaries: WeeklyTrainingLoadSummary[];
  loading?: boolean;
}

export function LoadSnapshotCard({ summaries, loading }: LoadSnapshotCardProps) {
  const theme = useAppTheme();
  const latest = summaries[0];

  return (
    <HomeCard testID="load-snapshot-card" title="Training load (28 days)">
      {!latest ? (
        <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
          {loading ? 'Loading training load…' : 'No training load recorded in the last 28 days.'}
        </Text>
      ) : (
        <View style={styles.content}>
          <Text style={[styles.period, { color: theme.colors.text }]}>
            {formatDateDisplay(latest.weekStartDate)} – {formatDateDisplay(latest.weekEndDate)}
          </Text>
          <Text style={[styles.meta, { color: theme.colors.textMuted }]}>
            {formatRatedUnratedSummary(latest)}
          </Text>
          {formatWeeklyLoadSummary(latest).map((line) => (
            <Text key={line} style={[styles.line, { color: theme.colors.text }]}>
              {line}
            </Text>
          ))}
        </View>
      )}
      <PrimaryButton
        label="Load history"
        onPress={() => router.push('/(tabs)/performance/load')}
      />
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  content: {
    gap: 4,
  },
  period: {
    fontSize: 15,
    fontWeight: '600',
  },
  meta: {
    fontSize: 12,
  },
  line: {
    fontSize: 14,
  },
  empty: {
    fontSize: 14,
  },
});
