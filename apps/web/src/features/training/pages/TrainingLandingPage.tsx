import { Link } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { todayDateOnly } from '@/core/date/dateOnly';
import { MetricPill } from '@/features/training/components/MetricPill';
import { TrainingStatusBadge } from '@/features/training/components/TrainingStatusBadge';
import { useTrainingOverview } from '@/features/training/hooks/usePlans';
import { planTypeLabel } from '@/features/training/models/labels';
import type { TrainingOverview } from '@/features/training/models/schemas';
import styles from '@/features/training/pages/TrainingLandingPage.module.scss';

type OverviewOccurrence = NonNullable<TrainingOverview['upcomingOccurrences']>[number];
type OverviewPlan = NonNullable<TrainingOverview['activePlans']>[number];

function occurrencePath(occurrence: OverviewOccurrence): string {
  return `/app/training/plans/${occurrence.trainingPlanId}/days/${occurrence.workoutDayId}/occurrences/${occurrence.occurrenceId}`;
}

function pickFeaturedOccurrence(upcoming: OverviewOccurrence[]): OverviewOccurrence | null {
  const inProgress = upcoming.find((item) => item.status === 'IN_PROGRESS');
  if (inProgress) {
    return inProgress;
  }
  const scheduled = upcoming
    .filter((item) => item.status === 'SCHEDULED')
    .sort((a, b) => a.scheduledDate.localeCompare(b.scheduledDate));
  return scheduled[0] ?? upcoming[0] ?? null;
}

