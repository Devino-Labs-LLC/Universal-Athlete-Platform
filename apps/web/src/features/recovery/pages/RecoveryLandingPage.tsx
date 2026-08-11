import { useState } from 'react';
import { Link } from 'react-router-dom';

import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import tableStyles from '@/core/components/Table.module.scss';
import { formatDateDisplay, todayDateOnly } from '@/core/date/dateOnly';
import { BaselineDeviationTable } from '@/features/recovery/components/BaselineDeviationTable';
import { RecoverySubNav } from '@/features/recovery/components/RecoverySubNav';
import { useRecoveryOverview } from '@/features/recovery/hooks/useRecoveryOverview';
import { bodyAreaLabel, bodySideLabel, readinessBandLabel, recommendationActionLabel } from '@/features/recovery/models/labels';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';
import {
  isTrendDays,
  TREND_DAYS_OPTIONS,
  type TrendDays,
} from '@/features/recovery/models/schemas';

const today = todayDateOnly();

export function RecoveryLandingPage() {
  const [trendDays, setTrendDays] = useState<TrendDays>(7);
  const overviewQuery = useRecoveryOverview(today, trendDays);

  return (
    <Page
      title="Recovery"
      description={`Recovery overview for ${formatDateDisplay(today)}.`}
      actions={
        <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span className={tableStyles.subtle}>Trend window</span>
          <select
            className="input"
            value={trendDays}
            onChange={(event) => {
              const next = Number(event.target.value);
              if (isTrendDays(next)) {
                setTrendDays(next);
              }
            }}
          >
            {TREND_DAYS_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option} days
              </option>
            ))}
          </select>
        </label>
      }
    >
      <RecoverySubNav />

      {overviewQuery.isLoading ? <LoadingView message="Loading recovery overview…" /> : null}
      {overviewQuery.isError ? (
        <ErrorView message={recoveryErrorMessage(overviewQuery.error)} onRetry={() => overviewQuery.refetch()} />
      ) : null}

      {overviewQuery.data ? (
        <div style={{ display: 'grid', gap: '1rem' }}>
          <section className="card">
            <h2 className="cardTitle">Today&rsquo;s check-in</h2>
            {overviewQuery.data.checkInPresent && overviewQuery.data.checkIn ? (
              <div className="statGrid">
                <div className="stat">
                  <span className="statLabel">Fatigue</span>
                  <span className="statValue">{overviewQuery.data.checkIn.fatigue ?? '—'}</span>
                </div>
                <div className="stat">
                  <span className="statLabel">Muscle soreness</span>
                  <span className="statValue">{overviewQuery.data.checkIn.muscleSoreness ?? '—'}</span>
                </div>
                <div className="stat">
                  <span className="statLabel">Stress</span>
                  <span className="statValue">{overviewQuery.data.checkIn.stress ?? '—'}</span>
                </div>
                <div className="stat">
                  <span className="statLabel">Mood</span>
                  <span className="statValue">{overviewQuery.data.checkIn.mood ?? '—'}</span>
                </div>
                <div className="stat">
                  <span className="statLabel">Sleep</span>
                  <span className="statValue">
                    {overviewQuery.data.checkIn.sleepDurationMinutes != null
                      ? `${Math.round(overviewQuery.data.checkIn.sleepDurationMinutes / 60)}h`
                      : '—'}
                  </span>
                </div>
              </div>
            ) : (
              <EmptyView title="No check-in yet today" message="Submit a recovery check-in from Home to see today's summary here." />
            )}
          </section>

          <section className="card">
            <h2 className="cardTitle">Baselines &amp; comparisons</h2>
            <BaselineDeviationTable
              baselines={overviewQuery.data.baselines}
              deviations={overviewQuery.data.deviations}
            />
          </section>

          <section className="card">
            <h2 className="cardTitle">Readiness</h2>
            {overviewQuery.data.readinessPresent && overviewQuery.data.readiness ? (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
                <div>
                  <p style={{ margin: 0, fontSize: 'var(--uap-font-size-lg)', fontWeight: 600 }}>
                    {readinessBandLabel(overviewQuery.data.readiness.readinessBand)}
                  </p>
                  {overviewQuery.data.readiness.limitingDimensions.length > 0 ? (
                    <p className={tableStyles.subtle}>
                      Limiting: {overviewQuery.data.readiness.limitingDimensions.join(', ')}
                    </p>
                  ) : null}
                </div>
                <Link to={`/app/recovery/readiness/${overviewQuery.data.readiness.readinessAssessmentId}`}>
                  View details
                </Link>
              </div>
            ) : (
              <EmptyView title="No readiness assessment yet" message="A readiness assessment will appear once one is generated for today." />
            )}
          </section>

          <section className="card">
            <h2 className="cardTitle">Guidance</h2>
            {overviewQuery.data.recommendationPresent && overviewQuery.data.recommendation ? (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
                <p style={{ margin: 0 }}>{recommendationActionLabel(overviewQuery.data.recommendation.overallAction)}</p>
                <Link to={`/app/recovery/guidance/${overviewQuery.data.recommendation.recommendationId}`}>
                  View details
                </Link>
              </div>
            ) : (
              <EmptyView title="No recommendation yet" message="Training guidance will appear once a recommendation is generated for today." />
            )}
          </section>

          {overviewQuery.data.discomfort.length > 0 ? (
            <section className="card">
              <h2 className="cardTitle">Reported discomfort</h2>
              <ul style={{ display: 'grid', gap: '0.35rem', margin: 0, paddingLeft: '1.1rem' }}>
                {overviewQuery.data.discomfort.map((entry, index) => (
                  <li key={`${entry.bodyArea}-${entry.bodySide}-${index}`}>
                    {bodyAreaLabel(entry.bodyArea)} ({bodySideLabel(entry.bodySide)}) — intensity {entry.intensity}
                    {entry.notes ? `: ${entry.notes}` : ''}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
        </div>
      ) : null}
    </Page>
  );
}
