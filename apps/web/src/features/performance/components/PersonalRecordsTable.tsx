import { Link } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import tableStyles from '@/core/components/Table.module.scss';
import { formatDateDisplay, parseDateOnly } from '@/core/date/dateOnly';
import { personalRecordTypeLabel } from '@/features/performance/models/labels';
import surfaces from '@/features/performance/styles/performanceSurfaces.module.scss';
import { formatPersonalRecord } from '@/features/performance/utils/formatPersonalRecord';
import type { PersonalRecord } from '@/features/performance/models/schemas';

interface PersonalRecordsTableProps {
  records: PersonalRecord[];
  showExerciseColumn?: boolean;
}

export function PersonalRecordsTable({ records, showExerciseColumn = true }: PersonalRecordsTableProps) {
  if (records.length === 0) {
    return <p className={tableStyles.subtle}>No personal records recorded yet.</p>;
  }

  return (
    <div className={surfaces.tableWrap}>
      <table className={tableStyles.table}>
        <caption className="srOnly">Personal records</caption>
        <thead>
          <tr>
            {showExerciseColumn ? <th scope="col">Exercise</th> : null}
            <th scope="col">Record type</th>
            <th scope="col">Value</th>
            <th scope="col">Achieved</th>
          </tr>
        </thead>
        <tbody>
          {records.map((record) => (
            <tr key={record.id}>
              {showExerciseColumn ? (
                <td>
                  <Link className={tableStyles.link} to={`/app/performance/exercises/${record.exercisePerformanceKey}`}>
                    {record.exerciseName}
                  </Link>
                </td>
              ) : null}
              <td>
                <div className={surfaces.metaRow}>
                  <span>{personalRecordTypeLabel(record.recordType)}</span>
                  {record.estimated ? <Badge tone="info">Estimated</Badge> : null}
                </div>
              </td>
              <td className={tableStyles.numeric}>{formatPersonalRecord(record)}</td>
              <td>{record.achievedAt ? formatDateDisplay(parseDateOnly(record.achievedAt.slice(0, 10))) : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
