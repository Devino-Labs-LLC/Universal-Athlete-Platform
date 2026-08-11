import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { ScheduleStatusBadge } from '@/features/training/components/ScheduleStatusBadge';
import { GenerateForm, GenerationResultSummary } from '@/features/training/forms/GenerateForm';
import { ScheduleActivateForm } from '@/features/training/forms/ScheduleActivateForm';
import { usePlan } from '@/features/training/hooks/usePlans';
import { useScheduleMutations } from '@/features/training/hooks/useScheduleMutations';
import { PLAN_STATUS_LABELS } from '@/features/training/models/labels';
import { trainingErrorMessage } from '@/features/training/models/trainingErrors';
import type { GenerationResult } from '@/features/training/models/schemas';

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
      description="Manage schedule activation separately from plan content status."
      actions={
        <Link to={`/app/training/plans/${planId}`}>
          <Button type="button" variant="secondary">
            Back to builder
          </Button>
        </Link>
      }
    >
      {errorMessage ? <p className="formError">{errorMessage}</p> : null}

      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">Content vs schedule</h2>
        <p>
          Content status: <strong>{PLAN_STATUS_LABELS[plan.status] ?? plan.status}</strong>
        </p>
        <p>
          Schedule status:{' '}
          {plan.scheduleStatus ? <ScheduleStatusBadge status={plan.scheduleStatus} /> : 'Not configured'}
        </p>
        <dl className="statGrid">
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
            <dd>{plan.scheduleGeneratedThrough ?? '—'}</dd>
          </div>
        </dl>
      </section>

      {planIsMutable && scheduleStatus === 'DRAFT' ? (
        <section className="card" style={{ marginBottom: '1rem' }}>
          <h2 className="cardTitle">Activate schedule</h2>
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
        <section className="card" style={{ marginBottom: '1rem' }}>
          <h2 className="cardTitle">Schedule actions</h2>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
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
        <section className="card">
          <h2 className="cardTitle">Generate occurrences</h2>
          <GenerateForm
            defaultFrom={plan.scheduleStartDate ?? plan.startDate}
            defaultTo={plan.scheduleGeneratedThrough ?? plan.scheduleEndDate ?? plan.endDate ?? undefined}
            onSubmit={async (values) => {
              const result = await scheduleMutations.generate.mutateAsync(values);
              setGenerationResult(result);
              return result;
            }}
          />
          {generationResult ? <GenerationResultSummary result={generationResult} /> : null}
        </section>
      ) : null}
    </Page>
  );
}
