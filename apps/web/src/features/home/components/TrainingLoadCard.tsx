import { HomeCard } from '@/features/home/components/HomeCard';
import type { TodayDashboard } from '@/features/home/schemas';

interface TrainingLoadCardProps {
  trainingLoad: TodayDashboard['trainingLoad'];
}

export function TrainingLoadCard({ trainingLoad }: TrainingLoadCardProps) {
  if (!trainingLoad?.loadPresent) {
    return (
      <HomeCard title="Training load">
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>No training load data yet.</p>
      </HomeCard>
    );
  }

  return (
    <HomeCard title="Training load">
      {trainingLoad.completedSetCount != null ? (
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>
          Sets completed: {trainingLoad.completedSetCount}
        </p>
      ) : null}
      {trainingLoad.totalVolumeKilograms != null ? (
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>
          Volume: {trainingLoad.totalVolumeKilograms} kg
        </p>
      ) : null}
      {trainingLoad.averageSessionRpe != null ? (
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>
          Avg session RPE: {trainingLoad.averageSessionRpe}
        </p>
      ) : null}
    </HomeCard>
  );
}
