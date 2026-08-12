import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import type { DateOnly } from '@/core/date/dateOnly';
import { todayDateOnly } from '@/core/date/dateOnly';
import { CalendarGrid } from '@/features/training/components/CalendarGrid';
import { usePlans } from '@/features/training/hooks/usePlans';
import { useTrainingCalendar } from '@/features/training/hooks/useTrainingCalendar';
import {
  addMonths,
  formatMonthYear,
  monthGridRange,
  startOfMonth,
} from '@/features/training/utils/calendarRange';
import styles from '@/features/training/calendar/TrainingCalendarPage.module.scss';

export function TrainingCalendarPage() {
  const [visibleMonth, setVisibleMonth] = useState<DateOnly>(() => startOfMonth(todayDateOnly()));
  const [selectedDate, setSelectedDate] = useState<DateOnly | null>(todayDateOnly());
  const [statusFilter, setStatusFilter] = useState('');
  const [planFilter, setPlanFilter] = useState('');

  const range = useMemo(() => monthGridRange(visibleMonth), [visibleMonth]);
  const calendarQuery = useTrainingCalendar(range.from, range.to, {
    status: statusFilter || undefined,
    trainingPlanId: planFilter || undefined,
  });
  const plansQuery = usePlans();

  if (calendarQuery.isError) {
    return (
      <ErrorView message="Unable to load calendar." onRetry={() => calendarQuery.refetch()} />
    );
  }

  return (
    <Page
      title="Training calendar"
      description="Athlete training calendar — month view with selected-day detail."
      width="wide"
      actions={
        <Link to="/app/training/plans">
          <Button type="button" variant="secondary">
            Plans
          </Button>
        </Link>
      }
    >
      <section className={styles.toolbar} aria-label="Calendar controls">
        <div className={styles.monthNav}>
          <Button type="button" variant="secondary" onClick={() => setVisibleMonth(addMonths(visibleMonth, -1))}>
            Previous month
          </Button>
          <h2 className={styles.monthTitle}>{formatMonthYear(visibleMonth)}</h2>
          <Button type="button" variant="secondary" onClick={() => setVisibleMonth(addMonths(visibleMonth, 1))}>
            Next month
          </Button>
        </div>
        <div className={styles.filters}>
          <label className={styles.filter}>
            <span className={styles.filterLabel}>Status</span>
            <select className="input" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="">All</option>
              <option value="SCHEDULED">Scheduled</option>
              <option value="IN_PROGRESS">In progress</option>
              <option value="COMPLETED">Completed</option>
            </select>
          </label>
          <label className={styles.filter}>
            <span className={styles.filterLabel}>Plan</span>
            <select className="input" value={planFilter} onChange={(e) => setPlanFilter(e.target.value)}>
              <option value="">All plans</option>
              {(plansQuery.data ?? []).map((plan) => (
                <option key={plan.id} value={plan.id}>
                  {plan.name}
                </option>
              ))}
            </select>
          </label>
        </div>
      </section>

      {calendarQuery.isLoading ? <LoadingView message="Loading calendar…" /> : null}
      <CalendarGrid
        visibleMonth={visibleMonth}
        selectedDate={selectedDate}
        entries={calendarQuery.data ?? []}
        onSelectDate={setSelectedDate}
      />
    </Page>
  );
}
