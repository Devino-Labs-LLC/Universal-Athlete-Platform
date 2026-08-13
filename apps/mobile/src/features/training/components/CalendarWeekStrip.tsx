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
            accessibilityRole="button"
            accessibilityLabel={`${formatWeekdayShort(date)} ${formatDayOfMonth(date)}${selected ? ', selected' : ''}`}
            accessibilityState={{ selected }}
            onPress={() => onSelectDate(date)}
            style={[
              styles.day,
              {
                backgroundColor: selected
                  ? theme.colors.accentCyanMuted
                  : theme.colors.surfaceElevated,
                borderColor: selected ? theme.colors.accentCyan : theme.colors.border,
                minHeight: 44,
              },
            ]}>
            <Text
              style={[
                styles.weekday,
                { color: selected ? theme.colors.accentCyan : theme.colors.textMuted },
              ]}>
              {formatWeekdayShort(date)}
            </Text>
            <Text
              style={[
                styles.dayNumber,
                { color: selected ? theme.colors.text : theme.colors.text },
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
    justifyContent: 'center',
    borderWidth: StyleSheet.hairlineWidth,
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
