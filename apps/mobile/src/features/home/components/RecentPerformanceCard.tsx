import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { personalRecordTypeLabel } from '@/src/features/home/models/todayLabels';
import { formatDecimal } from '@/src/features/home/utils/formatMetrics';
import { TrainingDashboardPersonalRecord } from '@/src/features/training/schemas';

interface RecentPerformanceCardProps {
  records: TrainingDashboardPersonalRecord[];
}

export function RecentPerformanceCard({ records }: RecentPerformanceCardProps) {
  const theme = useAppTheme();

  if (records.length === 0) {
    return null;
  }

  const visible = records.slice(0, 3);

  return (
    <HomeCard testID="recent-performance-card" title="Recent performance">
      <View style={styles.list}>
        {visible.map((record) => {
          const value =
            record.normalizedValue != null
              ? `${formatDecimal(record.normalizedValue)}${record.normalizedUnit ? ` ${record.normalizedUnit}` : ''}`
              : null;

          return (
            <View key={record.personalRecordId} style={styles.item}>
              <Text style={[styles.exercise, { color: theme.colors.text }]}>
                {record.exerciseName}
              </Text>
              <Text style={[styles.detail, { color: theme.colors.textMuted }]}>
                {personalRecordTypeLabel(record.recordType)}
                {record.recordQualifier ? ` · ${record.recordQualifier}` : ''}
                {value ? ` · ${value}` : ''}
              </Text>
            </View>
          );
        })}
      </View>
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  list: {
    gap: 10,
  },
  item: {
    gap: 2,
  },
  exercise: {
    fontSize: 15,
    fontWeight: '600',
  },
  detail: {
    fontSize: 13,
  },
});
