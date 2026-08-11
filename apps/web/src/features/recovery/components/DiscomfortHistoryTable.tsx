import tableStyles from '@/core/components/Table.module.scss';
import { formatDateDisplay, parseDateOnly } from '@/core/date/dateOnly';
import { bodyAreaLabel, bodySideLabel } from '@/features/recovery/models/labels';
import type { BodyAreaDiscomfortHistory, BodyAreaDiscomfortHistoryEntry } from '@/features/recovery/models/schemas';

interface DiscomfortHistoryTableProps {
  history: BodyAreaDiscomfortHistory;
}

function formatIntensity(intensity: BodyAreaDiscomfortHistoryEntry['intensity']): string {
  if (intensity == null) {
    return '—';
  }
  return String(intensity.value);
}

export function DiscomfortHistoryTable({ history }: DiscomfortHistoryTableProps) {
  if (history.entries.length === 0) {
    return <p className={tableStyles.subtle}>No discomfort reported in this date range.</p>;
  }

  return (
    <div>
      <p className={tableStyles.subtle}>
        {history.observationCount} observation{history.observationCount === 1 ? '' : 's'} · average intensity{' '}
        {history.averageIntensity != null ? Number(history.averageIntensity).toFixed(1) : '—'} · peak intensity{' '}
        {history.maximumIntensity != null ? Number(history.maximumIntensity).toFixed(1) : '—'}
      </p>
      <table className={tableStyles.table}>
        <caption className="srOnly">Discomfort history entries</caption>
        <thead>
          <tr>
            <th scope="col">Date</th>
            <th scope="col">Body area</th>
            <th scope="col">Side</th>
            <th scope="col">Intensity</th>
            <th scope="col">Notes</th>
          </tr>
        </thead>
        <tbody>
          {history.entries.map((entry, index) => (
            <tr key={`${entry.date}-${entry.bodyArea}-${entry.side}-${index}`}>
              <th scope="row">{formatDateDisplay(parseDateOnly(entry.date))}</th>
              <td>{bodyAreaLabel(entry.bodyArea)}</td>
              <td>{bodySideLabel(entry.side)}</td>
              <td className={tableStyles.numeric}>{formatIntensity(entry.intensity)}</td>
              <td>{entry.notes ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
