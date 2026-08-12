import tableStyles from '@/core/components/Table.module.scss';
import { formatDateDisplay, parseDateOnly } from '@/core/date/dateOnly';
import {
  formatDailyLoadSummary,
  formatOccurrenceLoadSummary,
  formatWeeklyLoadSummary,
} from '@/features/performance/utils/formatLoadMetrics';
import type { TrainingLoadHistory } from '@/features/performance/models/schemas';
import surfaces from '@/features/performance/styles/performanceSurfaces.module.scss';

interface TrainingLoadHistoryTableProps {
  history: TrainingLoadHistory;
}

export function TrainingLoadHistoryTable({ history }: TrainingLoadHistoryTableProps) {
  if (history.granularity === 'OCCURRENCE') {
    const occurrences = history.occurrences ?? [];
    if (occurrences.length === 0) {
      return <p className={tableStyles.subtle}>No sessions found in this date range.</p>;
    }
    return (
      <div className={surfaces.tableWrap}>
        <table className={tableStyles.table}>
          <caption className="srOnly">Session-level training load</caption>
          <thead>
            <tr>
              <th scope="col">Date</th>
              <th scope="col">Summary</th>
            </tr>
          </thead>
          <tbody>
            {occurrences.map((occurrence) => (
              <tr key={occurrence.summary.id}>
                <th scope="row">{formatDateDisplay(parseDateOnly(occurrence.summary.scheduledDate))}</th>
                <td>{formatOccurrenceLoadSummary(occurrence.summary).join(' \u00b7 ')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (history.granularity === 'DAILY') {
    const days = history.dailySummaries ?? [];
    if (days.length === 0) {
      return <p className={tableStyles.subtle}>No training load recorded in this date range.</p>;
    }
    return (
      <div className={surfaces.tableWrap}>
        <table className={tableStyles.table}>
          <caption className="srOnly">Daily training load</caption>
          <thead>
            <tr>
              <th scope="col">Date</th>
              <th scope="col">Summary</th>
            </tr>
          </thead>
          <tbody>
            {days.map((day) => (
              <tr key={day.date}>
                <th scope="row">{formatDateDisplay(parseDateOnly(day.date))}</th>
                <td>{formatDailyLoadSummary(day).join(' \u00b7 ')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  const weeks = history.weeklySummaries ?? [];
  if (weeks.length === 0) {
    return <p className={tableStyles.subtle}>No training load recorded in this date range.</p>;
  }
  return (
    <div className={surfaces.tableWrap}>
      <table className={tableStyles.table}>
        <caption className="srOnly">Weekly training load</caption>
        <thead>
          <tr>
            <th scope="col">Week</th>
            <th scope="col">Summary</th>
          </tr>
        </thead>
        <tbody>
          {weeks.map((week) => (
            <tr key={week.weekStartDate}>
              <th scope="row">
                {formatDateDisplay(parseDateOnly(week.weekStartDate))} –{' '}
                {formatDateDisplay(parseDateOnly(week.weekEndDate))}
              </th>
              <td>{formatWeeklyLoadSummary(week).join(' \u00b7 ')}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
