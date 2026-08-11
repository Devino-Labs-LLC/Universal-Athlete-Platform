import { StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useMemo } from 'react';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { HomeCard } from '@/src/features/home/components/HomeCard';
import { PersonalRecordCard } from '@/src/features/performance/components/PersonalRecordCard';
import { usePersonalRecords } from '@/src/features/performance/hooks/usePersonalRecords';
import { PersonalRecord } from '@/src/features/performance/models/performanceSchemas';
import { performanceErrorMessage } from '@/src/features/performance/utils/performanceErrors';

function groupRecordsByExercise(records: PersonalRecord[]): Map<string, PersonalRecord[]> {
  const grouped = new Map<string, PersonalRecord[]>();
  for (const record of records) {
    const key = record.exercisePerformanceKey;
    const existing = grouped.get(key) ?? [];
    existing.push(record);
    grouped.set(key, existing);
  }
  return grouped;
}

export function PersonalRecordsScreen() {
  const theme = useAppTheme();
  const recordsQuery = usePersonalRecords();
  const records = recordsQuery.data ?? [];
  const grouped = useMemo(() => groupRecordsByExercise(records), [records]);
  const exerciseKeys = Array.from(grouped.keys());

  if (recordsQuery.isLoading && !recordsQuery.data) {
    return <LoadingView message="Loading personal records…" />;
  }

  if (recordsQuery.isError && !recordsQuery.data) {
    const message = isApiError(recordsQuery.error)
      ? recordsQuery.error.message
      : performanceErrorMessage(recordsQuery.error);
    return <ErrorView message={message} onRetry={() => recordsQuery.refetch()} />;
  }

  return (
    <Screen
      scroll
      testID="personal-records-screen"
      refreshing={recordsQuery.isFetching}
      onRefresh={() => recordsQuery.refetch()}>
      {exerciseKeys.length === 0 ? (
        <Text style={[styles.empty, { color: theme.colors.textMuted }]}>
          No personal records recorded yet.
        </Text>
      ) : (
        exerciseKeys.map((exerciseKey) => {
          const exerciseRecords = grouped.get(exerciseKey) ?? [];
          const exerciseName = exerciseRecords[0]?.exerciseName ?? 'Exercise';

          return (
            <HomeCard
              key={exerciseKey}
              testID={`exercise-group-${exerciseKey}`}
              title={exerciseName}>
              <View style={styles.list}>
                {exerciseRecords.map((record) => (
                  <PersonalRecordCard
                    key={record.id}
                    record={record}
                    onPress={() =>
                      router.push(`/(tabs)/performance/exercises/${exerciseKey}`)
                    }
                  />
                ))}
              </View>
            </HomeCard>
          );
        })
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  empty: {
    fontSize: 15,
  },
  list: {
    gap: 12,
  },
});
