import { useMemo, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EmptyView } from '@/src/core/components/EmptyView';
import { ErrorView } from '@/src/core/components/ErrorView';
import { LoadingView } from '@/src/core/components/LoadingView';
import { PrimaryButton } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { isApiError } from '@/src/core/api/errors';
import { DateOnly, todayDateOnly } from '@/src/core/date/dateOnly';
import { CalendarWeekStrip } from '@/src/features/training/components/CalendarWeekStrip';
import { WorkoutOccurrenceCard } from '@/src/features/training/components/WorkoutOccurrenceCard';
import { useTrainingCalendar } from '@/src/features/training/hooks/useTrainingCalendar';
import {
  addDays,
  endOfWeek,
  startOfWeek,
} from '@/src/features/training/utils/calendarRange';
import {
  navigateToOccurrenceDetail,
  navigateToOccurrenceLaunch,
} from '@/src/features/training/utils/trainingNavigation';

export function TrainingCalendarScreen() {
  const theme = useAppTheme();
  const today = todayDateOnly();
  const [weekStart, setWeekStart] = useState<DateOnly>(() => startOfWeek(today));
  const [selectedDate, setSelectedDate] = useState<DateOnly>(today);

  const weekEnd = useMemo(() => endOfWeek(weekStart), [weekStart]);
  const calendarQuery = useTrainingCalendar(weekStart, weekEnd);

  const selectedEntries = useMemo(() => {
    return (calendarQuery.data ?? []).filter((entry) => entry.scheduledDate === selectedDate);
  }, [calendarQuery.data, selectedDate]);

  if (calendarQuery.isError && !calendarQuery.data) {
    const message = isApiError(calendarQuery.error)
      ? calendarQuery.error.message
      : 'Failed to load calendar';
    return <ErrorView message={message} onRetry={() => calendarQuery.refetch()} />;
  }

  const showListLoader = calendarQuery.isLoading && !calendarQuery.data;

  return (
    <Screen
      scroll
      testID="training-calendar-screen"
      refreshing={calendarQuery.isFetching}
      onRefresh={() => calendarQuery.refetch()}>
      <View style={styles.weekNav}>
        <PrimaryButton
          label="Previous week"
          onPress={() => {
            const previous = addDays(weekStart, -7);
            setWeekStart(previous);
            setSelectedDate(previous);
          }}
        />
        <PrimaryButton
          label="Next week"
          onPress={() => {
            const next = addDays(weekStart, 7);
            setWeekStart(next);
            setSelectedDate(next);
          }}
        />
      </View>

      <Text style={[styles.range, { color: theme.colors.textMuted }]}>
        {weekStart} – {weekEnd}
      </Text>

      <CalendarWeekStrip
        weekStart={weekStart}
        selectedDate={selectedDate}
        onSelectDate={setSelectedDate}
      />

      <Text style={[styles.sectionTitle, { color: theme.colors.text }]}>
        {selectedDate}
      </Text>

      {showListLoader ? (
        <LoadingView message="Loading workouts…" />
      ) : selectedEntries.length === 0 ? (
        <EmptyView message="No workouts scheduled for this day." />
      ) : (
        selectedEntries.map((entry) => (
          <WorkoutOccurrenceCard
            key={entry.occurrenceId}
            occurrence={entry}
            onPress={() =>
              navigateToOccurrenceDetail(
                entry.trainingPlanId,
                entry.workoutDayId,
                entry.occurrenceId,
              )
            }
            onPrimaryAction={() =>
              navigateToOccurrenceLaunch(
                entry.trainingPlanId,
                entry.workoutDayId,
                entry.occurrenceId,
              )
            }
          />
        ))
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  weekNav: {
    flexDirection: 'row',
    gap: 8,
  },
  range: {
    fontSize: 14,
    textAlign: 'center',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
});
