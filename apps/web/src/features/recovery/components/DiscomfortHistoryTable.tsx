import { Badge } from '@/core/components/Badge';
import tableStyles from '@/core/components/Table.module.scss';
import { formatDateDisplay, parseDateOnly } from '@/core/date/dateOnly';
import { bodyAreaLabel, bodySideLabel } from '@/features/recovery/models/labels';
import type { BodyAreaDiscomfortHistory, BodyAreaDiscomfortHistoryEntry } from '@/features/recovery/models/schemas';
import surfaces from '@/features/recovery/styles/recoverySurfaces.module.scss';
import { discomfortIntensityTone } from '@/features/recovery/utils/readinessVisual';

interface DiscomfortHistoryTableProps {
  history: BodyAreaDiscomfortHistory;
}

function intensityValue(intensity: BodyAreaDiscomfortHistoryEntry['intensity']): number | null {
  if (intensity == null) {
    return null;
  }
  return intensity.value;
}

export function DiscomfortHistoryTable({ history }: DiscomfortHistoryTableProps) {
  if (history.entries.length === 0) {
    return <p className={tableStyles.subtle}>No discomfort reported in this date range.</p>;
  }

  return (
    <div className={surfaces.hub} style={{ gap: 'var(--uap-space-3)' }}>
      <div className={surfaces.kpiRow}>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Observations</span>
          <span className={surfaces.metricValue}>{history.observationCount}</span>
        </div>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Avg intensity</span>
          <span className={surfaces.metricValue}>
            {history.averageIntensity != null ? Number(history.averageIntensity).toFixed(1) : '—'}
          </span>
        </div>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Peak intensity</span>
          <span className={surfaces.metricValue}>
            {history.maximumIntensity != null ? Number(history.maximumIntensity).toFixed(1) : '—'}
          </span>
        </div>
      </div>

      <div className={surfaces.tableWrap}>
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
            {history.entries.map((entry, index) => {
              const intensity = intensityValue(entry.intensity);
              return (
                <tr key={`${entry.date}-${entry.bodyArea}-${entry.side}-${index}`}>
                  <th scope="row">{formatDateDisplay(parseDateOnly(entry.date))}</th>
                  <td>{bodyAreaLabel(entry.bodyArea)}</td>
                  <td>{bodySideLabel(entry.side)}</td>
                  <td>
                    {intensity != null ? (
                      <Badge tone={discomfortIntensityTone(intensity)}>{String(intensity)}</Badge>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>{entry.notes ?? '—'}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
