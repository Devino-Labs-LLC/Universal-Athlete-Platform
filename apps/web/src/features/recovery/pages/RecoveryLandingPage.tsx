import { useState } from 'react';
import { Link } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import { Button } from '@/core/components/Button';
import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { ScoreRing } from '@/core/components/ScoreRing';
import { formatDateDisplay, todayDateOnly } from '@/core/date/dateOnly';
import { MetricPill } from '@/features/training/components/MetricPill';
import { BaselineDeviationTable } from '@/features/recovery/components/BaselineDeviationTable';
import { RecoverySubNav } from '@/features/recovery/components/RecoverySubNav';
import { useRecoveryOverview } from '@/features/recovery/hooks/useRecoveryOverview';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';
import {
  bodyAreaLabel,
  bodySideLabel,
  metricTypeLabel,
  readinessBandLabel,
  readinessDimensionLabel,
  recommendationActionLabel,
  recommendationStatusLabel,
  trendDirectionLabel,
} from '@/features/recovery/models/labels';
import {
  isTrendDays,
  TREND_DAYS_OPTIONS,
  type TrendDays,
} from '@/features/recovery/models/schemas';
import surfaces from '@/features/recovery/styles/recoverySurfaces.module.scss';
import { formatDurationSeconds, formatVolumeKg } from '@/features/recovery/utils/formatMetrics';
import {
  discomfortIntensityTone,
  readinessRingTone,
  trendDirectionBadgeTone,
} from '@/features/recovery/utils/readinessVisual';

const today = todayDateOnly();

function formatSleepHours(minutes: number | null | undefined): string {
  if (minutes == null) {
    return '—';
  }
  return `${Math.round(minutes / 60)}h`;
}

function formatMetric(value: number | null | undefined): string {
  return value == null ? '—' : String(value);
}

