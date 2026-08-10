import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { DateOnly } from '@/src/core/date/dateOnly';
import {
  formatDayOfMonth,
  formatWeekdayShort,
  weekDates,
} from '@/src/features/training/utils/calendarRange';

interface CalendarWeekStripProps {
  weekStart: DateOnly;
  selectedDate: DateOnly;
  onSelectDate: (date: DateOnly) => void;
}

export function CalendarWeekStrip({ weekStart, selectedDate, onSelectDate }: CalendarWeekStripProps) {
  const theme = useAppTheme();
  const dates = weekDates(weekStart);

  return (
    <View style={styles.strip} testID="calendar-week-strip">
      {dates.map((date) => {
        const selected = date === selectedDate;
        return (
          <Pressable
            key={date}
            testID={`calendar-day-${date}`}
            onPress={() => onSelectDate(date)}
            style={[
              styles.day,
              {
                backgroundColor: selected ? theme.colors.primary : theme.colors.surface,
                borderColor: theme.colors.border,
              },
            ]}>
            <Text
              style={[
                styles.weekday,
                { color: selected ? theme.colors.primaryText : theme.colors.textMuted },
              ]}>
              {formatWeekdayShort(date)}
            </Text>
            <Text
              style={[
                styles.dayNumber,
                { color: selected ? theme.colors.primaryText : theme.colors.text },
              ]}>
              {formatDayOfMonth(date)}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  strip: {
    flexDirection: 'row',
    gap: 6,
  },
  day: {
    flex: 1,
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: 10,
    paddingVertical: 8,
    gap: 2,
  },
  weekday: {
    fontSize: 11,
    fontWeight: '600',
  },
  dayNumber: {
    fontSize: 16,
    fontWeight: '700',
  },
});
