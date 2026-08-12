import { Link } from 'react-router-dom';

import { HomeCard } from '@/features/home/components/HomeCard';
import { personalRecordTypeLabel } from '@/features/home/labels/todayLabels';
import type { TrainingDashboardPersonalRecord } from '@/features/home/schemas';

interface RecentPerformanceCardProps {
  records: TrainingDashboardPersonalRecord[] | undefined;
}

export function RecentPerformanceCard({ records }: RecentPerformanceCardProps) {
  if (!records || records.length === 0) {
    return (
      <HomeCard title="Recent performance">
        <p className="emptyHint">No recent personal records.</p>
        <Link to="/app/performance/records">View all records</Link>
      </HomeCard>
    );
  }

  return (
    <HomeCard title="Recent performance">
      <ul style={{ margin: 0, paddingLeft: '1.25rem', color: 'var(--uap-text-secondary)' }}>
        {records.slice(0, 5).map((record) => (
          <li key={record.personalRecordId}>
            <Link
              to={
                record.exercisePerformanceKey
                  ? `/app/performance/exercises/${record.exercisePerformanceKey}`
                  : '/app/performance/records'
              }
            >
              {record.exerciseName}
            </Link>{' '}
            — {personalRecordTypeLabel(record.recordType)}
            {record.normalizedValue != null ? `: ${record.normalizedValue}` : ''}
          </li>
        ))}
      </ul>
      <Link to="/app/performance/records">View all records</Link>
    </HomeCard>
  );
}