export function RecoveryLandingPage() {
  const [trendDays, setTrendDays] = useState<TrendDays>(7);
  const overviewQuery = useRecoveryOverview(today, trendDays);

  const overview = overviewQuery.data;
  const readiness = overview?.readinessPresent ? overview.readiness : null;
  const score = readiness?.readinessScore != null ? Number(readiness.readinessScore) : null;
  const hasScore = score != null && Number.isFinite(score);
  const load = overview?.trainingLoadContext ?? null;

  return (
    <Page
      title="Recovery"
      description={`Recovery overview for ${formatDateDisplay(today)}.`}
      width="wide"
      actions={
        <label className={surfaces.filter}>
          <span className={surfaces.filterLabel}>Trend window</span>
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

      {overview ? (
        <div className={surfaces.hub}>
          <section className={surfaces.hero} aria-labelledby="recovery-hero-heading">
            <ScoreRing
              score={hasScore ? score : null}
              label="Readiness"
              tone={readinessRingTone(readiness?.readinessBand)}
              size={128}
            />
            <div className={surfaces.heroCopy}>
              <p className={surfaces.eyebrow} id="recovery-hero-heading">
                Today · Recovery
              </p>
              {readiness ? (
                <>
                  <h2 className={surfaces.heroTitle}>{readinessBandLabel(readiness.readinessBand)}</h2>
                  <div className={surfaces.metaRow}>
                    {hasScore ? (
                      <MetricPill label="Score">{Math.round(score)}</MetricPill>
                    ) : (
                      <MetricPill label="Score">—</MetricPill>
                    )}
                  </div>
                  {readiness.limitingDimensions.length > 0 ? (
                    <p className={surfaces.metaText}>
                      Limiting: {readiness.limitingDimensions.map(readinessDimensionLabel).join(', ')}
                    </p>
                  ) : (
                    <p className={surfaces.metaText}>No limiting dimensions flagged for today.</p>
                  )}
                  <Link
                    className={surfaces.panelLink}
                    to={`/app/recovery/readiness/${readiness.readinessAssessmentId}`}
                  >
                    View details
                  </Link>
                </>
              ) : (
                <>
                  <h2 className={surfaces.heroTitle}>No readiness assessment yet</h2>
                  <p className={surfaces.metaText}>
                    A readiness score will appear once an assessment is generated for today. Start with a
                    recovery check-in if you have not submitted one, then generate daily state from Home.
                  </p>
                  <Link to="/app/recovery/check-in">
                    <Button type="button">Check in</Button>
                  </Link>
                </>
              )}
            </div>
          </section>

          <section className={surfaces.panel} aria-labelledby="signals-heading">
            <div className={surfaces.panelHeader}>
              <h2 className={surfaces.panelTitle} id="signals-heading">
                Today&apos;s signals
              </h2>
              <Link className={surfaces.panelLink} to="/app/recovery/check-in">
                {overview.checkInPresent ? 'Update check-in' : 'Check in'}
              </Link>
            </div>
            {overview.checkInPresent && overview.checkIn ? (
              <div className={surfaces.metricGrid}>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Fatigue</span>
                  <span className={surfaces.metricValue}>{formatMetric(overview.checkIn.fatigue)}</span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Soreness</span>
                  <span className={surfaces.metricValue}>{formatMetric(overview.checkIn.muscleSoreness)}</span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Stress</span>
                  <span className={surfaces.metricValue}>{formatMetric(overview.checkIn.stress)}</span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Mood</span>
                  <span className={surfaces.metricValue}>{formatMetric(overview.checkIn.mood)}</span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Motivation</span>
                  <span className={surfaces.metricValue}>{formatMetric(overview.checkIn.motivation)}</span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Sleep</span>
                  <span className={surfaces.metricValue}>
                    {formatSleepHours(overview.checkIn.sleepDurationMinutes)}
                  </span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Sleep quality</span>
                  <span className={surfaces.metricValue}>{formatMetric(overview.checkIn.sleepQuality)}</span>
                </div>
              </div>
            ) : (
              <EmptyView
                title="No check-in yet today"
                message="Submit a recovery check-in to see today's summary here. Daily state, readiness, and guidance are generated separately from Home."
              />
            )}
          </section>

          <section className={surfaces.panel} aria-labelledby="trends-heading">
            <div className={surfaces.panelHeader}>
              <h2 className={surfaces.panelTitle} id="trends-heading">
                Recovery trends
              </h2>
              <span className={surfaces.panelHint}>{trendDays}-day window</span>
            </div>
            {overview.trends.length === 0 ? (
              <EmptyView
                title="No trend data yet"
                message="Trends appear after enough check-ins accumulate in the selected window."
              />
            ) : (
              <ul className={surfaces.trendList}>
                {overview.trends.map((trend) => (
                  <li key={trend.metricType} className={surfaces.trendRow}>
                    <div>
                      <p className={surfaces.trendName}>{metricTypeLabel(trend.metricType)}</p>
                      <p className={surfaces.metaText}>
                        {trend.observationCount} observation{trend.observationCount === 1 ? '' : 's'}
                      </p>
                    </div>
                    <Badge tone={trendDirectionBadgeTone(trend.trendDirection)}>
                      {trendDirectionLabel(trend.trendDirection)}
                    </Badge>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className={surfaces.panel} aria-labelledby="baselines-heading">
            <div className={surfaces.panelHeader}>
              <h2 className={surfaces.panelTitle} id="baselines-heading">
                Baselines &amp; comparisons
              </h2>
              <Link className={surfaces.panelLink} to="/app/recovery/analytics">
                Open analytics
              </Link>
            </div>
            <BaselineDeviationTable baselines={overview.baselines} deviations={overview.deviations} />
          </section>

          <section className={surfaces.panel} aria-labelledby="guidance-heading">
            <div className={surfaces.panelHeader}>
              <h2 className={surfaces.panelTitle} id="guidance-heading">
                Guidance
              </h2>
            </div>
            {overview.recommendationPresent && overview.recommendation ? (
              <div className={surfaces.trendRow}>
                <div>
                  <p className={surfaces.heroTitle} style={{ fontSize: 'var(--uap-font-size-lg)' }}>
                    {recommendationActionLabel(overview.recommendation.overallAction)}
                  </p>
                  <div className={surfaces.metaRow} style={{ marginTop: '0.5rem' }}>
                    <Badge tone="info">
                      {recommendationStatusLabel(overview.recommendation.recommendationStatus)}
                    </Badge>
                    {overview.recommendation.adjustmentTypes.length > 0 ? (
                      <span className={surfaces.metaText}>
                        Adjustments: {overview.recommendation.adjustmentTypes.join(', ')}
                      </span>
                    ) : null}
                  </div>
                </div>
                <Link
                  className={surfaces.panelLink}
                  to={`/app/recovery/guidance/${overview.recommendation.recommendationId}`}
                >
                  View details
                </Link>
              </div>
            ) : (
              <EmptyView
                title="No recommendation yet"
                message="Training guidance will appear once a recommendation is generated for today."
              />
            )}
          </section>

          {overview.discomfort.length > 0 ? (
            <section className={surfaces.panel} aria-labelledby="discomfort-heading">
              <div className={surfaces.panelHeader}>
                <h2 className={surfaces.panelTitle} id="discomfort-heading">
                  Reported discomfort
                </h2>
              </div>
              <ul className={surfaces.discomfortList}>
                {overview.discomfort.map((entry, index) => (
                  <li
                    key={`${entry.bodyArea}-${entry.bodySide}-${index}`}
                    className={surfaces.discomfortRow}
                  >
                    <div>
                      <p className={surfaces.discomfortName}>
                        {bodyAreaLabel(entry.bodyArea)} ({bodySideLabel(entry.bodySide)})
                      </p>
                      {entry.notes ? <p className={surfaces.metaText}>{entry.notes}</p> : null}
                    </div>
                    <Badge tone={discomfortIntensityTone(entry.intensity)}>
                      Intensity {entry.intensity}
                    </Badge>
                  </li>
                ))}
              </ul>
            </section>
          ) : null}

          {load ? (
            <section className={surfaces.panel} aria-labelledby="load-heading">
              <div className={surfaces.panelHeader}>
                <h2 className={surfaces.panelTitle} id="load-heading">
                  Training load context
                </h2>
                <span className={surfaces.panelHint}>Parallel context only — not causal</span>
              </div>
              <div className={surfaces.metricGrid}>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Sessions</span>
                  <span className={surfaces.metricValue}>{load.occurrenceCount}</span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Rated</span>
                  <span className={surfaces.metricValue}>{load.ratedOccurrenceCount}</span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Unrated</span>
                  <span className={surfaces.metricValue}>{load.unratedOccurrenceCount}</span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Duration</span>
                  <span className={surfaces.metricValue}>
                    {formatDurationSeconds(load.totalDurationSeconds)}
                  </span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Volume</span>
                  <span className={surfaces.metricValue}>
                    {load.totalVolumeKilograms != null ? formatVolumeKg(load.totalVolumeKilograms) : '—'}
                  </span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Session-RPE load</span>
                  <span className={surfaces.metricValue}>
                    {load.totalSessionRpeLoad != null ? Number(load.totalSessionRpeLoad).toFixed(0) : '—'}
                  </span>
                </div>
              </div>
            </section>
          ) : null}
        </div>
      ) : null}
    </Page>
  );
}
