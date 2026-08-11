import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { PersonalRecordCard } from '@/src/features/performance/components/PersonalRecordCard';
import { PersonalRecord } from '@/src/features/performance/models/performanceSchemas';

interface RecentRecordsSectionProps {
  records: PersonalRecord[];
  loading?: boolean;
}

function navigateToExercise(key: string) {
  router.push(`/(tabs)/performance/exercises/${key}`);
}

export function RecentRecordsSection({ records, loading }: RecentRecordsSectionProps) {
  const theme = useAppTheme();

  return (
    <HomeCard testID="recent-records-section" title="Recent personal records">
      {records.length === 0 ? (
        <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
          {loading ? 'Loading records…' : 'No personal records in the last 30 days.'}
        </Text>
      ) : (
        <View style={styles.list}>
          {records.map((record) => (
            <PersonalRecordCard
              key={record.id}
              record={record}
              onPress={() => navigateToExercise(record.exercisePerformanceKey)}
            />
          ))}
        </View>
      )}
      <PrimaryButton
        label="View all records"
        onPress={() => router.push('/(tabs)/performance/records')}
      />
    </HomeCard>
  );
}

const styles = StyleSheet.create({
  list: {
    gap: 12,
  },
  empty: {
    fontSize: 14,
  },
});