function addDays(dateOnly: string, days: number): string {
  const date = new Date(`${dateOnly}T12:00:00`);
  date.setDate(date.getDate() + days);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function weekdayShort(dateOnly: string): string {
  return new Date(`${dateOnly}T12:00:00`).toLocaleDateString(undefined, { weekday: 'short' });
}

function dayNumber(dateOnly: string): string {
  return String(Number(dateOnly.split('-')[2]));
}

export function TrainingLandingPage() {
  const overviewQuery = useTrainingOverview(todayDateOnly());

  if (overviewQuery.isLoading) {
    return <LoadingView message="Loading training overview…" />;
  }

  if (overviewQuery.isError) {
    return <ErrorView message="Unable to load training overview." onRetry={() => overviewQuery.refetch()} />;
  }

  const overview = overviewQuery.data!;
  const upcoming = [...(overview.upcomingOccurrences ?? [])].sort((a, b) =>
    a.scheduledDate.localeCompare(b.scheduledDate),
  );
  const activePlans = overview.activePlans ?? [];
  const recent = overview.recentCompletedSessions ?? [];
  const featured = pickFeaturedOccurrence(upcoming);
  const weekEnd = addDays(overview.date, 6);
  const thisWeek = upcoming.filter(
    (item) => item.scheduledDate >= overview.date && item.scheduledDate <= weekEnd,
  );
  const isEmptyHub = activePlans.length === 0 && upcoming.length === 0 && recent.length === 0;

  return (
    <Page
      title="Training"
      description="Operational hub for upcoming sessions, active plans, and recent work."
      width="wide"
      actions={
        <div className={styles.headerActions}>
          <Link to="/app/training/plans/new">
            <Button type="button">New plan</Button>
          </Link>
          <Link to="/app/training/calendar">
            <Button type="button" variant="secondary">
              Calendar
            </Button>
          </Link>
          <Link to="/app/exercises">
            <Button type="button" variant="secondary">
              Exercise catalog
            </Button>
          </Link>
          <Link to="/app/environments">
            <Button type="button" variant="secondary">
              Environments
            </Button>
          </Link>
        </div>
      }
    >
      <div className={styles.hub}>
        <section className={styles.hero} aria-labelledby="next-training-heading">
          <p className={styles.eyebrow} id="next-training-heading">
            Next / current training
          </p>
          {featured ? (
            <div className={styles.heroBody}>
              <div className={styles.heroCopy}>
                <div className={styles.heroMeta}>
                  <TrainingStatusBadge kind="occurrence" status={featured.status} />
                  <span className={styles.metaText}>{featured.scheduledDate}</span>
                  <MetricPill label="Ex">
                    {featured.completedExerciseCount}/{featured.exerciseCount}
                  </MetricPill>
                </div>
                <h2 className={styles.heroTitle}>{featured.workoutDayName}</h2>
                <p className={styles.metaText}>{featured.trainingPlanName}</p>
              </div>
              <div className={styles.heroActions}>
                <Link to={occurrencePath(featured)}>
                  <Button type="button">
                    {featured.status === 'IN_PROGRESS' ? 'Continue session' : 'Open session'}
                  </Button>
                </Link>
                <Link to={`/app/training/plans/${featured.trainingPlanId}`}>
                  <Button type="button" variant="secondary">
                    Open plan
                  </Button>
                </Link>
              </div>
            </div>
          ) : (
            <div className={styles.emptyHero}>
              <p className={styles.emptyLead}>No session queued.</p>
              <p className={styles.metaText}>
                {isEmptyHub
                  ? 'Create a plan and activate a schedule to populate this hub.'
                  : 'Activate a schedule or generate occurrences to see upcoming work.'}
              </p>
              <div className={styles.heroActions}>
                <Link to="/app/training/plans/new">
                  <Button type="button">Create plan</Button>
                </Link>
                <Link to="/app/training/plans">
                  <Button type="button" variant="secondary">
                    View all plans
                  </Button>
                </Link>
              </div>
            </div>
          )}
        </section>

        <section className={styles.panel} aria-labelledby="this-week-heading">
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle} id="this-week-heading">
              This week
            </h2>
            <span className={styles.panelHint}>
              {overview.date} – {weekEnd}
            </span>
          </div>
          {thisWeek.length === 0 ? (
            <EmptyView
              title="No workouts this week"
              message="Scheduled sessions in the next seven days will appear here."
            />
          ) : (
            <ul className={styles.weekList}>
              {thisWeek.map((occurrence) => {
                const selected = featured?.occurrenceId === occurrence.occurrenceId;
                return (
                  <li key={occurrence.occurrenceId}>
                    <Link
                      to={occurrencePath(occurrence)}
                      className={[styles.weekRow, selected ? styles.weekRowSelected : '']
                        .filter(Boolean)
                        .join(' ')}
                    >
                      <span className={styles.weekDate}>
                        <span className={styles.weekDay}>{weekdayShort(occurrence.scheduledDate)}</span>
                        <span className={styles.weekNum}>{dayNumber(occurrence.scheduledDate)}</span>
                      </span>
                      <span className={styles.weekCopy}>
                        <span className={styles.weekTitle}>{occurrence.workoutDayName}</span>
                        <span className={styles.metaText}>{occurrence.trainingPlanName}</span>
                      </span>
                      <TrainingStatusBadge kind="occurrence" status={occurrence.status} />
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
        </section>

        <section className={styles.panel} aria-labelledby="active-plans-heading">
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle} id="active-plans-heading">
              Active plans
            </h2>
            <Link className={styles.panelLink} to="/app/training/plans">
              View all plans
            </Link>
          </div>
          {activePlans.length === 0 ? (
            <EmptyView title="No active plans" message="Create a plan to get started." />
          ) : (
            <ul className={styles.planList}>
              {activePlans.map((plan) => (
                <li key={plan.trainingPlanId}>
                  <ActivePlanRow plan={plan} />
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className={styles.panel} aria-labelledby="recent-activity-heading">
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle} id="recent-activity-heading">
              Recent activity
            </h2>
            <span className={styles.panelHint}>Completed sessions from overview</span>
          </div>
          {recent.length === 0 ? (
            <EmptyView
              title="No recent sessions"
              message="Completed workouts will show here once you finish sessions."
            />
          ) : (
            <ul className={styles.recentList}>
              {recent.map((session) => (
                <li key={session.occurrenceId} className={styles.recentRow}>
                  <div>
                    <p className={styles.recentTitle}>{session.workoutDayName}</p>
                    <p className={styles.metaText}>
                      {session.trainingPlanName} · {session.scheduledDate}
                    </p>
                  </div>
                  <MetricPill label="Done">
                    {session.completedExerciseCount}/{session.exerciseCount}
                  </MetricPill>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </Page>
  );
}

function ActivePlanRow({ plan }: { plan: OverviewPlan }) {
  return (
    <article className={styles.planRow}>
      <div className={styles.planCopy}>
        <div className={styles.planTitleRow}>
          <h3 className={styles.planName}>{plan.name}</h3>
          <TrainingStatusBadge kind="plan" status={plan.status} />
        </div>
        <p className={styles.metaText}>
          {planTypeLabel(plan.type)} · starts {plan.startDate}
          {plan.endDate ? ` · ends ${plan.endDate}` : ''}
          {plan.scheduleTimezone ? ` · ${plan.scheduleTimezone}` : ''}
        </p>
      </div>
      <div className={styles.planActions}>
        <Link to={`/app/training/plans/${plan.trainingPlanId}`}>
          <Button type="button" variant="secondary">
            Open builder
          </Button>
        </Link>
        <Link to={`/app/training/plans/${plan.trainingPlanId}/schedule`}>
          <Button type="button" variant="ghost">
            Schedule
          </Button>
        </Link>
      </div>
    </article>
  );
}
