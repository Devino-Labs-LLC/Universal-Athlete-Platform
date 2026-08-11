import { Pressable, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { personalRecordTypeLabel } from '@/src/features/home/models/todayLabels';
import { formatPersonalRecord } from '@/src/features/performance/utils/formatPersonalRecord';
import { TrainingDashboardPersonalRecord } from '@/src/features/training/schemas';

interface RecentPerformanceCardProps {
  records: TrainingDashboardPersonalRecord[];
}

function resolveExercisePerformanceKey(
  record: TrainingDashboardPersonalRecord,
): string | undefined {
  const passthrough = record as TrainingDashboardPersonalRecord & {
    exercisePerformanceKey?: string;
  };
  return passthrough.exercisePerformanceKey;
}

function navigateToRecord(record: TrainingDashboardPersonalRecord) {
  const key = resolveExercisePerformanceKey(record);
  if (key) {
    router.push(`/(tabs)/performance/exercises/${key}`);
    return;
  }
  router.push('/(tabs)/performance/records');
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
          const formatted = formatPersonalRecord({
            id: record.personalRecordId,
            exercisePerformanceKey: resolveExercisePerformanceKey(record) ?? '',
            exerciseDefinitionId: '',
            recordType: record.recordType,
            recordQualifier: record.recordQualifier,
            exerciseName: record.exerciseName,
            normalizedValue: record.normalizedValue,
            normalizedUnit: record.normalizedUnit,
            measuredValue: undefined,
            measuredUnit: undefined,
            achievedAt: record.achievedAt,
            scheduledDate: record.scheduledDate,
          });

          return (
            <Pressable
              key={record.personalRecordId}
              testID={`recent-performance-row-${record.personalRecordId}`}
              onPress={() => navigateToRecord(record)}
              style={({ pressed }) => [styles.item, pressed && styles.pressed]}>
              <Text style={[styles.exercise, { color: theme.colors.text }]}>
                {record.exerciseName}
              </Text>
              <Text style={[styles.detail, { color: theme.colors.textMuted }]}>
                {personalRecordTypeLabel(record.recordType)}
                {record.recordQualifier ? ` · ${record.recordQualifier}` : ''}
                {formatted ? ` · ${formatted}` : ''}
              </Text>
            </Pressable>
          );
        })}
      </View>
      <Pressable
        testID="recent-performance-view-all"
        onPress={() => router.push('/(tabs)/performance/records')}>
        <Text style={[styles.viewAll, { color: theme.colors.primary }]}>View all</Text>
      </Pressable>
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
  pressed: {
    opacity: 0.7,
  },
  exercise: {
    fontSize: 15,
    fontWeight: '600',
  },
  detail: {
    fontSize: 13,
  },
  viewAll: {
    fontSize: 14,
    fontWeight: '600',
    marginTop: 4,
  },
});
