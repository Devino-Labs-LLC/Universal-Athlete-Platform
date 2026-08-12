import { Button } from '@/core/components/Button';
import { DAY_OF_WEEK_LABELS } from '@/features/training/models/labels';
import type { WorkoutDay } from '@/features/training/models/schemas';
import styles from '@/features/training/components/DayList.module.scss';

interface DayListProps {
  days: WorkoutDay[];
  selectedDayId: string | null;
  onSelectDay: (dayId: string) => void;
  onMoveUp: (dayId: string) => void;
  onMoveDown: (dayId: string) => void;
  onEditDay: (day: WorkoutDay) => void;
  onDeleteDay: (day: WorkoutDay) => void;
  readOnly?: boolean;
}

export function DayList({
  days,
  selectedDayId,
  onSelectDay,
  onMoveUp,
  onMoveDown,
  onEditDay,
  onDeleteDay,
  readOnly = false,
}: DayListProps) {
  const grouped = days.reduce<Record<number, WorkoutDay[]>>((acc, day) => {
    const week = day.planWeekNumber ?? 1;
    acc[week] = acc[week] ?? [];
    acc[week]!.push(day);
    return acc;
  }, {});

  const weeks = Object.keys(grouped)
    .map(Number)
    .sort((a, b) => a - b);

  if (days.length === 0) {
    return <p className={styles.empty}>No workout days yet. Create one to start building.</p>;
  }

  return (
    <div className={styles.list}>
      {weeks.map((week) => (
        <section key={week} className={styles.weekGroup}>
          <h3 className={styles.weekTitle}>Week {week}</h3>
          <ul className={styles.items}>
            {grouped[week]!
              .sort((a, b) => a.displayOrder - b.displayOrder)
              .map((day, index, weekDays) => {
                const selected = selectedDayId === day.id;
                return (
                  <li key={day.id} className={styles.item}>
                    <button
                      type="button"
                      className={[styles.dayButton, selected ? styles.selected : '']
                        .filter(Boolean)
                        .join(' ')}
                      onClick={() => onSelectDay(day.id)}
                      aria-pressed={selected}
                    >
                      <span className={styles.dayTitle}>{day.title}</span>
                      <span className={styles.dayMeta}>
                        {day.scheduledDayOfWeek
                          ? DAY_OF_WEEK_LABELS[day.scheduledDayOfWeek]
                          : 'Unscheduled'}
                        {day.trainingEnvironmentOverrideId ? ' · Env override' : ''}
                      </span>
                    </button>
                    {!readOnly ? (
                      <div className={styles.actions}>
                        <Button
                          type="button"
                          variant="ghost"
                          aria-label={`Move ${day.title} up`}
                          disabled={index === 0}
                          onClick={() => onMoveUp(day.id)}
                        >
                          ↑
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          aria-label={`Move ${day.title} down`}
                          disabled={index === weekDays.length - 1}
                          onClick={() => onMoveDown(day.id)}
                        >
                          ↓
                        </Button>
                        <Button type="button" variant="secondary" onClick={() => onEditDay(day)}>
                          Edit
                        </Button>
                        <Button type="button" variant="secondary" onClick={() => onDeleteDay(day)}>
                          Delete
                        </Button>
                      </div>
                    ) : null}
                  </li>
                );
              })}
          </ul>
        </section>
      ))}
    </div>
  );
}
