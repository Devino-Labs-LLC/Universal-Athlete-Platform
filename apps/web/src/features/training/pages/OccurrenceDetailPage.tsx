import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { OccurrencePerformanceSummary } from '@/features/performance/components/OccurrencePerformanceSummary';
import { useOccurrencePerformance } from '@/features/performance/hooks/useOccurrencePerformance';
import { ConfirmationDialog } from '@/features/profile/components/ConfirmationDialog';
import {
  isOccurrenceDeletable,
  isOccurrenceReschedulable,
} from '@/features/training/api/occurrencesApi';
import { RescheduleForm } from '@/features/training/forms/RescheduleForm';
import { useOccurrenceDetail, useOccurrenceMutations } from '@/features/training/hooks/useOccurrences';
import { usePlan } from '@/features/training/hooks/usePlans';
import { OCCURRENCE_STATUS_LABELS } from '@/features/training/models/labels';
import { trainingErrorMessage } from '@/features/training/models/trainingErrors';
import { formatExecutionPrescription } from '@/features/training/utils/prescriptionFormat';

export function OccurrenceDetailPage() {
  const { planId = '', dayId = '', occurrenceId = '' } = useParams();
  const navigate = useNavigate();
  const planQuery = usePlan(planId);
  const occurrenceQuery = useOccurrenceDetail(planId, dayId, occurrenceId);
  const mutations = useOccurrenceMutations(planId, dayId);
  const [showReschedule, setShowReschedule] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (planQuery.isLoading || occurrenceQuery.isLoading) {
    return <LoadingView message="Loading occurrence…" />;
  }

  if (occurrenceQuery.isError || !occurrenceQuery.data) {
    return (
      <ErrorView message="Unable to load occurrence." onRetry={() => occurrenceQuery.refetch()} />
    );
  }

  const occurrence = occurrenceQuery.data;
  const canReschedule = isOccurrenceReschedulable(occurrence);
  const canDelete = isOccurrenceDeletable(occurrence);
  const isCompleted = occurrence.status === 'COMPLETED';

  return (
    <Page
      title="Workout occurrence"
      description="Read-only planner view with snapshot prescriptions."
      actions={
        <Link to={`/app/training/plans/${planId}`}>
          <Button type="button" variant="secondary">
            Back to plan
          </Button>
        </Link>
      }
    >
      {errorMessage ? <p className="formError">{errorMessage}</p> : null}

      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">{planQuery.data?.name ?? 'Training plan'}</h2>
        <dl className="statGrid">
          <div>
            <dt>Status</dt>
            <dd>{OCCURRENCE_STATUS_LABELS[occurrence.status] ?? occurrence.status}</dd>
          </div>
          <div>
            <dt>Scheduled date</dt>
            <dd>{occurrence.scheduledDate}</dd>
          </div>
          <div>
            <dt>Planned start</dt>
            <dd>{occurrence.plannedStartTime ?? '—'}</dd>
          </div>
          <div>
            <dt>Origin</dt>
            <dd>{occurrence.origin ?? '—'}</dd>
          </div>
        </dl>
        {occurrence.environment?.plannedEnvironment?.name ? (
          <p>Planned environment: {occurrence.environment.plannedEnvironment.name}</p>
        ) : null}
        {occurrence.athleteNotes ? <p>Notes: {occurrence.athleteNotes}</p> : null}
      </section>

      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">Snapshot prescriptions</h2>
        {(occurrence.executions ?? []).length === 0 ? (
          <p>No execution snapshots yet.</p>
        ) : (
          <ul>
            {(occurrence.executions ?? []).map((execution) => (
              <li key={execution.id} style={{ marginBottom: '0.5rem' }}>
                <strong>{execution.exerciseName}</strong>
                <p>{formatExecutionPrescription(execution)}</p>
              </li>
            ))}
          </ul>
        )}
      </section>

      {isCompleted ? (
        <section className="card" style={{ marginBottom: '1rem' }}>
          <h2 className="cardTitle">Performance</h2>
          <OccurrencePerformanceSection planId={planId} dayId={dayId} occurrenceId={occurrenceId} />
        </section>
      ) : null}

      {canReschedule || canDelete ? (
        <section className="card">
          <h2 className="cardTitle">Manage occurrence</h2>
          {canReschedule ? (
            <>
              <Button type="button" variant="secondary" onClick={() => setShowReschedule((v) => !v)}>
                {showReschedule ? 'Hide reschedule' : 'Reschedule'}
              </Button>
              {showReschedule ? (
                <div style={{ marginTop: '1rem' }}>
                  <RescheduleForm
                    defaultDate={occurrence.scheduledDate}
                    onSubmit={async (values) => {
                      try {
                        await mutations.reschedule.mutateAsync({ occurrenceId, request: values });
                        setShowReschedule(false);
                      } catch (error) {
                        setErrorMessage(trainingErrorMessage(error));
                      }
                    }}
                  />
                </div>
              ) : null}
            </>
          ) : null}
          {canDelete ? (
            <Button
              type="button"
              variant="secondary"
              style={{ marginLeft: '0.5rem' }}
              onClick={() => setConfirmDelete(true)}
            >
              Delete
            </Button>
          ) : null}
        </section>
      ) : null}

      <ConfirmationDialog
        open={confirmDelete}
        title="Delete occurrence?"
        message="Only scheduled, untouched occurrences can be deleted."
        confirmLabel="Delete"
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => {
          void mutations.remove.mutateAsync(occurrenceId).then(() => {
            navigate(`/app/training/calendar`);
          });
        }}
      />
    </Page>
  );
}

interface OccurrencePerformanceSectionProps {
  planId: string;
  dayId: string;
  occurrenceId: string;
}

function OccurrencePerformanceSection({ planId, dayId, occurrenceId }: OccurrencePerformanceSectionProps) {
  const performanceQuery = useOccurrencePerformance(planId, dayId, occurrenceId);

  if (performanceQuery.isLoading) {
    return <LoadingView message="Loading performance summary…" />;
  }

  if (performanceQuery.isError || !performanceQuery.data) {
    return <p>Performance summary is not available for this session yet.</p>;
  }

  return (
    <>
      <OccurrencePerformanceSummary performance={performanceQuery.data} />
      <p style={{ marginTop: '1rem' }}>
        <Link to={`/app/performance/sessions/${planId}/${dayId}/${occurrenceId}`}>View performance details</Link>
      </p>
    </>
  );
}
