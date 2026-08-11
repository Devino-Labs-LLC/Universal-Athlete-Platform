import { StyleSheet, Text } from 'react-native';
import { router } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { todayDateOnly } from '@/src/core/date/dateOnly';
import { RecoveryHistoryRow } from '@/src/features/recovery/components/RecoveryHistoryRow';
import { useRecoveryHistory } from '@/src/features/recovery/hooks/useRecoveryHistory';
import { addDays } from '@/src/features/training/utils/calendarRange';

export function RecoveryHistoryScreen() {
  const theme = useAppTheme();
  const today = todayDateOnly();
  const startDate = addDays(today, -29);

  const historyQuery = useRecoveryHistory(startDate, today, true);
  const days = historyQuery.data?.days ?? [];

  if (historyQuery.isLoading) {
    return <LoadingView message="Loading recovery history…" />;
  }

  if (historyQuery.isError) {
    const message = isApiError(historyQuery.error)
      ? historyQuery.error.message
      : 'Failed to load history';
    return <ErrorView message={message} onRetry={() => historyQuery.refetch()} />;
  }

  return (
    <Screen scroll testID="recovery-history-screen">
      <Text style={[styles.title, { color: theme.colors.text }]}>Recovery history</Text>
      <Text style={[styles.subtitle, { color: theme.colors.textMuted }]}>
        Last 30 days
      </Text>

      {days.length === 0 ? (
        <EmptyView message="No check-ins in this period." />
      ) : (
        days.map((day) => (
          <RecoveryHistoryRow
            key={day.date}
            day={day}
            onPress={() => router.push(`/(tabs)/recovery/check-in?date=${day.date}`)}
          />
        ))
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 22,
    fontWeight: '700',
  },
  subtitle: {
    fontSize: 14,
  },
});
