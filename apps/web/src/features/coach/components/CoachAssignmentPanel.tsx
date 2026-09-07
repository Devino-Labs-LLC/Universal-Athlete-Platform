import { useState, type FormEvent } from 'react';

import { todayDateOnly } from '@/core/date/dateOnly';
import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { useCreateCoachAssignment, useCoachAssignments } from '@/features/coach/hooks/useCoachQueries';
import { coachErrorMessage } from '@/features/coach/models/errors';
import styles from '@/features/coach/pages/CoachPages.module.scss';

export function CoachAssignmentPanel({
  teamId,
  athleteId,
  collaborationAvailable,
}: {
  teamId: string;
  athleteId: string;
  collaborationAvailable: boolean;
}) {
  const assignmentsQuery = useCoachAssignments(teamId, athleteId, collaborationAvailable);
  const createAssignment = useCreateCoachAssignment(teamId, athleteId);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [scheduledDate, setScheduledDate] = useState(() => String(todayDateOnly()));
  const [formError, setFormError] = useState<string | null>(null);

  if (!collaborationAvailable) {
    return (
      <section className={styles.identityPanel} aria-label="Training collaboration">
        <h2 className={styles.sectionTitle}>Training collaboration</h2>
        <p className={styles.meta}>
          This athlete has not shared training collaboration. Assignment is unavailable.
        </p>
      </section>
    );
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    try {
      await createAssignment.mutateAsync({
        title: title.trim(),
        description: description.trim(),
        scheduledDate,
        idempotencyKey: crypto.randomUUID(),
      });
      setTitle('');
      setDescription('');
    } catch (error) {
      setFormError(coachErrorMessage(error, 'Unable to assign training.'));
    }
  }

  return (
    <section className={styles.identityPanel} aria-label="Training collaboration">
      <h2 className={styles.sectionTitle}>Training collaboration</h2>
      <p className={styles.meta}>
        Assigned work is separate from Athlete Readiness guidance. Assigning does not change readiness.
      </p>
      {assignmentsQuery.isLoading ? <LoadingView message="Loading assignments…" /> : null}
      {assignmentsQuery.isError ? (
        <ErrorView
          message={coachErrorMessage(assignmentsQuery.error, 'Unable to load assignments.')}
          onRetry={() => void assignmentsQuery.refetch()}
        />
      ) : null}
      <ul className={styles.meta}>
        {(assignmentsQuery.data ?? []).map((assignment) => (
          <li key={assignment.id}>
            {assignment.title} · {assignment.scheduledDate} · {assignment.status}
          </li>
        ))}
      </ul>
      <form className={styles.sectionStack} onSubmit={(event) => void onSubmit(event)}>
        <label className={styles.field}>
          <span className={styles.fieldLabel}>Session title</span>
          <input
            className={styles.select}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            required
            maxLength={160}
          />
        </label>
        <label className={styles.field}>
          <span className={styles.fieldLabel}>Prescription</span>
          <input
            className={styles.select}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            maxLength={2000}
          />
        </label>
        <label className={styles.field}>
          <span className={styles.fieldLabel}>Date</span>
          <input
            className={styles.select}
            type="date"
            value={scheduledDate}
            onChange={(event) => setScheduledDate(event.target.value)}
            required
          />
        </label>
        {formError ? <p className={styles.meta}>{formError}</p> : null}
        <Button type="submit" disabled={createAssignment.isPending || title.trim().length === 0}>
          Assign session
        </Button>
      </form>
    </section>
  );
}
