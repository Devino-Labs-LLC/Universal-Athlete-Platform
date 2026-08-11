import tableStyles from '@/core/components/Table.module.scss';
import { formatDateDisplay, parseDateOnly } from '@/core/date/dateOnly';
import { ratingLabelForMetric } from '@/features/recovery/models/labels';
import type { AthleteRecoveryHistoryDay } from '@/features/recovery/models/schemas';

interface CheckInHistoryTableProps {
  days: AthleteRecoveryHistoryDay[];
}

function ratingCell(value: number | null | undefined, metric: 'fatigue' | 'muscleSoreness' | 'stress' | 'mood' | 'motivation' | 'sleepQuality'): string {
  if (value == null) {
    return '—';
  }
  return `${value} (${ratingLabelForMetric(metric, value)})`;
}

export function CheckInHistoryTable({ days }: CheckInHistoryTableProps) {
  const daysWithData = days.filter((day) => day.checkIn != null);

  if (daysWithData.length === 0) {
    return <p className={tableStyles.subtle}>No recovery check-ins recorded in this date range.</p>;
  }

  return (
    <table className={tableStyles.table}>
      <caption className="srOnly">Recovery check-in history</caption>
      <thead>
        <tr>
          <th scope="col">Date</th>
          <th scope="col">Sleep</th>
          <th scope="col">Fatigue</th>
          <th scope="col">Soreness</th>
          <th scope="col">Stress</th>
          <th scope="col">Mood</th>
          <th scope="col">Discomfort</th>
        </tr>
      </thead>
      <tbody>
        {daysWithData.map((day) => {
          const checkIn = day.checkIn!;
          return (
            <tr key={day.date}>
              <th scope="row">{formatDateDisplay(parseDateOnly(day.date))}</th>
              <td className={tableStyles.numeric}>
                {checkIn.sleepDurationMinutes != null ? `${Math.round(checkIn.sleepDurationMinutes / 60)}h` : '—'}
              </td>
              <td>{ratingCell(checkIn.fatigue?.value, 'fatigue')}</td>
              <td>{ratingCell(checkIn.muscleSoreness?.value, 'muscleSoreness')}</td>
              <td>{ratingCell(checkIn.stress?.value, 'stress')}</td>
              <td>{ratingCell(checkIn.mood?.value, 'mood')}</td>
              <td>{checkIn.discomfortAreas.length > 0 ? `${checkIn.discomfortAreas.length} area(s)` : 'None'}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
