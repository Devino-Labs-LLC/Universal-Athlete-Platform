import type { DateOnly } from '@/core/date/dateOnly';
import { todayDateOnly } from '@/core/date/dateOnly';
import { OccurrenceCard } from '@/features/training/components/OccurrenceCard';
import type { CalendarEntry } from '@/features/training/models/schemas';
import { daysInMonthGrid, isSameMonth } from '@/features/training/utils/calendarRange';
import styles from '@/features/training/components/CalendarGrid.module.scss';

interface CalendarGridProps {
  visibleMonth: DateOnly;
  selectedDate: DateOnly | null;
  entries: CalendarEntry[];
  onSelectDate: (date: DateOnly) => void;
}

export function CalendarGrid({
  visibleMonth,
  selectedDate,
  entries,
  onSelectDate,
}: CalendarGridProps) {
  const gridDays = daysInMonthGrid(visibleMonth);
  const today = todayDateOnly();

  const entriesByDate = entries.reduce<Record<string, CalendarEntry[]>>((acc, entry) => {
    acc[entry.scheduledDate] = acc[entry.scheduledDate] ?? [];
    acc[entry.scheduledDate]!.push(entry);
    return acc;
  }, {});

  return (
    <div className={styles.grid} role="grid" aria-label="Training calendar">
      {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((label) => (
        <div key={label} className={styles.weekdayHeader} role="columnheader">
          {label}
        </div>
      ))}
      {gridDays.map((date) => {
        const dayEntries = entriesByDate[date] ?? [];
        const inMonth = isSameMonth(date, visibleMonth);
        const isSelected = selectedDate === date;
        const isToday = date === today;

        return (
          <button
            key={date}
            type="button"
            role="gridcell"
            className={[
              styles.dayCell,
              !inMonth ? styles.outsideMonth : '',
              isSelected ? styles.selected : '',
              isToday ? styles.today : '',
            ]
              .filter(Boolean)
              .join(' ')}
            onClick={() => onSelectDate(date)}
            aria-label={`${date}, ${dayEntries.length} workouts`}
          >
            <span className={styles.dayNumber}>{Number(date.split('-')[2])}</span>
            {dayEntries.length > 0 ? (
              <span className={styles.dot} aria-hidden="true">
                {dayEntries.length}
              </span>
            ) : null}
          </button>
        );
      })}
      {selectedDate ? (
        <aside className={styles.sidePanel} aria-label="Selected day occurrences">
          <h3>{selectedDate}</h3>
          {entriesByDate[selectedDate]?.length ? (
            <div className={styles.occurrenceList}>
              {entriesByDate[selectedDate]!.map((entry) => (
                <OccurrenceCard key={entry.occurrenceId} entry={entry} />
              ))}
            </div>
          ) : (
            <p className={styles.emptyDay}>No workouts on this date.</p>
          )}
        </aside>
      ) : null}
    </div>
  );
}
