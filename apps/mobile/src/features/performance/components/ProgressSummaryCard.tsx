import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { ProgressComposition } from '@/src/features/performance/models/progressComposition';

interface ProgressSummaryCardProps {
  progress: ProgressComposition;
}

function statusLabel(status: ProgressComposition['overall']): string {
  switch (status) {
    case 'READY':
      return 'Ready';
    case 'INSUFFICIENT':
      return 'Not enough yet';
    default:
      return 'No history';
  }
}

export function ProgressSummaryCard({ progress }: ProgressSummaryCardProps) {
  const theme = useAppTheme();

  return (
    <HomeCard testID="progress-summary-card" eyebrow="Progress" title="Athlete progress" dense>
      <Text
        style={[styles.headline, { color: theme.colors.text }]}
        accessibilityRole="text"
        accessibilityLabel={progress.headline}>
        {progress.headline}
      </Text>
      <View style={styles.rows}>
        <Text style={[styles.row, { color: theme.colors.textMuted }]}>
          Sessions · {progress.consistency.count} · {statusLabel(progress.consistency.status)}
        </Text>
        <Text style={[styles.row, { color: theme.colors.textMuted }]}>
          Rated effort · {progress.effort.count} · {statusLabel(progress.effort.status)}
        </Text>
        <Text style={[styles.row, { color: theme.colors.textMuted }]}>
          Personal records · {progress.performance.count} · {statusLabel(progress.performance.status)}
        </Text>
        <Text style={[styles.row, { color: theme.colors.textMuted }]}>
          Recovery check-ins · {progress.recovery.count} · {statusLabel(progress.recovery.status)}
        </Text>
        <Text style={[styles.row, { color: theme.colors.textMuted }]}>
          Weekly load points · {progress.load.count} · {statusLabel(progress.load.status)}
        </Text>
      </View>
      {!progress.canShowLoadSeries ? (
        <Text style={[styles.note, { color: theme.colors.textMuted }]}>
          Load charts stay hidden until at least three weekly summaries exist.
        </Text>
      ) : null}
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  headline: {
    fontSize: 15,
    fontWeight: '600',
    lineHeight: 21,
  },
  rows: {
    gap: 4,
  },
  row: {
    fontSize: 13,
    lineHeight: 18,
  },
  note: {
    fontSize: 12,
    lineHeight: 17,
  },
});
