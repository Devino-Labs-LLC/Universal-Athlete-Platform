import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { MetricPill } from '@/features/training/components/MetricPill';
import { ScheduleStatusBadge } from '@/features/training/components/ScheduleStatusBadge';
import { TrainingStatusBadge } from '@/features/training/components/TrainingStatusBadge';
import { GenerateForm, GenerationResultSummary } from '@/features/training/forms/GenerateForm';
import { ScheduleActivateForm } from '@/features/training/forms/ScheduleActivateForm';
import { usePlan } from '@/features/training/hooks/usePlans';
import { useScheduleMutations } from '@/features/training/hooks/useScheduleMutations';
import { trainingErrorMessage } from '@/features/training/models/trainingErrors';
import type { GenerationResult } from '@/features/training/models/schemas';
import styles from '@/features/training/pages/ScheduleManagementPage.module.scss';

export function ScheduleManagementPage() {
  const { planId = '' } = useParams();
  const planQuery = usePlan(planId);
  const scheduleMutations = useScheduleMutations(planId);
  const [generationResult, setGenerationResult] = useState<GenerationResult | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (planQuery.isLoading) {
    return <LoadingView message="Loading schedule…" />;
  }

  if (planQuery.isError || !planQuery.data) {
    return <ErrorView message="Unable to load plan schedule." onRetry={() => planQuery.refetch()} />;
  }

  const plan = planQuery.data;
  const scheduleStatus = plan.scheduleStatus ?? 'DRAFT';
  const planIsMutable = plan.status !== 'ARCHIVED';

  return (
    <Page
      title={`Schedule · ${plan.name}`}
      description="Programming and periodization controls — separate from plan content status."
      width="wide"
      actions={
        <Link to={`/app/training/plans/${planId}`}>
          <Button type="button" variant="secondary">
            Back to builder
          </Button>
        </Link>
      }
    >
      {errorMessage ? <p className="formError">{errorMessage}</p> : null}

      <section className={styles.panel} aria-labelledby="schedule-state-heading">
        <div className={styles.panelHeader}>
          <h2 className={styles.panelTitle} id="schedule-state-heading">
            Current schedule
          </h2>
          <div className={styles.badgeRow}>
            <TrainingStatusBadge kind="plan" status={plan.status} />
            {plan.scheduleStatus ? <ScheduleStatusBadge status={plan.scheduleStatus} /> : null}
          </div>
        </div>
        <dl className={styles.statGrid}>
          <div>
            <dt>Schedule start</dt>
            <dd>{plan.scheduleStartDate ?? '—'}</dd>
          </div>
          <div>
            <dt>Schedule end</dt>
            <dd>{plan.scheduleEndDate ?? '—'}</dd>
          </div>
          <div>
            <dt>Timezone</dt>
            <dd>{plan.scheduleTimezone ?? '—'}</dd>
          </div>
          <div>
            <dt>Recurrence</dt>
            <dd>{plan.recurrenceMode ?? '—'}</dd>
          </div>
          <div>
            <dt>Generated through</dt>
            <dd>
              <MetricPill>{plan.scheduleGeneratedThrough ?? '—'}</MetricPill>
            </dd>
          </div>
        </dl>
      </section>

      {planIsMutable && scheduleStatus === 'DRAFT' ? (
        <section className={styles.panel} aria-labelledby="activate-heading">
          <h2 className={styles.panelTitle} id="activate-heading">
            Activate schedule
          </h2>
          <p className={styles.hint}>Set recurrence and start programming occurrences.</p>
          <ScheduleActivateForm
            defaultStartDate={plan.startDate}
            onSubmit={async (values) => {
              try {
                const result = await scheduleMutations.activate.mutateAsync(values);
                if (result.generation) {
                  setGenerationResult(result.generation);
                }
              } catch (error) {
                setErrorMessage(trainingErrorMessage(error));
              }
            }}
          />
        </section>
      ) : null}

      {planIsMutable && (scheduleStatus === 'ACTIVE' || scheduleStatus === 'PAUSED') ? (
        <section className={styles.panel} aria-labelledby="actions-heading">
          <h2 className={styles.panelTitle} id="actions-heading">
            Schedule actions
          </h2>
          <div className={styles.actionBar}>
            {scheduleStatus === 'ACTIVE' ? (
              <Button
                type="button"
                variant="secondary"
                onClick={() =>
                  scheduleMutations.pause.mutate(undefined, {
                    onError: (error) => setErrorMessage(trainingErrorMessage(error)),
                  })
                }
              >
                Pause
              </Button>
            ) : null}
            {scheduleStatus === 'PAUSED' ? (
              <Button
                type="button"
                onClick={() =>
                  scheduleMutations.resume.mutate(undefined, {
                    onError: (error) => setErrorMessage(trainingErrorMessage(error)),
                  })
                }
              >
                Resume
              </Button>
            ) : null}
            {scheduleStatus === 'ACTIVE' || scheduleStatus === 'PAUSED' ? (
              <Button
                type="button"
                variant="secondary"
                onClick={() =>
                  scheduleMutations.complete.mutate(undefined, {
                    onError: (error) => setErrorMessage(trainingErrorMessage(error)),
                  })
                }
              >
                Complete schedule
              </Button>
            ) : null}
          </div>
        </section>
      ) : null}

      {planIsMutable && scheduleStatus === 'ACTIVE' ? (
        <section className={styles.panel} aria-labelledby="generate-heading">
          <h2 className={styles.panelTitle} id="generate-heading">
            Generate occurrences
          </h2>
          <p className={styles.hint}>
            Explicit generation only — choose a bounded range. Results report created vs existing vs
            cancelled placements.
          </p>
          <GenerateForm
            defaultFrom={plan.scheduleStartDate ?? plan.startDate}
            defaultTo={plan.scheduleGeneratedThrough ?? plan.scheduleEndDate ?? plan.endDate ?? undefined}
            onSubmit={async (values) => {
              const result = await scheduleMutations.generate.mutateAsync(values);
              setGenerationResult(result);
              return result;
            }}
          />
          {generationResult ? (
            <div className={styles.generationResult}>
              <GenerationResultSummary result={generationResult} />
            </div>
          ) : null}
        </section>
      ) : null}
    </Page>
  );
}
