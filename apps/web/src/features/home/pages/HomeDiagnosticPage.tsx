import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { isApiError } from '@/core/api/errors';
import { formatDateDisplay, parseDateOnly, todayDateOnly } from '@/core/date/dateOnly';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { fetchTodayDashboard } from '@/features/home/api';
import {
  deriveReadinessBand,
  deriveRecommendationAction,
  deriveTrainingOccurrenceCount,
} from '@/features/home/schemas';
import { trainingClientKeys } from '@/features/home/queryKeys';

export function HomeDiagnosticPage() {
  const { apiClient } = useAuthSession();
  const today = todayDateOnly();

  const todayQuery = useQuery({
    queryKey: trainingClientKeys.today(today),
    queryFn: () => fetchTodayDashboard(apiClient, today),
  });

  if (todayQuery.isLoading) {
    return <LoadingView message="Loading today dashboard…" />;
  }

  if (todayQuery.isError) {
    const message = isApiError(todayQuery.error)
      ? todayQuery.error.message
      : 'Failed to load today dashboard';
    return <ErrorView message={message} onRetry={() => void todayQuery.refetch()} />;
  }

  const data = todayQuery.data;
  if (!data) {
    return <EmptyView message="No today dashboard data is available yet." />;
  }

  const readinessBand = deriveReadinessBand(data);
  const recommendationAction = deriveRecommendationAction(data);
  const occurrenceCount = deriveTrainingOccurrenceCount(data);

  return (
    <Page
      title="Today diagnostic"
      description="Week 1 connectivity check against the training today endpoint."
    >
      <div className="statGrid">
        <div className="card stat">
          <span className="statLabel">Date</span>
          <span className="statValue">{formatDateDisplay(parseDateOnly(data.date))}</span>
        </div>
        <div className="card stat">
          <span className="statLabel">Recovery present</span>
          <span className="statValue">{data.recovery.checkInPresent ? 'Yes' : 'No'}</span>
        </div>
        <div className="card stat">
          <span className="statLabel">Readiness band</span>
          <span className="statValue">{readinessBand ?? 'Not available'}</span>
        </div>
        <div className="card stat">
          <span className="statLabel">Recommendation action</span>
          <span className="statValue">{recommendationAction ?? 'Not available'}</span>
        </div>
        <div className="card stat">
          <span className="statLabel">Scheduled workouts</span>
          <span className="statValue">{occurrenceCount}</span>
        </div>
      </div>
    </Page>
  );
}
