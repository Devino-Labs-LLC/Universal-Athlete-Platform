import { Link } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { todayDateOnly } from '@/core/date/dateOnly';
import { OccurrenceCard } from '@/features/training/components/OccurrenceCard';
import { useTrainingOverview } from '@/features/training/hooks/usePlans';
import type { CalendarEntry } from '@/features/training/models/schemas';

export function TrainingLandingPage() {
  const overviewQuery = useTrainingOverview(todayDateOnly());

  if (overviewQuery.isLoading) {
    return <LoadingView message="Loading training overview…" />;
  }

  if (overviewQuery.isError) {
    return <ErrorView message="Unable to load training overview." onRetry={() => overviewQuery.refetch()} />;
  }

  const overview = overviewQuery.data!;
  const upcoming = overview.upcomingOccurrences ?? [];
  const activePlans = overview.activePlans ?? [];

  return (
    <Page
      title="Training"
      description="Overview of active plans, upcoming workouts, and quick navigation."
      actions={
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link to="/app/training/plans/new">
            <Button type="button">New plan</Button>
          </Link>
          <Link to="/app/training/calendar">
            <Button type="button" variant="secondary">
              Calendar
            </Button>
          </Link>
        </div>
      }
    >
      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">Active plans</h2>
        {activePlans.length === 0 ? (
          <EmptyView title="No active plans" message="Create a plan to get started." />
        ) : (
          <div style={{ display: 'grid', gap: '0.75rem' }}>
            {activePlans.map((plan) => (
              <article key={plan.trainingPlanId} className="card">
                <h3>{plan.name}</h3>
                <p>{plan.type}</p>
                <Link to={`/app/training/plans/${plan.trainingPlanId}`}>Open builder</Link>
              </article>
            ))}
          </div>
        )}
        <p style={{ marginTop: '0.75rem' }}>
          <Link to="/app/training/plans">View all plans</Link>
        </p>
      </section>

      <section className="card">
        <h2 className="cardTitle">Upcoming workouts</h2>
        {upcoming.length === 0 ? (
          <EmptyView title="No upcoming workouts" message="Activate a schedule to generate occurrences." />
        ) : (
          <div style={{ display: 'grid', gap: '0.5rem' }}>
            {upcoming.map((occurrence) => (
              <OccurrenceCard
                key={occurrence.occurrenceId}
                entry={
                  {
                    occurrenceId: occurrence.occurrenceId,
                    trainingPlanId: occurrence.trainingPlanId,
                    trainingPlanName: occurrence.trainingPlanName,
                    workoutDayId: occurrence.workoutDayId,
                    workoutDayName: occurrence.workoutDayName,
                    scheduledDate: occurrence.scheduledDate,
                    status: occurrence.status,
                    exerciseCount: occurrence.exerciseCount,
                    completedExerciseCount: occurrence.completedExerciseCount,
                  } satisfies CalendarEntry
                }
              />
            ))}
          </div>
        )}
      </section>
    </Page>
  );
}
