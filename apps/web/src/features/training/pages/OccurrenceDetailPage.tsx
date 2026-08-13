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
import { MetricPill } from '@/features/training/components/MetricPill';
import { TrainingStatusBadge } from '@/features/training/components/TrainingStatusBadge';
import { RescheduleForm } from '@/features/training/forms/RescheduleForm';
import { useOccurrenceDetail, useOccurrenceMutations } from '@/features/training/hooks/useOccurrences';
import { usePlan } from '@/features/training/hooks/usePlans';
import { trainingErrorMessage } from '@/features/training/models/trainingErrors';
import { formatExecutionPrescription } from '@/features/training/utils/prescriptionFormat';
import styles from '@/features/training/pages/OccurrenceDetailPage.module.scss';

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
    return <LoadingView message="Loading workout…" />;
  }

  if (occurrenceQuery.isError || !occurrenceQuery.data) {
    return (
      <ErrorView message="Unable to load workout." onRetry={() => occurrenceQuery.refetch()} />
    );
  }

  const occurrence = occurrenceQuery.data;
  const canReschedule = isOccurrenceReschedulable(occurrence);
  const canDelete = isOccurrenceDeletable(occurrence);
  const isCompleted = occurrence.status === 'COMPLETED';
  const executions = occurrence.executions ?? [];
  const completedCount = executions.filter((item) => item.status === 'COMPLETED').length;

  return (
    <Page
      title="Workout"
      description="Session review and preparation — read-only planner view with snapshot prescriptions."
      width="wide"
      actions={
        <Link to={`/app/training/plans/${planId}`}>
          <Button type="button" variant="secondary">
            Back to plan
          </Button>
        </Link>
      }
    >
      {errorMessage ? <p className="formError">{errorMessage}</p> : null}

      <section className={styles.hero} aria-labelledby="occurrence-heading">
        <div className={styles.heroCopy}>
          <p className={styles.eyebrow}>Session</p>
          <h2 className={styles.heroTitle} id="occurrence-heading">
            {planQuery.data?.name ?? 'Training plan'}
          </h2>
          <div className={styles.metaRow}>
            <TrainingStatusBadge kind="occurrence" status={occurrence.status} />
            <span className={styles.metaText}>{occurrence.scheduledDate}</span>
            {occurrence.plannedStartTime ? (
              <span className={styles.metaText}>{occurrence.plannedStartTime}</span>
            ) : null}
            <MetricPill label="Progress">
              {completedCount}/{executions.length || '—'}
            </MetricPill>
          </div>
        </div>
        <dl className={styles.statGrid}>
          <div>
            <dt>Origin</dt>
            <dd>{occurrence.origin ?? '—'}</dd>
          </div>
          <div>
            <dt>Planned environment</dt>
            <dd>{occurrence.environment?.plannedEnvironment?.name ?? '—'}</dd>
          </div>
          <div>
            <dt>Actual environment</dt>
            <dd>{occurrence.environment?.actualEnvironment?.name ?? '—'}</dd>
          </div>
        </dl>
        {occurrence.athleteNotes ? <p className={styles.notes}>Notes: {occurrence.athleteNotes}</p> : null}
      </section>

      <section className={styles.panel} aria-labelledby="snapshot-heading">
        <h2 className={styles.panelTitle} id="snapshot-heading">
          Snapshot prescriptions
        </h2>
        {executions.length === 0 ? (
          <p className={styles.empty}>No execution snapshots yet.</p>
        ) : (
          <ul className={styles.prescriptionList}>
            {executions.map((execution, index) => (
              <li key={execution.id} className={styles.prescriptionRow}>
                <span className={styles.order} aria-hidden="true">
                  {String(index + 1).padStart(2, '0')}
                </span>
                <div>
                  <strong className={styles.exerciseName}>{execution.exerciseName}</strong>
                  <p className={styles.prescription}>{formatExecutionPrescription(execution)}</p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      {isCompleted ? (
        <section className={styles.panel} aria-labelledby="performance-heading">
          <h2 className={styles.panelTitle} id="performance-heading">
            Performance
          </h2>
          <OccurrencePerformanceSection planId={planId} dayId={dayId} occurrenceId={occurrenceId} />
        </section>
      ) : null}

      {canReschedule || canDelete ? (
        <section className={styles.panel} aria-labelledby="manage-heading">
          <h2 className={styles.panelTitle} id="manage-heading">
            Manage occurrence
          </h2>
          <div className={styles.actionBar}>
            {canReschedule ? (
              <Button type="button" variant="secondary" onClick={() => setShowReschedule((v) => !v)}>
                {showReschedule ? 'Hide reschedule' : 'Reschedule'}
              </Button>
            ) : null}
            {canDelete ? (
              <Button type="button" variant="secondary" onClick={() => setConfirmDelete(true)}>
                Delete
              </Button>
            ) : null}
          </div>
          {canReschedule && showReschedule ? (
            <div className={styles.rescheduleForm}>
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
        </section>
      ) : null}

      <ConfirmationDialog
        open={confirmDelete}
        title="Delete occurrence?"
        message="Only scheduled, untouched occurrences can be deleted."
        confirmLabel="Delete"
        onCancel={() => setConfirmDelete(false)}
        onConfirm={() => {
          void mutations.remove
            .mutateAsync(occurrenceId)
            .then(() => {
              navigate('/app/training/calendar');
            })
            .catch((error: unknown) => {
              setErrorMessage(trainingErrorMessage(error));
              setConfirmDelete(false);
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
    return <p className={styles.empty}>Performance summary is not available for this session yet.</p>;
  }

  return (
    <>
      <OccurrencePerformanceSummary performance={performanceQuery.data} />
      <p className={styles.performanceLink}>
        <Link to={`/app/performance/sessions/${planId}/${dayId}/${occurrenceId}`}>View performance details</Link>
      </p>
    </>
  );
}
